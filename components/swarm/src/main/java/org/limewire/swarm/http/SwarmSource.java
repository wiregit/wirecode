package org.limewire.swarm.http;

import java.net.SocketAddress;

public interface SwarmSource {

    SocketAddress getAddress();

    String getUri();

    boolean isRangeRequestSupported();

}
