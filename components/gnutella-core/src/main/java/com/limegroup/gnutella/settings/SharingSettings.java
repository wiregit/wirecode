
package com.limegroup.gnutella.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;

/**
 * Settings for sharing
 */
public class SharingSettings extends LimeProps {
    
    private SharingSettings() {}
    
    public static final File DEFAULT_SAVE_DIR =
        new File(CommonUtils.getUserHomeDir(), "Shared");

    /**
     * Whether or not we're going to add an alternate for ourselves
     * to our shared files.  Primarily set to false for testing.
     */
    public static final BooleanSetting ADD_ALTERNATE_FOR_SELF =
        FACTORY.createBooleanSetting("ADD_ALTERNATE_FOR_SELF", true);

    /**
     * The directory for saving files.
     */
    public static final FileSetting DIRECTORY_FOR_SAVING_FILES = 
        (FileSetting)FACTORY.createFileSetting("DIRECTORY_FOR_SAVING_FILES", 
            DEFAULT_SAVE_DIR).setAlwaysSave(true);
    
    /**
     * The directory where incomplete files are stored (downloads in progress).
     */
    public static final FileSetting INCOMPLETE_DIRECTORY =
        FACTORY.createFileSetting("INCOMPLETE_DIRECTORY", 
            (new File(DIRECTORY_FOR_SAVING_FILES.getValue().getParent(),
                "Incomplete")));
    
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final FileSetting DOWNLOAD_SNAPSHOT_FILE =
        FACTORY.createFileSetting("DOWNLOAD_SNAPSHOT_FILE", 
            (new File(INCOMPLETE_DIRECTORY.getValue(), "downloads.dat")));
            
    /**
	 * A file with a snapshot of current downloading files.
	 */                
    public static final FileSetting DOWNLOAD_SNAPSHOT_BACKUP_FILE =
        FACTORY.createFileSetting("DOWNLOAD_SNAPSHOT_BACKUP_FILE", 
            (new File(INCOMPLETE_DIRECTORY.getValue(), "downloads.bak")));            
    
    /** The minimum age in days for which incomplete files will be deleted.
     *  This values may be zero or negative; doing so will cause LimeWire to
     *  delete ALL incomplete files on startup. */   
    public static final IntSetting INCOMPLETE_PURGE_TIME =
        FACTORY.createIntSetting("INCOMPLETE_PURGE_TIME", 7);
    
    /**
     * Specifies whether or not completed downloads
     * should automatically be cleared from the download window.
     */    
    public static final BooleanSetting CLEAR_DOWNLOAD =
        FACTORY.createBooleanSetting("CLEAR_DOWNLOAD", false);
        
    
    /**
     * Helper method left from SettingsManager.
     *
	 * Sets the directory for saving files.
	 *
     * <p><b>Modifies:</b> DIRECTORY_FOR_SAVING_FILES, INCOMPLETE_DIRECTORY, 
     *                     DOWNLOAD_SNAPSHOT_FILE</p>
     *
	 * @param   saveDir  A <tt>File</tt> instance denoting the
	 *                   abstract pathname of the directory for
	 *                   saving files.
	 *
	 * @throws  <tt>IOException</tt>
	 *          If the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system.
	 *
	 * @throws  <tt>NullPointerException</tt>
	 *          If the "dir" parameter is null.
	 */
    public static final void setSaveDirectory(File saveDir) throws IOException {
		if(saveDir == null) throw new NullPointerException();
		if(!saveDir.isDirectory()) {
			if(!saveDir.mkdirs()) throw new IOException("could not create save dir");
		}

		String parentDir = saveDir.getParent();
		File incDir = new File(parentDir, "Incomplete");
		if(!incDir.isDirectory()) {
			if(!incDir.mkdirs()) throw new IOException("could not create incomplete dir");
		}
		
        FileUtils.setWriteable(saveDir);
        FileUtils.setWriteable(incDir);

		if(!saveDir.canRead() || !saveDir.canWrite() ||
		   !incDir.canRead()  || !incDir.canWrite()) {
			throw new IOException("could not write to selected directory");
		}
		
		// Canonicalize the files ... 
		try {
		    saveDir = FileUtils.getCanonicalFile(saveDir);
		} catch(IOException ignored) {}
		try {
		    incDir = FileUtils.getCanonicalFile(incDir);
		} catch(IOException ignored) {}
		File snapFile = new File(incDir, "downloads.dat");
		try {
		    snapFile = FileUtils.getCanonicalFile(snapFile);
		} catch(IOException ignored) {}
		File snapBackup = new File(incDir, "downloads.bak");
		try {
		    snapBackup = FileUtils.getCanonicalFile(snapBackup);
		} catch(IOException ignored) {}
		
        DIRECTORY_FOR_SAVING_FILES.setValue(saveDir);
        INCOMPLETE_DIRECTORY.setValue(incDir);
        DOWNLOAD_SNAPSHOT_FILE.setValue(snapFile);
        DOWNLOAD_SNAPSHOT_BACKUP_FILE.setValue(snapBackup);
    }
    
