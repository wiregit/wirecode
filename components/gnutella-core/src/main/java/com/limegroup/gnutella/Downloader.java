package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import java.net.InetAddress;
import com.sun.java.util.collections.Iterator;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import java.io.File;

/**
 * The downloader interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader extends BandwidthTracker {
    public static final int QUEUED            = 0;
    public static final int CONNECTING        = 1;
    public static final int DOWNLOADING       = 2;
    public static final int WAITING_FOR_RETRY = 3;
    public static final int COMPLETE          = 4;
    public static final int ABORTED           = 5;
    public static final int GAVE_UP           = 6;
    public static final int COULDNT_MOVE_TO_LIBRARY = 7;
    public static final int WAITING_FOR_RESULTS = 8;
    public static final int CORRUPT_FILE      = 9;
    public static final int REMOTE_QUEUED     = 10;

    /**
     * Stops this.  If the download is already stopped, does nothing.
     *     @modifies this
     */
    public void stop();

    /**
     * Resumes this.  If the download is GAVE_UP, tries all locations again and
     * returns true.  If WAITING_FOR_RETRY, forces the retry immediately and
     * returns true.  If some other downloader is currently downloading the
     * file, throws AlreadyDowloadingException.  Otherwise does nothing and
     * returns false. 
     *     @modifies this 
     */
    public boolean resume() throws AlreadyDownloadingException;

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
     * Returns the number of retries this is waiting for. 
     * Result meaningful on in WAIT_FOR_RETRY state.
     */
     public int getRetriesWaiting();
    
    /**
     * Returns the last address that this tried to connect to, or null if it
     * hasn't tried any.  Useful primarily for CONNECTING.  
     */
    public String getAddress();
    
    /**
     * Returns the locations from which this is currently downloading, as an
     * iterator of Endpoint.  If this is swarming, may return multiple
     * addresses.  Result meaningful only in the DOWNLOADING state.
     */
    public Iterator /* of Endpoint */ getHosts();
	
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
    public String getQueuePosition();
}
