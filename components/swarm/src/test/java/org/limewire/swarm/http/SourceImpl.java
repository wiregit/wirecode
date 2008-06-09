package org.limewire.swarm.http;

import java.net.SocketAddress;

public class SourceImpl implements Source {
    
    private final SocketAddress socketAddress;
    private final String uri;
    private final boolean rangeRequestSupported;

    public SourceImpl(SocketAddress socketAddress, String uri, boolean rangeRequestSupported) {
        this.socketAddress = socketAddress;
        this.uri = uri;
        this.rangeRequestSupported = rangeRequestSupported;
    }

    public SocketAddress getAddress() {
        return socketAddress;
    }

    public String getUri() {
        return uri;
    }

    public boolean isRangeRequestSupported() {
        return rangeRequestSupported;
    }
    
    @Override
    public String toString() {
        return "Source for: " + socketAddress + ", uri: " + uri + ", id: " + System.identityHashCode(this);
    }

}
