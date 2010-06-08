package org.limewire.mojito.message;

import java.math.BigInteger;
import java.net.SocketAddress;

/**
 * An interface for <tt>PING</tt> response {@link Message}s.
 */
public interface PingResponse extends ResponseMessage {

    /**
     * Returns this Nodes external address as reported by 
     * the remote Node.
     */
    public SocketAddress getExternalAddress();

    /**
     * Returns the remote Nodes estimation of the 
     * current DHT size.
     */
    public BigInteger getEstimatedSize();
}
