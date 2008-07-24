package com.limegroup.gnutella.net.address.gnutella;

import java.util.Set;

import com.limegroup.gnutella.GUID;

public class PushProxyMediatorAddressImpl implements PushProxyMediatorAddress{
    private final GUID guid;
    private final Set<PushProxyAddress> proxies;

    public PushProxyMediatorAddressImpl(GUID guid, Set<PushProxyAddress> proxies) {
        this.guid = guid;
        this.proxies = proxies;
    }

    public GUID getClientID() {
        return guid;
    }

    public Set<PushProxyAddress> getPushProxies() {
        return proxies;
    }
    
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PushProxyMediatorAddress)) {
            return false;
        }
        PushProxyMediatorAddress other = (PushProxyMediatorAddress)o;
        return guid.equals(other.getClientID()) && proxies.equals(other.getPushProxies());
    }
    
    public int hashCode() {
        // TODO push up - but what are the side affects,
        // TODO especially for existing callers.
        int hash = 7;
        hash = hash * 31 + guid.hashCode();
        hash = hash * 31 + proxies.hashCode();
        return hash;
    }
}
