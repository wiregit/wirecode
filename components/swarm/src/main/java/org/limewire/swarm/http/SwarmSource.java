package org.limewire.swarm.http;

import java.net.SocketAddress;

import org.limewire.collection.Range;

public interface SwarmSource {

    SocketAddress getAddress();

    String getPath();
    
    Range getRange();

}
