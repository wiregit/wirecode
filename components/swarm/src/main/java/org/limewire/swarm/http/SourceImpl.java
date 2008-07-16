package org.limewire.swarm.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import com.limegroup.gnutella.http.URIUtils;

public class SourceImpl implements SwarmSource {
    
    private final SocketAddress socketAddress;
    private final String uri;
    private final boolean rangeRequestSupported;

    public SourceImpl(URI uri, boolean rangeRequestSupported) {
        this.socketAddress = new InetSocketAddress(uri.getHost(), URIUtils.getPort(uri));
        this.uri = uri.getPath();
        this.rangeRequestSupported = rangeRequestSupported;
    }
    
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
