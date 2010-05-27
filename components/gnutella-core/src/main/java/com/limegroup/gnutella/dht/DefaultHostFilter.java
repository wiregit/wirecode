package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import org.limewire.mojito.util.HostFilter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * A simple implementation of {@link HostFilter} that delegates 
 * all calls to {@link IPFilter}.
 */
class DefaultHostFilter implements HostFilter {
    
    private final Provider<IPFilter> ipFilter;
    
    @Inject
    public DefaultHostFilter(Provider<IPFilter> ipFilter) {
        this.ipFilter = ipFilter;
    }
    
    @Override
    public boolean allow(SocketAddress addr) {
        return ipFilter.get().allow(addr);
    }
}
