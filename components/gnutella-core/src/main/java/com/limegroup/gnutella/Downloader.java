package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import java.net.InetAddress;
import com.sun.java.util.collections.Iterator;

/**
 * The downloader interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader {
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
     * Launches the downloaded file with the appropriate program.  If the
     * download isn't complete, launches whatever has been downloaded, taking
     * extra work (e.g., copying) if necessary to avoid file locking problems.  
     * Returns immediately, regardless of whether the launch worked or not.
     */
    public void launch();

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
     * Returns the subset of getHosts that supports chat, if any. 
     * Result meaningful only in the DOWNLOADING state.
     */
    public Iterator /* of Endpoint */ getChattableHosts();
}
