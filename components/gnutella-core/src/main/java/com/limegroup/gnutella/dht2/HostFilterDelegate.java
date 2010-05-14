package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;

import org.limewire.mojito2.util.HostFilter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * 
 */
class HostFilterDelegate implements HostFilter {
    
    private final Provider<IPFilter> ipFilter;
    
    @Inject
    public HostFilterDelegate(Provider<IPFilter> ipFilter) {
        this.ipFilter = ipFilter;
    }
    
    @Override
    public boolean allow(SocketAddress addr) {
        return ipFilter.get().allow(addr);
    }
}
