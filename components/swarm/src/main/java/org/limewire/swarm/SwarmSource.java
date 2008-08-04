package org.limewire.swarm;

import java.net.SocketAddress;

import org.limewire.collection.IntervalSet;

public interface SwarmSource {

    SocketAddress getAddress();

    String getPath();
    
    IntervalSet getAvailableRanges();

}
