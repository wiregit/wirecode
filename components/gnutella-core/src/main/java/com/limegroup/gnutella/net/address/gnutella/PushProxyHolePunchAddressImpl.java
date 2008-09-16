package com.limegroup.gnutella.net.address.gnutella;

import org.limewire.io.Connectable;

public class PushProxyHolePunchAddressImpl implements PushProxyHolePunchAddress{
    
    private final int version;
    private final Connectable directConnectionAddress;
    private final PushProxyMediatorAddress mediatorAddress;

    public PushProxyHolePunchAddressImpl(int version, Connectable directConnectionAddress, PushProxyMediatorAddress mediatorAddress) {
        this.version = version;
        this.directConnectionAddress = directConnectionAddress;
        this.mediatorAddress = mediatorAddress;
    }

    public int getVersion() {
        return version;
    }

    public Connectable getDirectConnectionAddress() {
        return directConnectionAddress;
    }

    public PushProxyMediatorAddress getMediatorAddress() {
        return mediatorAddress;
    }
    
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PushProxyHolePunchAddress)) {
            return false;
        }
        PushProxyHolePunchAddress other = (PushProxyHolePunchAddress)o;
        return version == other.getVersion() && directConnectionAddress.equals(other.getDirectConnectionAddress()) 
                && mediatorAddress.equals(other.getMediatorAddress());
    }
    
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + version;
        hash = hash * 31 + directConnectionAddress.hashCode();
        hash = hash * 31 + mediatorAddress.hashCode();
        return hash;
    }
}
