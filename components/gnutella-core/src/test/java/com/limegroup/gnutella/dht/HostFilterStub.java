package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import org.limewire.mojito.util.HostFilter;

class HostFilterStub implements HostFilter {
    
    @Override
    public boolean allow(SocketAddress addr) {
        return true;
    }
}