package com.limegroup.gnutella;

import java.net.InetAddress;

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

//      /**
//       * Attempts to restart this.  If the download is already in progress, does
//       * nothing.  Non-blocking.
//       *     @modifies this
//       */     
//      public void resume();

    /**
     * Stops this.  If the download is already stopped, does nothing.
     *     @modifies this
     */
    public void stop();

    /**
     * Returns the state of this: one of NOT_CONNECTED, CONNECTED, ERROR, COMPLETE,
     * REQUESTING, QUEUED, WAITING_FOR_RETRY.
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
}
