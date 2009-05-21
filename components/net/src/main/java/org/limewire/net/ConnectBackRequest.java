package org.limewire.net;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

public class ConnectBackRequest {
   
    private final Connectable address;
    private final GUID clientGuid;
    private final int supportedFWTVersion;

    public ConnectBackRequest(Connectable address, GUID clientGuid, int supportedFWTVersion) {
        this.address = Objects.nonNull(address, "address");
        this.clientGuid = Objects.nonNull(clientGuid, "clientGuid");
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
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
