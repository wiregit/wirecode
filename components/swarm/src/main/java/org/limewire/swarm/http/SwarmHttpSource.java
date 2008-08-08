package org.limewire.swarm.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmSource;

import com.limegroup.gnutella.http.URIUtils;

public class SwarmHttpSource implements SwarmSource {

    private final SocketAddress socketAddress;

    private final String path;

    private IntervalSet availableRanges = new IntervalSet();

    public SwarmHttpSource(URI uri, Range range) {
        this.socketAddress = new InetSocketAddress(uri.getHost(), URIUtils.getPort(uri));
        this.path = uri.getPath();
        this.availableRanges.add(range);
    }

    public SwarmHttpSource(URI uri, long fileSize) {
        this(uri, Range.createRange(0, fileSize - 1));
    }

    public SocketAddress getAddress() {
        return socketAddress;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Source for: " + socketAddress + ", uri: " + path + ", id: "
                + System.identityHashCode(this);
    }

    public IntervalSet getAvailableRanges() {
        return availableRanges;
    }

    public Type getType() {
        return SwarmSource.Type.HTTP;
    }
}
