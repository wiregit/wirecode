package com.limegroup.gnutella.net.address;

import java.util.Set;

import org.limewire.io.Connectable;

import com.limegroup.gnutella.GUID;

public class PushProxyMediatorAddressImpl implements PushProxyMediatorAddress {
    
    private final GUID guid;
    private final Set<Connectable> proxies;

    public PushProxyMediatorAddressImpl(GUID guid, Set<Connectable> proxies) {
        this.guid = guid;
        this.proxies = proxies;
    }

    public GUID getClientID() {
        return guid;
    }

    public Set<Connectable> getPushProxies() {
        return proxies;
    }
    
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PushProxyMediatorAddress)) {
            return false;
        }
        PushProxyMediatorAddress other = (PushProxyMediatorAddress)o;
        // TODO proxies.equals doesn't work, since IpPorts don't override equals as expected
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