    /**
     * Retrieves the save directory.
     */
    public static final File getSaveDirectory() {
        return DIRECTORY_FOR_SAVING_FILES.getValue();
    }
    
    /*********************************************************************/
    
    /**
     * Default file extensions.
     */
    private static final String DEFAULT_EXTENSIONS_TO_SHARE =
		"asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
		"ra;ram;wma;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;"+
		"mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
		"exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
		"bin;dmg;jve;nsv;med;mod;7z;iso;lwtp;pmf;m4a;idx;bz2;sea;pf;arc;arj;"+
		"bz;tbz;mime;taz;ua;toast;lit;rpm;sxw;l6t";
    
    /**
	 * The shared directories (an array of Files!).
	 */
    public static final FileArraySetting DIRECTORIES_TO_SHARE =
        FACTORY.createFileArraySetting("DIRECTORIES_TO_SEARCH_FOR_FILES", new File[0]);
    
    /**
     * Shared directories that should not be shared recursively.
     * */
    public static final FileArraySetting DIRECTORIES_TO_SHARE_NON_RECURSIVELY =
        FACTORY.createFileArraySetting("DIRECTORIES_TO_SHARE_NON_RECURSIVELY", new File[0]);
    
    /**
	 * File extensions that are shared.
	 */
    public static final StringSetting EXTENSIONS_TO_SHARE =
        FACTORY.createStringSetting("EXTENSIONS_TO_SEARCH_FOR", 
                                            DEFAULT_EXTENSIONS_TO_SHARE);
                                            
    /**
     * Sets the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted.   For example, if allowed==50, an incoming
     * connection has a 50-50 chance being accepted.  If allowed==100, all
     * incoming connections are accepted.
     */                                                        
    public static final IntSetting FREELOADER_ALLOWED =
        FACTORY.createIntSetting("FREELOADER_ALLOWED", 100);
    
    /**
     * Minimum the number of files a host must share to not be considered
     * a freeloader.  For example, if files==0, no host is considered a
     * freeloader.
     */
    public static final IntSetting FREELOADER_FILES =
        FACTORY.createIntSetting("FREELOADER_FILES", 1);
    
    /**
	 * The timeout value for persistent HTTP connections in milliseconds.
	 */
    public static final IntSetting PERSISTENT_HTTP_CONNECTION_TIMEOUT =
        FACTORY.createIntSetting("PERSISTENT_HTTP_CONNECTION_TIMEOUT", 15000);
    
    /**
     * Specifies whether or not completed uploads
     * should automatically be cleared from the upload window.
     */
    public static final BooleanSetting CLEAR_UPLOAD =
        FACTORY.createBooleanSetting("CLEAR_UPLOAD", true);
    
    /**
	 * Whether or not browsers should be allowed to perform uploads.
	 */
    public static final BooleanSetting ALLOW_BROWSER =
        FACTORY.createBooleanSetting("ALLOW_BROWSER", false);
        
    /**
     * Helper method left from SettingsManager.
     *
	 * Adds one directory to the directory string only if
     * it is a directory and is not already listed.
	 *
     * <p><b>Modifies:</b> DIRECTORIES_TO_SHARE</p>
     *
	 * @param dir  a <tt>File</tt> instance denoting the
	 *             abstract pathname of the new directory
	 *             to add
	 *
	 * @throws  IOException
	 *          if the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system
	 */
    public static final void addDirectory(File dir) throws IOException {
    
        if( dir == null ||
          !dir.isDirectory() ||
          !dir.exists() )
            throw new IOException();
        
        File[] shared = DIRECTORIES_TO_SHARE.getValue();
        Set set = new HashSet();
        for(int i = 0; i < shared.length; i++) {
            set.add(shared[i]);
        }
        
        set.add(dir);
        DIRECTORIES_TO_SHARE.setValue((File[])set.toArray(new File[0]));
    }
    
    /**
     * Helper method left from SettingsManager.
     *
	 * Sets the shared directories.  This method filters
     * out any duplicate or invalid directories in the string.
     * Note, however, that it does not currently filter out
     * listing subdirectories that have parent directories
     * also in the string.
	 *
     * <p><b>Modifies:</b> DIRECTORIES_TO_SHARE</p>
     *
	 * @param dirs an array of <tt>File</tt> instances denoting
	 *  the abstract pathnames of the shared directories
	 */
    public static final void setDirectories(File[] files) throws IOException {
    
        Set set = new HashSet();
        for(int i = 0; i < files.length; i++) {
            if( files[i] == null ||
              !files[i].isDirectory() ||
              !files[i].exists() )
                throw new IOException();            
            set.add(files[i]);
        }
        
        DIRECTORIES_TO_SHARE.setValue((File[])set.toArray(new File[0]));
    }
}
