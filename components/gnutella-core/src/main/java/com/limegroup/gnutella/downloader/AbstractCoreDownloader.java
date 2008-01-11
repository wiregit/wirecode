package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.settings.SharingSettings;

public abstract class AbstractCoreDownloader implements CoreDownloader {
    
	/** LOCKING: this */
	protected volatile Map<String, Serializable> propertiesMap;

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

	protected final SaveLocationManager saveLocationManager;
	
	protected AbstractCoreDownloader(SaveLocationManager saveLocationManager) {
	    this.saveLocationManager = Objects.nonNull(saveLocationManager, "saveLocationManager");	    
	    synchronized(this) {
	        propertiesMap = new HashMap<String, Serializable>();
	        propertiesMap.put(CoreDownloader.ATTRIBUTES, (Serializable)attributes);
	    }
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setInactivePriority(int)
     */
	public void setInactivePriority(int priority) {
	    inactivePriority = priority;
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#getInactivePriority()
     */
	public int getInactivePriority() {
	    return inactivePriority;
	}
	
	@SuppressWarnings("unchecked")
    public void addNewProperties(Map<String, Serializable> newProperties) {
	    synchronized(this) {
    	    for(Map.Entry<String, Serializable> entry : newProperties.entrySet()) {
    	        if(propertiesMap.get(entry.getKey()) == null)
    	            propertiesMap.put(entry.getKey(), entry.getValue());
    	    }
    	    
    	    // and go through attributes individually, since this will always have an attr map
    	    Map<String, Serializable> newAttributes = (Map<String, Serializable>)newProperties.get(CoreDownloader.ATTRIBUTES);
    	    if(newAttributes != null) {
    	        for(Map.Entry<String, Serializable> entry : newAttributes.entrySet()) {
    	            if(attributes.get(entry.getKey()) == null)
    	                attributes.put(entry.getKey(), entry.getValue());
    	        }    	            
    	    }   
	    }
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setAttribute(java.lang.String, java.io.Serializable)
     */
	public Serializable setAttribute(String key, Serializable value) {
	    return attributes.put( key, value );
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#getAttribute(java.lang.String)
     */
	public Serializable getAttribute(String key) {
	    return attributes.get( key );
	}

	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#removeAttribute(java.lang.String)
     */
	public Object removeAttribute(String key) {
	    return attributes.remove( key );
	}
	
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#conflictsSaveFile(java.io.File)
     */
	public boolean conflictsSaveFile(File saveFile) {
		return getSaveFile().equals(saveFile);
	}
    
	/* (non-Javadoc)
     * @see com.limegroup.gnutella.downloader.CoreDownloader#setSaveFile(java.io.File, java.lang.String, boolean)
     */
	public void setSaveFile(File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException {
	    if (fileName == null) {
	        fileName = getDefaultFileName();
	    }
	    
	    if (saveDirectory == null) {
	        saveDirectory = SharingSettings.getSaveDirectory(fileName);
	    }
	    
	    try {
	        fileName = CommonUtils.convertFileName(saveDirectory, fileName);
	    }
	    catch (IOException ie) {
	        if (saveDirectory.isDirectory()) {
	            throw new SaveLocationException(SaveLocationException.PATH_NAME_TOO_LONG, saveDirectory);
	        }
	        // if not a directory, give precedence to error messages below
	    }
	    
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
		if (saveLocationManager.isSaveLocationTaken(candidateFile)) {
			throw new SaveLocationException(SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO, candidateFile);
		}
	     
	    // Passed sanity checks, so save file
	    synchronized (this) {
	        if (!isRelocatable())
	            throw new SaveLocationException(SaveLocationException.FILE_ALREADY_SAVED, candidateFile);
	        propertiesMap.put(CoreDownloader.SAVE_FILE, candidateFile);
	    }
	}
	
    /**
	 * Returns the value for the key {@link CoreDownloader#DEFAULT_FILENAME} from
	 * the properties map.
	 * <p>
	 * Subclasses should put the name into the map or override this
	 * method.
	 */
    protected synchronized String getDefaultFileName() {       
        String fileName = (String)propertiesMap.get(CoreDownloader.DEFAULT_FILENAME); 
        assert fileName != null : "defaultFileName is null, "+
                         "subclass may have not overridden getDefaultFileName";
		return CommonUtils.convertFileName(fileName);
    }
}
