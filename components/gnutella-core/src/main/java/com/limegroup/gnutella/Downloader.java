package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import java.net.InetAddress;
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
     * Retuns the downloaded file.  If the
     * download isn't complete, returns whatever has been downloaded, taking
     * extra work (e.g., copying) if necessary to avoid file locking problems.  
     */
    public File getDownloadFragment();

    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP
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
     * Returns the address of the downloader, or null if this is not currently
     * connected. 
     */
    public String getHost();
	/**
     * Returns the port of the downloader, or null if this is not currently
     * connected. 
     */
    public int getPort();

	/**
	 * returns true if that chat is enambled
	 */
	public boolean chatEnabled();

    /**
     * Returns the number of pushes results this is waiting for. 
     *     @requires this in the WAITING_FOR_RETRY state
     */
    public int getPushesWaiting();

    /**
     * Returns the number of retries this is waiting for. 
     *     @requires this in the WAITING_FOR_RETRY state
     */
    public int getRetriesWaiting();

    /** 
     * Returns an LimeXMLDocument array describing the downloaded
     * file.
     */
    public LimeXMLDocument[] getXMLDocs();

	/**
	 * Inherited from the <tt>BandwidthTracker</tt> interface.
	 * Returns the number of bytes transferred by this <tt>UpdateTracker</tt>
	 * component since the last time this method was called.
	 *
	 * @return the number of bytes transferred by this <tt>UpdateTracker</tt>
	 *         component since the last time this method was called
	 */
	public int getNewBytesTransferred();

    /** call this method if you need to add a newly downloaded file to the
        FileManager repository.
    */
    public void addFileToFM(File f, String hash);
    
}
