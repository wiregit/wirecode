package com.limegroup.gnutella;

import org.limewire.io.Address;

import com.limegroup.gnutella.BrowseHostHandler.PushRequestDetails;
import com.limegroup.gnutella.downloader.PushedSocketHandler;

public interface BrowseHostHandlerManager extends PushedSocketHandler {

	public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID);

    public interface BrowseHostCallback {
        void putInfo(GUID _serventid, PushRequestDetails details);
    }

    public void initialize();

    /**
     * Creates a browse host handler with a session guid <code>browseGuid</code>.
     * 
     * This is used for browses on {@link Address} objects, call 
     * {@link BrowseHostHandler#browseHost(Address)} on the created browse
     * host handler.
     */
    public BrowseHostHandler createBrowseHostHandler(GUID browseGuid);

}