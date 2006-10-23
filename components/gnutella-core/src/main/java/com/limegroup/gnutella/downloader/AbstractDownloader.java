package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;

public abstract class AbstractDownloader implements Downloader, Serializable {

    /** 
     * Make everything transient.  This class does not serialize or deserialize
     * anything, even though it behaves as if propertiesMap would be persisted.
     * Therefore it is essential that implementing classes: 
     *   1. serialize & deserialize propertiesMap
     *   2. store the attributes field inside propertiesMap and scan for it
     *      when deserializing.
     */
    private static final ObjectStreamField[] serialPersistentFields = 
    	ObjectStreamClass.NO_FIELDS;
    
	protected static final String ATTRIBUTES = "attributes";

	protected static final String DEFAULT_FILENAME = "defaultFileName";

	protected static final String FILE_SIZE = "fileSize";

	protected Map<String, Serializable> propertiesMap;

	/**
	 * The key under which the saveFile File is stored in the attribute map
	 * used in serializing and deserializing ManagedDownloaders. 
	 */
	protected static final String SAVE_FILE = "saveFile";
	
	/**
	 * The current priority of this download -- only valid if inactive.
	 * Has no bearing on the download itself, and is used only so that the
	 * download doesn't have to be indexed in DownloadManager's inactive list
	 * every second, for GUI updates.
	 */
	private volatile int inactivePriority;
	
	/**
	 * A map of attributes associated with the download. The attributes
	 * may be used by GUI, to keep some additional information about
	 * the download.
	 */
	protected Map<String, Serializable> attributes = new HashMap<String, Serializable>();

	protected AbstractDownloader() {
		propertiesMap = new HashMap<String, Serializable>();
		propertiesMap.put(ATTRIBUTES, (Serializable)attributes);
	}
	/**
	 * Sets the inactive priority of this download.
	 */
	public void setInactivePriority(int priority) {
	    inactivePriority = priority;
	}

	/**
	 * Gets the inactive priority of this download.
	 */
	public int getInactivePriority() {
	    return inactivePriority;
	}

	/**
	 * Sets a new attribute associated with the download.
	 * The attributes are used eg. by GUI to store some extra
	 * information about the download.
	 * @param key A key used to identify the attribute.
	 * @patam value The value of the key.
	 * @return A prvious value of the attribute, or <code>null</code>
	 *         if the attribute wasn't set.
	 */
	public Serializable setAttribute(String key, Serializable value) {
	    return attributes.put( key, value );
	}

	/**
	 * Gets a value of attribute associated with the download.
	 * The attributes are used eg. by GUI to store some extra
	 * information about the download.
	 * @param key A key which identifies the attribue.
	 * @return The value of the specified attribute,
	 *         or <code>null</code> if value was not specified.
	 */
	public Serializable getAttribute(String key) {
	    return attributes.get( key );
	}

	/**
	 * Removes an attribute associated with this download.
	 * @param key A key which identifies the attribute do remove.
	 * @return A value of the attribute or <code>null</code> if
	 *         attribute was not set.
	 */
	public Object removeAttribute(String key) {
	    return attributes.remove( key );
	}

	public abstract GUID getQueryGUID();
	
	/**
     * Starts the download.
     */
	public abstract void startDownload();
	
	/**
	 * @return whether the download is still alive and cannot be
	 * restarted.
	 */
	public abstract boolean isAlive();
	
	/**
	 * @return whether it makes sense to restart this download.
	 */
	public abstract boolean shouldBeRestarted();
	
	/**
	 * @return whether the download should be removed from 
	 * the waiting list.
	 */
	public abstract boolean shouldBeRemoved();
	
	/**
	 * Handles state changes and other operations while
	 * inactive.
	 */
	public abstract void handleInactivity();
	
	public abstract boolean isQueuable();
	
    /**
     * Cleans up any resources before this downloader 
     * is completely disposed.
     */
	public abstract void finish();
	
	public abstract boolean conflicts(URN urn, int fileSize, File... files);
	
	public abstract boolean conflictsWithIncompleteFile(File incomplete);
	
	public boolean conflictsSaveFile(File saveFile) {
		return getSaveFile().equals(saveFile);
	}
	
	public abstract void initialize(DownloadManager m, 
			FileManager fm, DownloadCallback ac);

	/**
	 * Sets the file name and directory where the download will be saved once
	 * complete.
	 * 
	 * @param overwrite true if overwriting an existing file is allowed
	 * @throws IOException if FileUtils.isReallyParent(testParent, testChild) throws IOException
	 */
	public void setSaveFile(File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
	    if (fileName == null)
	        fileName = getDefaultFileName();
	    if (saveDirectory == null)
	        saveDirectory = SharingSettings.getSaveDirectory(fileName);
	    
	    if (!saveDirectory.isDirectory()) {
	        if (saveDirectory.exists())
	            throw new SaveLocationException(SaveLocationException.NOT_A_DIRECTORY, saveDirectory);
	        throw new SaveLocationException(SaveLocationException.DIRECTORY_DOES_NOT_EXIST, saveDirectory);
	    }
	    
	    File candidateFile = new File(saveDirectory, fileName);
	    try {
	        if (!FileUtils.isReallyParent(saveDirectory, candidateFile))
	            throw new SaveLocationException(SaveLocationException.SECURITY_VIOLATION, candidateFile);
	    } catch (IOException e) {
	        throw new SaveLocationException(SaveLocationException.FILESYSTEM_ERROR, candidateFile);
	    }
		
	    if (! FileUtils.setWriteable(saveDirectory))    
	        throw new SaveLocationException(SaveLocationException.DIRECTORY_NOT_WRITEABLE,saveDirectory);
		
	    if (candidateFile.exists()) {
	        if (!candidateFile.isFile()) // TODO: how does this mix with torrents?
	            throw new SaveLocationException(SaveLocationException.FILE_NOT_REGULAR, candidateFile);
	        if (!overwrite)
	            throw new SaveLocationException(SaveLocationException.FILE_ALREADY_EXISTS, candidateFile);
	    }
		
		// check if another existing download is being saved to this download
		// we ignore the overwrite flag on purpose in this case
		if (RouterService.getDownloadManager().isSaveLocationTaken(candidateFile)) {
			throw new SaveLocationException(SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO, candidateFile);
		}
	     
	    // Passed sanity checks, so save file
	    synchronized (this) {
	        if (!isRelocatable())
	            throw new SaveLocationException(SaveLocationException.FILE_ALREADY_SAVED, candidateFile);
	        propertiesMap.put(SAVE_FILE, candidateFile);
	    }
	}
	
    /**
	 * Returns the value for the key {@link #DEFAULT_FILENAME} from
	 * the properties map.
	 * <p>
	 * Subclasses should put the name into the map or override this
	 * method.
	 */
    protected synchronized String getDefaultFileName() {       
        String fileName = (String)propertiesMap.get(DEFAULT_FILENAME); 
         if (fileName == null) {
             Assert.that(false,"defaultFileName is null, "+
                         "subclass may have not overridden getDefaultFileName");
         }
		 return CommonUtils.convertFileName(fileName);
    }
}
