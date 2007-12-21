package com.limegroup.gnutella.version;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.URN;

public interface UpdateHandler {

    /**
     * Initializes data as read from disk.
     */
    public void initialize();

    /**
     * Sparks off an attempt to download any pending updates.
     */
    public void tryToDownloadUpdates();

    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int version);

    /**
     * Notification that a new message has arrived.
     *
     * (The actual processing is passed of to be run in a different thread.
     *  All notifications are processed in the same thread, sequentially.)
     */
    public void handleNewData(final byte[] data, final ReplyHandler handler);

    /**
     * Retrieves the latest id available.
     */
    public int getLatestId();

    /**
     * Gets the bytes to send on the wire.
     */
    public byte[] getLatestBytes();

    /**
     * Notifies this that an update with the given URN has finished downloading.
     * 
     * If this was our update, we notify the gui.  Its ok if the user restarts
     * as the rest of the updates will be downloaded the next session.
     */
    public void inNetworkDownloadFinished(final URN urn, final boolean good);

}