package com.limegroup.gnutella;

import java.net.Socket;

import com.limegroup.gnutella.BrowseHostHandler.PushRequestDetails;

public interface BrowseHostHandlerManager {

	public BrowseHostHandler createBrowseHostHandler(GUID guid, GUID serventID);
    public boolean handlePush(int index, GUID serventID, final Socket socket);

    public interface BrowseHostCallback {
        void putInfo(GUID _serventid, PushRequestDetails details);
    }

}