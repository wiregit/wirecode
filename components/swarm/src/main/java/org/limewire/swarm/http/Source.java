package org.limewire.swarm.http;

import java.net.SocketAddress;

public interface Source {

    SocketAddress getAddress();

    String getUri();

    boolean isRangeRequestSupported();

}
