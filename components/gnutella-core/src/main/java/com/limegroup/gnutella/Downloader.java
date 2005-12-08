pbckage com.limegroup.gnutella;

import jbva.io.File;

/**
 * The downlobder interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop bnd resume downloads.  Note that there is no start method;
 * it is bssumed that the downloader will start as soon as it is instantiated.
 */
public interfbce Downloader extends BandwidthTracker {
    public stbtic final int QUEUED                  = 0;
    public stbtic final int CONNECTING              = 1;
    public stbtic final int DOWNLOADING             = 2;
    public stbtic final int BUSY                    = 3;
    public stbtic final int COMPLETE                = 4;
    public stbtic final int ABORTED                 = 5;
    /** When b downloader is in the GAVE_UP state, it can still try downloading
     *  if mbtching results pour in.  So you should 'stop' downloaders that are
     *  in the GAVE_UP stbte.
     */
    public stbtic final int GAVE_UP                 = 6;
    public stbtic final int DISK_PROBLEM 			= 7;
    public stbtic final int WAITING_FOR_RESULTS     = 8;
    public stbtic final int CORRUPT_FILE            = 9;
    public stbtic final int REMOTE_QUEUED           = 10;
    public stbtic final int HASHING                 = 11;
    public stbtic final int SAVING                  = 12;
    public stbtic final int WAITING_FOR_USER        = 13;
    public stbtic final int WAITING_FOR_CONNECTIONS = 14;
    public stbtic final int ITERATIVE_GUESSING      = 15;
    public stbtic final int IDENTIFY_CORRUPTION     = 16;
    public stbtic final int RECOVERY_FAILED         = 17;
    public stbtic final int PAUSED                  = 18;

    
    /**
     * Stops this.  If the downlobd is already stopped, does nothing.
     *     @modifies this
     */
    public void stop();
    
    /**
     * Pbuses this download.  If the download is already paused or stopped, does nothing.
     */
    public void pbuse();
    
    /**
     * Determines if this downlobd is paused or not.
     */
    public boolebn isPaused();
    
    /**
     * Determines if this downlobder is in an inactive state that can be resumed
     * from.
     */
    public boolebn isInactive();
	
	/**
     * Determines if this cbn have its saveLocation changed.
     */
    public boolebn isRelocatable();
    
    /**
     * Returns the inbctive priority of this download.
     */
    public int getInbctivePriority();

    /**
     * Resumes this.  If the downlobd is GAVE_UP, tries all locations again and
     * returns true.  If WAITING_FOR_RETRY, forces the retry immedibtely and
     * returns true.  If some other downlobder is currently downloading the
     * file, throws AlrebdyDowloadingException.  If WAITING_FOR_USER, then
     * lbunches another query.  Otherwise does nothing and returns false. 
     *     @modifies this 
     */
    public boolebn resume();
    
    /**
     * Returns the file thbt this downloader is using.
     * This is useful for retrieving informbtion from the file,
     * such bs the icon.
     *
     * This should NOT be used for plbying the file.  Instead,
     * use getDownlobdFragment for the reasons described in that
     * method.
     */
    public File getFile();

    /**
     * If this downlobd is not yet complete, returns a copy of the first
     * contiguous frbgment of the incomplete file.  (The copying helps prevent
     * file locking problems.)  Returns null if the downlobd hasn't started or
     * the copy fbiled.  If the download is complete, returns the saved file.
     *
     * @return the copied file frbgment, saved file, or null 
     */
    public File getDownlobdFragment();

    /**
     * Sets the directory where the file will be sbved. If saveLocation is null, 
     * the defbult save directory will be used.
     *
     * @pbram saveDirectory the directory where the file should be saved. null indicates the default.
     * @pbram fileName the name of the file to be saved in saveDirectory. null indicates the default.
     * @pbram overwrite is true if saving should be allowed to overwrite existing files
     * @throws SbveLocationException when the new file location could not be set
     */
    public void setSbveFile(File saveDirectory, String fileName, boolean overwrite) throws SaveLocationException;
    
    /** Returns the file under which the downlobd will be saved when complete.  Counterpart to setSaveFile. */
    public File getSbveFile();
    
    /**
     * Returns the stbte of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP, COULDNT_MOVE_TO_LIBRARY,
     * WAITING_FOR_RESULTS, or CORRUPT_FILE
     */
    public int getStbte();

    /**
     * Returns bn upper bound on the amount of time this will stay in the current
     * stbte, in seconds.  Returns Integer.MAX_VALUE if unknown.
     */
    public int getRembiningStateTime();

    /**
     * Returns the size of this file in bytes, i.e., the totbl amount to
     * downlobd. 
     */
    public int getContentLength();

    /**
     * Returns the bmount read by this so far, in bytes.
     */
    public int getAmountRebd();
    
    /**
     * @return the bmount of data pending to be written on disk (i.e. in cache, queue)
     */
    public int getAmountPending();
    
    /**
     * @return the number locbtions from which this is currently downloading.
     * Result mebningful only in the DOWNLOADING state.
     */
    public int getNumHosts();
    
    /**
     * Returns the vendor of the lbst downloading host.
     */
    public String getVendor();
	
	/**
	 * Returns b chat-enabled <tt>Endpoint</tt> instance for this
	 * <tt>Downlobder</tt>.
	 */
	public Endpoint getChbtEnabledHost();

	/**
	 * Returns whether or not there is b chat-enabled host available for
	 * this <tt>Downlobder</tt>.
	 *
	 * @return <tt>true</tt> if there is b chat-enabled host for this 
	 *  <tt>Downlobder</tt>, <tt>false</tt> otherwise
	 */
	public boolebn hasChatEnabledHost();

    /**
     * either trebts a corrupt file as normal file and saves it, or 
     * discbrds the corruptFile, depending on the value of delete.
     */
    public void discbrdCorruptDownload(boolean delete);

	/**
	 * Returns b browse-enabled <tt>Endpoint</tt> instance for this
	 * <tt>Downlobder</tt>.
	 */
	public RemoteFileDesc getBrowseEnbbledHost();

	/**
	 * Returns whether or not there is b browse-enabled host available for
	 * this <tt>Downlobder</tt>.
	 *
	 * @return <tt>true</tt> if there is b browse-enabled host for this 
	 *  <tt>Downlobder</tt>, <tt>false</tt> otherwise
	 */
	public boolebn hasBrowseEnabledHost();

    /**
     * Returns the position of the downlobd on the uploader, relavent only if
     * the downlobder is queueud.
     */
    public int getQueuePosition();
    
    /**
     * Return the number of vblidated alternate locations for this download
     */
    public int getNumberOfAlternbteLocations();
    
    /**
     * Return the number of invblid alternate locations for this download
     */
    public int getNumberOfInvblidAlternateLocations();
    
    /**
     * @return the number of possible hosts for this downlobd
     */
    public int getPossibleHostCount();

    /**
     * @return the number of hosts we tried which were busy. We will try these
     * lbter
     */
    public int getBusyHostCount();

    /**
     * @return the number of hosts we bre remotely queued on. 
     */
    public int getQueuedHostCount();
    
    /**
     * Determines if the downlobd is completed.
     */
    public boolebn isCompleted();
	
	/**
	 * @return the bmount of data that has been verified
	 */
	public int getAmountVerified();
	
	/**
	 * @return the chunk size for the given downlobd
	 */
	public int getChunkSize();
	
	/**
	 * @return the bmount of data lost due to corruption
	 */
	public int getAmountLost();

	/**
	 * Returns the shb1 urn associated with the file being downloaded, or
	 * <code>null</code> if there is none.
	 * @return
	 */
	public URN getSHA1Urn();
    
    /**
     * Sets b new attribute associated with the download.
     * The bttributes are used eg. by GUI to store some extra
     * informbtion about the download.
     * @pbram key A key used to identify the attribute.
     * @pbtam value The value of the key.
     * @return A prvious vblue of the attribute, or <code>null</code>
     *         if the bttribute wasn't set.
     */
    public Object setAttribute( String key, Object vblue );

    /**
     * Gets b value of attribute associated with the download.
     * The bttributes are used eg. by GUI to store some extra
     * informbtion about the download.
     * @pbram key A key which identifies the attribue.
     * @return The vblue of the specified attribute,
     *         or <code>null</code> if vblue was not specified.
     */
    public Object getAttribute( String key );

    /**
     * Removes bn attribute associated with this download.
     * @pbram key A key which identifies the attribute do remove.
     * @return A vblue of the attribute or <code>null</code> if
     *         bttribute was not set.
     */
    public Object removeAttribute( String key );
}

