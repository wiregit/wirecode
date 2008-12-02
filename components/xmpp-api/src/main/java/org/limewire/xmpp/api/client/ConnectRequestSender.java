package org.limewire.xmpp.api.client;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;

public interface ConnectRequestSender {

    public void send(String userId, Connectable address, GUID clientGuid, int supportedFWTVersion);
    
}
