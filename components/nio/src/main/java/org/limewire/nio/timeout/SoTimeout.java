package org.limewire.nio.timeout;

import java.net.SocketException;

/**
 * Defines the interface to get the socket timeout value in milliseconds. 
 * Returning 0 implies the timeout option is disabled (i.e., timeout of 
 * infinity). Returning a negative value implies an error.
 */

public interface SoTimeout {

    public int getSoTimeout() throws SocketException;
}
