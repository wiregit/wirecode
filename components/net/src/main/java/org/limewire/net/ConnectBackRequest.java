package org.limewire.net;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;

public class ConnectBackRequest {
   
    private final Connectable address;
    private final GUID clientGuid;
    private final int supportedFWTVersion;
    
    private int hashCode = -1;

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
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConnectBackRequest) {
            ConnectBackRequest other = (ConnectBackRequest)obj;
            return address.equals(other.address) && clientGuid.equals(other.clientGuid) && supportedFWTVersion == other.supportedFWTVersion;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        int hash = hashCode;
        if (hash == -1) {
            hash = address.hashCode();
            hash = hash * 31 + clientGuid.hashCode();
            hash = hash * 31 + supportedFWTVersion;
            hashCode = hash;
        }
        return hash;
    }
}
