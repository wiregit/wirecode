package com.limegroup.gnutella.version;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.URN;

public interface UpdateHandler {

    /**
     * Initializes data as read from disk.
     */
    public void initialize();

    /**
     * Notification that a ReplyHandler has received a VM containing an update.
     */
    public void handleUpdateAvailable(final ReplyHandler rh, final int version);

    /**
     * Notification that a new message has arrived.
     * <p>
     * (The actual processing is passed of to be run in a different thread.
     *  All notifications are processed in the same thread, sequentially.)
     */
    public void handleNewData(final byte[] data, final ReplyHandler handler);

    /**
     * Retrieves the latest id available.
     */
    public int getLatestId();
    
    /**
     * @return the new version of the locally available update message
     */
    public int getNewVersion();
    
    /**
     * @return the key version of the locally available update message
     */
    public int getKeyVersion();

    /**
     * Gets the bytes to send on the wire.
     */
    public byte[] getLatestBytes();

    /**
     * Notifies this that an update with the given URN has finished downloading.
     * <p>
     * If this was our update, we notify the GUI.  It's OK if the user restarts
     * as the rest of the updates will be downloaded the next session.
     */
    public void inNetworkDownloadFinished(final URN urn, final boolean good);

    /**
     * Returns the final bytes that the old key system used for update responses.
     */
    public byte[] getOldUpdateResponse();

    /**
     * Returns the currently known update collection.
     * @return null if there is none
     */
    public UpdateCollection getUpdateCollection();
    
    
    public void addListener(EventListener<UpdateEvent> listener);

    public boolean removeListener(EventListener<UpdateEvent> listener);
    
    /**
     * @param version advertised simpp version by other client, -1 if client 
     * does not support simpp version
     * @param newVersion advertised newVersion by other client, -1 if client 
     * does not support new simpp version
     * @param keyVersion adtvertised keyVersion by other client, -1 if client
     * does not support key version
     * @return true if a simpp message should be requested
     */
    public boolean shouldRequestUpdateMessage(int version, int newVersion, int keyVersion);
}