package org.limewire.mojito.entity;

import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito.routing.Contact;

/**
 * A {@link PingEntity} is the result of a DHT <tt>PING</tt> operation.
 */
public interface PingEntity extends Entity {

    /**
     * Returns the remote host's {@link Contact} information.
     */
    public Contact getContact();
    
    /**
     * Returns my {@link SocketAddress}.
     */
    public SocketAddress getExternalAddress();
    
    /**
     * Returns the size of the DHT as estimated by the remote host.
     */
    public BigInteger getEstimatedSize();
}
