package org.limewire.swarm.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.http.URIUtils;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.impl.AbstractSwarmSource;
import org.limewire.swarm.impl.LoggingSwarmSourceListener;
import org.limewire.swarm.impl.ReconnectingSwarmSourceListener;


public class SwarmHttpSource extends AbstractSwarmSource {

    private final SocketAddress socketAddress;

    private final String path;

    private IntervalSet availableRanges = new IntervalSet();

    private final String id;

    public SwarmHttpSource(URI uri, Range range) {
        this.socketAddress = new InetSocketAddress(uri.getHost(), URIUtils.getPort(uri));
        this.path = uri.getPath();
        this.availableRanges.add(range);
        addListener(new ReconnectingSwarmSourceListener());
        addListener(new LoggingSwarmSourceListener());
        //addListener(new EchoSwarmSourceListener());
        id = uri.toString() + "-" + range.toString();
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

    public SwarmSourceType getType() {
        return SwarmSourceType.HTTP;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SwarmHttpSource)) {
            return false;
        }
        SwarmHttpSource source = (SwarmHttpSource) obj;
        return id.equals(source.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
