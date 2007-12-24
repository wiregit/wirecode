package com.limegroup.gnutella;

import com.limegroup.gnutella.BrowseHostHandler.PushRequestDetails;
import com.limegroup.gnutella.downloader.PushedSocketHandler;

public interface BrowseHostHandlerManager extends PushedSocketHandler {

	public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID);

    public interface BrowseHostCallback {
        void putInfo(GUID _serventid, PushRequestDetails details);
    }

    public void initialize();

}