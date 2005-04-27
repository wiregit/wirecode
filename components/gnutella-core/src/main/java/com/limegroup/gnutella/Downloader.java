package com.limegroup.gnutella;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;

import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.downloader.IllegalDownloaderStateException;

/**
 * The downloader interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader extends BandwidthTracker {
    public static final int QUEUED                  = 0;
    public static final int CONNECTING              = 1;
    public static final int DOWNLOADING             = 2;
    public static final int WAITING_FOR_RETRY       = 3;
    public static final int COMPLETE                = 4;
    public static final int ABORTED                 = 5;
    /** When a downloader is in the GAVE_UP state, it can still try downloading
     *  if matching results pour in.  So you should 'stop' downloaders that are
     *  in the GAVE_UP state.
     */
    public static final int GAVE_UP                 = 6;
    public static final int DISK_PROBLEM 			= 7;
    public static final int WAITING_FOR_RESULTS     = 8;
    public static final int CORRUPT_FILE            = 9;
    public static final int REMOTE_QUEUED           = 10;
    public static final int HASHING                 = 11;
    public static final int SAVING                  = 12;
    public static final int WAITING_FOR_USER        = 13;
    public static final int WAITING_FOR_CONNECTIONS = 14;
    public static final int ITERATIVE_GUESSING      = 15;
    public static final int IDENTIFY_CORRUPTION     = 16;
    public static final int RECOVERY_FAILED         = 17;
    public static final int PAUSED                  = 18;

    /////////////// Enumerated Return Codes for setSaveLocation ///////////////////
    /** setSaveLocation succeeded */
    public static final int SAVE_LOCATION_OK = 0;
    /** setSaveLocation was called too late to save file in new place */
    public static final int SAVE_LOCATION_ALREADY_SAVED = 1;
    /** setSaveLocation was passed a directory File where files cannot be created */
    public static final int SAVE_LOCATION_DIRECTORY_NOT_WRITEABLE = 2;
    /** setSaveLocation was passed a File with a non-existant parent */
    public static final int SAVE_LOCATION_HAS_NO_PARENT = 3; 
    /** setSaveLocation was passed a File that already exists */
    public static final int SAVE_LOCATION_ALREADY_EXISTS = 4;
    
    /**
     * Stops this.  If the download is already stopped, does nothing.
     *     @modifies this
     */
    public void stop();
    
    /**
     * Pauses this download.  If the download is already paused or stopped, does nothing.
     */
    public void pause();
    
    /**
     * Determines if this download is paused or not.
     */
    public boolean isPaused();
    
    /**
     * Determines if this downloader is in an inactive state that can be resumed
     * from.
     */
    public boolean isInactive();
	
	/**
     * Determines if this can have its saveLocation changed.
     */
    public boolean isRelocatable();
    
    /**
     * Returns the inactive priority of this download.
     */
    public int getInactivePriority();

    /**
     * Resumes this.  If the download is GAVE_UP, tries all locations again and
     * returns true.  If WAITING_FOR_RETRY, forces the retry immediately and
     * returns true.  If some other downloader is currently downloading the
     * file, throws AlreadyDowloadingException.  If WAITING_FOR_USER, then
     * launches another query.  Otherwise does nothing and returns false. 
     *     @modifies this 
     */
    public boolean resume() throws AlreadyDownloadingException;
    
    /**
     * Returns the file that this downloader is using.
     * This is useful for retrieving information from the file,
     * such as the icon.
     *
     * This should NOT be used for playing the file.  Instead,
     * use getDownloadFragment for the reasons described in that
     * method.
     */
    public File getFile();

    /**
     * If this download is not yet complete, returns a copy of the first
     * contiguous fragment of the incomplete file.  (The copying helps prevent
     * file locking problems.)  Returns null if the download hasn't started or
     * the copy failed.  If the download is complete, returns the saved file.
     *
     * @return the copied file fragment, saved file, or null 
     */
    public File getDownloadFragment();

    /**
     * Sets the location where the file will be saved.  If saveLocation is 
     * a directory, then the file will be saved in that directory.  If 
     * saveLocation is a regular file, then the file will be saved as the
     * saveLocation file. If saveLocation is null, the default saveLocation
     * will be used.
     *
     * @parm saveLocation the location where the file should be saved
     * @return Downloader.SAVE_LOCATION_SUCCESS upon success, another return code from Downloader.SAVE_LOCATION_* upon failure.
     */
    public int setSaveLocation(File saveLocation);

    
    /** 
     * This method is used to determine where the file will be saved once downloaded.
     *
     * @return A File representation of the directory or regular file where this file will be saved.  null indicates the program-wide default save directory.
     */
    public File getSaveLocation();
    
    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP, COULDNT_MOVE_TO_LIBRARY,
     * WAITING_FOR_RESULTS, or CORRUPT_FILE
     */
    public int getState();

    /**
     * Returns an upper bound on the amount of time this will stay in the current
     * state, in seconds.  Returns Integer.MAX_VALUE if unknown.
     */
    public int getRemainingStateTime();

    /** 
     * Returns the name of the current or last file this is downloading, or null
     * in the rare case that this has no more files to download.  (This might
     * happen if this has been stopped.)
     */
    public String getFileName();

    /**
     * Returns the size of this file in bytes, i.e., the total amount to
     * download. 
     */
    public int getContentLength();

    /**
     * Returns the amount read by this so far, in bytes.
     */
    public int getAmountRead();
    
    /**
     * Returns the locations from which this is currently downloading, as an
     * iterator of Endpoint.  If this is swarming, may return multiple
     * addresses.  Result meaningful only in the DOWNLOADING state.
     */
    public Iterator /* of Endpoint */ getHosts();
    
    /**
     * Returns the vendor of the last downloading host.
     */
    public String getVendor();
	
	/**
	 * Returns a chat-enabled <tt>Endpoint</tt> instance for this
	 * <tt>Downloader</tt>.
	 */
	public Endpoint getChatEnabledHost();

	/**
	 * Returns whether or not there is a chat-enabled host available for
	 * this <tt>Downloader</tt>.
	 *
	 * @return <tt>true</tt> if there is a chat-enabled host for this 
	 *  <tt>Downloader</tt>, <tt>false</tt> otherwise
	 */
	public boolean hasChatEnabledHost();

    /**
     * either treats a corrupt file as normal file and saves it, or 
     * discards the corruptFile, depending on the value of delete.
     */
    public void discardCorruptDownload(boolean delete);

	/**
	 * Returns a browse-enabled <tt>Endpoint</tt> instance for this
	 * <tt>Downloader</tt>.
	 */
	public RemoteFileDesc getBrowseEnabledHost();

	/**
	 * Returns whether or not there is a browse-enabled host available for
	 * this <tt>Downloader</tt>.
	 *
	 * @return <tt>true</tt> if there is a browse-enabled host for this 
	 *  <tt>Downloader</tt>, <tt>false</tt> otherwise
	 */
	public boolean hasBrowseEnabledHost();

    /**
     * Returns the position of the download on the uploader, relavent only if
     * the downloader is queueud.
     */
    public int getQueuePosition();
    
    /**
     * Return the number of validated alternate locations for this download
     */
    public int getNumberOfAlternateLocations();
    
    /**
     * Return the number of invalid alternate locations for this download
     */
    public int getNumberOfInvalidAlternateLocations();
    
    /**
     * @return the number of possible hosts for this download
     */
    public int getPossibleHostCount();

    /**
     * @return the number of hosts we tried which were busy. We will try these
     * later
     */
    public int getBusyHostCount();

    /**
     * @return the number of hosts we are remotely queued on. 
     */
    public int getQueuedHostCount();
    
    /**
     * Determines if the download is completed.
     */
    public boolean isCompleted();
	
	/**
	 * @return the amount of data that has been verified
	 */
	public int getAmountVerified();
	
	/**
	 * @return the chunk size for the given download
	 */
	public int getChunkSize();
	
	/**
	 * @return the amount of data lost due to corruption
	 */
	public int getAmountLost();


}

