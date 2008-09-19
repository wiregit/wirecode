package com.limegroup.gnutella.net.address;

import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.rudp.RUDPUtils;

import com.limegroup.gnutella.GUID;

public class FirewalledAddress implements Address {
    
    private final Connectable publicAddress;
    private final Connectable privateAddress;
    private final Set<Connectable> pushProxies;
    private final int fwtVersion;
    private final GUID clientGuid;
    
    public FirewalledAddress(Connectable publicAddress, Connectable privateAddress, GUID clientGuid, Set<Connectable> pushProxies, int fwtVersion) {
        this.publicAddress = publicAddress;
        this.privateAddress = privateAddress;
        this.clientGuid = clientGuid;
        this.pushProxies = pushProxies;
        this.fwtVersion = fwtVersion;
    }
    
    public Connectable getPublicAddress() {
        return publicAddress;
    }
    
    public Connectable getPrivateAddress() {
        return privateAddress;
    }
    
    public Set<Connectable> getPushProxies() {
        return pushProxies;
    }
    
    /**
     * Returns the version for reliable udp or 0 if it is not supported.
     * 
     * See {@link RUDPUtils#VERSION}.
     */
    public int getFwtVersion() {
        return fwtVersion;
    }
    
    public GUID getClientGuid() {
        return clientGuid;
    }
}
