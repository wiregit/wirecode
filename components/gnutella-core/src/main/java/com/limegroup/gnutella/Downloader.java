package com.limegroup.gnutella;

/**
 * The downloader interface.  The UI maintains a list of Downloader's and uses
 * its methods to stop and resume downloads.  Note that there is no start method;
 * it is assumed that the downloader will start as soon as it is instantiated.
 */
public interface Downloader {
    public static final int NOT_CONNECTED = 0;
    public static final int CONNECTED     = 1;
    public static final int ERROR         = 2;
    public static final int COMPLETE      = 3;
    public static final int REQUESTING    = 4;
    public static final int QUEUED        = 5;
    public static final int WAITING_FOR_RETRY = 6;

    /**
     * Attempts to restart this.  If the download is already in progress, does
     * nothing.  Non-blocking.
     *     @modifies this
     */     
    public void resume();

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
}
