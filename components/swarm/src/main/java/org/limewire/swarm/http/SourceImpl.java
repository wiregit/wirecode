package org.limewire.swarm.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import org.limewire.collection.Range;

import com.limegroup.gnutella.http.URIUtils;

public class SourceImpl implements SwarmSource {

    private final SocketAddress socketAddress;

    private final String uri;

    private final Range range;

    public SourceImpl(URI uri, Range range) {
        this.socketAddress = new InetSocketAddress(uri.getHost(), URIUtils.getPort(uri));
        this.uri = uri.getPath();
        this.range = range;
    }

    public SourceImpl(URI uri, long fileSize) {
       this(uri,Range.createRange(0,fileSize-1));
    }

    public SocketAddress getAddress() {
        return socketAddress;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "Source for: " + socketAddress + ", uri: " + uri + ", id: "
                + System.identityHashCode(this);
    }

    public Range getRange() {
        return range;
    }
}
