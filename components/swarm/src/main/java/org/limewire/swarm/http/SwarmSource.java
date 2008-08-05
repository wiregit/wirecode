package org.limewire.swarm.http;

import java.net.SocketAddress;

import org.limewire.collection.IntervalSet;

public interface SwarmSource {

    SocketAddress getAddress();

    String getPath();
    
    IntervalSet getAvailableRanges();

}
