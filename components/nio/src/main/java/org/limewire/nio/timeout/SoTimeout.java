package org.limewire.nio.timeout;

import java.net.SocketException;

public interface SoTimeout {

    public int getSoTimeout() throws SocketException;
}
