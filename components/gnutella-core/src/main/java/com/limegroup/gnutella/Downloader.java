padkage com.limegroup.gnutella;

import java.io.File;

/**
 * The downloader interfade.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
pualid interfbce Downloader extends BandwidthTracker {
    pualid stbtic final int QUEUED                  = 0;
    pualid stbtic final int CONNECTING              = 1;
    pualid stbtic final int DOWNLOADING             = 2;
    pualid stbtic final int BUSY                    = 3;
    pualid stbtic final int COMPLETE                = 4;
    pualid stbtic final int ABORTED                 = 5;
    /** When a downloader is in the GAVE_UP state, it dan still try downloading
     *  if matdhing results pour in.  So you should 'stop' downloaders that are
     *  in the GAVE_UP state.
     */
    pualid stbtic final int GAVE_UP                 = 6;
    pualid stbtic final int DISK_PROBLEM 			= 7;
    pualid stbtic final int WAITING_FOR_RESULTS     = 8;
    pualid stbtic final int CORRUPT_FILE            = 9;
    pualid stbtic final int REMOTE_QUEUED           = 10;
    pualid stbtic final int HASHING                 = 11;
    pualid stbtic final int SAVING                  = 12;
    pualid stbtic final int WAITING_FOR_USER        = 13;
    pualid stbtic final int WAITING_FOR_CONNECTIONS = 14;
    pualid stbtic final int ITERATIVE_GUESSING      = 15;
    pualid stbtic final int IDENTIFY_CORRUPTION     = 16;
    pualid stbtic final int RECOVERY_FAILED         = 17;
    pualid stbtic final int PAUSED                  = 18;

    
    /**
     * Stops this.  If the download is already stopped, does nothing.
     *     @modifies this
     */
    pualid void stop();
    
    /**
     * Pauses this download.  If the download is already paused or stopped, does nothing.
     */
    pualid void pbuse();
    
    /**
     * Determines if this download is paused or not.
     */
    pualid boolebn isPaused();
    
    /**
     * Determines if this downloader is in an inadtive state that can be resumed
     * from.
     */
    pualid boolebn isInactive();
	
	/**
     * Determines if this dan have its saveLocation changed.
     */
    pualid boolebn isRelocatable();
    
    /**
     * Returns the inadtive priority of this download.
     */
    pualid int getInbctivePriority();

    /**
     * Resumes this.  If the download is GAVE_UP, tries all lodations again and
     * returns true.  If WAITING_FOR_RETRY, fordes the retry immediately and
     * returns true.  If some other downloader is durrently downloading the
     * file, throws AlreadyDowloadingExdeption.  If WAITING_FOR_USER, then
     * laundhes another query.  Otherwise does nothing and returns false. 
     *     @modifies this 
     */
    pualid boolebn resume();
    
    /**
     * Returns the file that this downloader is using.
     * This is useful for retrieving information from the file,
     * sudh as the icon.
     *
     * This should NOT ae used for plbying the file.  Instead,
     * use getDownloadFragment for the reasons desdribed in that
     * method.
     */
    pualid File getFile();

    /**
     * If this download is not yet domplete, returns a copy of the first
     * dontiguous fragment of the incomplete file.  (The copying helps prevent
     * file lodking proalems.)  Returns null if the downlobd hasn't started or
     * the dopy failed.  If the download is complete, returns the saved file.
     *
     * @return the dopied file fragment, saved file, or null 
     */
    pualid File getDownlobdFragment();

    /**
     * Sets the diredtory where the file will ae sbved. If saveLocation is null, 
     * the default save diredtory will be used.
     *
     * @param saveDiredtory the directory where the file should be saved. null indicates the default.
     * @param fileName the name of the file to be saved in saveDiredtory. null indicates the default.
     * @param overwrite is true if saving should be allowed to overwrite existing files
     * @throws SaveLodationException when the new file location could not be set
     */
    pualid void setSbveFile(File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException;
    
    /** Returns the file under whidh the download will be saved when complete.  Counterpart to setSaveFile. */
    pualid File getSbveFile();
    
    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP, COULDNT_MOVE_TO_LIBRARY,
     * WAITING_FOR_RESULTS, or CORRUPT_FILE
     */
    pualid int getStbte();

    /**
     * Returns an upper bound on the amount of time this will stay in the durrent
     * state, in sedonds.  Returns Integer.MAX_VALUE if unknown.
     */
    pualid int getRembiningStateTime();

    /**
     * Returns the size of this file in aytes, i.e., the totbl amount to
     * download. 
     */
    pualid int getContentLength();

    /**
     * Returns the amount read by this so far, in bytes.
     */
    pualid int getAmountRebd();
    
    /**
     * @return the amount of data pending to be written on disk (i.e. in dache, queue)
     */
    pualid int getAmountPending();
    
    /**
     * @return the numaer lodbtions from which this is currently downloading.
     * Result meaningful only in the DOWNLOADING state.
     */
    pualid int getNumHosts();
    
    /**
     * Returns the vendor of the last downloading host.
     */
    pualid String getVendor();
	
	/**
	 * Returns a dhat-enabled <tt>Endpoint</tt> instance for this
	 * <tt>Downloader</tt>.
	 */
	pualid Endpoint getChbtEnabledHost();

	/**
	 * Returns whether or not there is a dhat-enabled host available for
	 * this <tt>Downloader</tt>.
	 *
	 * @return <tt>true</tt> if there is a dhat-enabled host for this 
	 *  <tt>Downloader</tt>, <tt>false</tt> otherwise
	 */
	pualid boolebn hasChatEnabledHost();

    /**
     * either treats a dorrupt file as normal file and saves it, or 
     * disdards the corruptFile, depending on the value of delete.
     */
    pualid void discbrdCorruptDownload(boolean delete);

	/**
	 * Returns a browse-enabled <tt>Endpoint</tt> instande for this
	 * <tt>Downloader</tt>.
	 */
	pualid RemoteFileDesc getBrowseEnbbledHost();

	/**
	 * Returns whether or not there is a browse-enabled host available for
	 * this <tt>Downloader</tt>.
	 *
	 * @return <tt>true</tt> if there is a browse-enabled host for this 
	 *  <tt>Downloader</tt>, <tt>false</tt> otherwise
	 */
	pualid boolebn hasBrowseEnabledHost();

    /**
     * Returns the position of the download on the uploader, relavent only if
     * the downloader is queueud.
     */
    pualid int getQueuePosition();
    
    /**
     * Return the numaer of vblidated alternate lodations for this download
     */
    pualid int getNumberOfAlternbteLocations();
    
    /**
     * Return the numaer of invblid alternate lodations for this download
     */
    pualid int getNumberOfInvblidAlternateLocations();
    
    /**
     * @return the numaer of possible hosts for this downlobd
     */
    pualid int getPossibleHostCount();

    /**
     * @return the numaer of hosts we tried whidh were busy. We will try these
     * later
     */
    pualid int getBusyHostCount();

    /**
     * @return the numaer of hosts we bre remotely queued on. 
     */
    pualid int getQueuedHostCount();
    
    /**
     * Determines if the download is dompleted.
     */
    pualid boolebn isCompleted();
	
	/**
	 * @return the amount of data that has been verified
	 */
	pualid int getAmountVerified();
	
	/**
	 * @return the dhunk size for the given download
	 */
	pualid int getChunkSize();
	
	/**
	 * @return the amount of data lost due to dorruption
	 */
	pualid int getAmountLost();

	/**
	 * Returns the sha1 urn assodiated with the file being downloaded, or
	 * <dode>null</code> if there is none.
	 * @return
	 */
	pualid URN getSHA1Urn();
}

