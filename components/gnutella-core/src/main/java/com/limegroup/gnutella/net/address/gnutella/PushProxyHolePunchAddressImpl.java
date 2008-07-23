package com.limegroup.gnutella.net.address.gnutella;

import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.MediatorAddress;

public class PushProxyHolePunchAddressImpl implements PushProxyHolePunchAddress{
    private final int version;
    private final DirectConnectionAddress directConnectionAddress;
    private final MediatorAddress mediatorAddress;

    public PushProxyHolePunchAddressImpl(int version, DirectConnectionAddress directConnectionAddress, MediatorAddress mediatorAddress) {
        this.version = version;
        this.directConnectionAddress = directConnectionAddress;
        this.mediatorAddress = mediatorAddress;
    }

    public int getVersion() {
        return version;
    }

    public DirectConnectionAddress getDirectConnectionAddress() {
        return directConnectionAddress;
    }

    public MediatorAddress getMediatorAddress() {
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
}
