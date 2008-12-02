package org.limewire.net;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;


public class ConnectRequestEvent {

    private final Connectable address;
    private final GUID clientGuid;
    private final int supportedFWTVersion;

    public ConnectRequestEvent(Connectable address, GUID clientGuid, int supportedFWTVersion) {
        this.address = address;
        this.clientGuid = clientGuid;
        this.supportedFWTVersion = supportedFWTVersion;
    }
    
    public Connectable getAddress() {
        return address;
    }
    
    public GUID getClientGuid() {
        return clientGuid;
    }
    
    public int getSupportedFWTVersion() {
        return supportedFWTVersion;
    }
    
}
