package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import java.net.InetAddress;

/**
 * The downloader interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader extends Transfer {

    public static final int QUEUED            = 0;
    public static final int CONNECTING        = 1;
    public static final int DOWNLOADING       = 2;
    public static final int WAITING_FOR_RETRY = 3;
    public static final int COMPLETE          = 4;
    public static final int ABORTED           = 5;
    public static final int GAVE_UP           = 6;
    public static final int COULDNT_MOVE_TO_LIBRARY = 7;

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
     * Returns an upper bound on the amount of time this will stay in the current
     * state, in seconds.  Returns Integer.MAX_VALUE if unknown.
     */
    public int getRemainingStateTime();

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
}
