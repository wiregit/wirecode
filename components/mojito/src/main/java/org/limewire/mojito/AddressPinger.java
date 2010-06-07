package org.limewire.mojito;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;

/**
 * An interface that provides a facility to PING nodes with their 
 * Internet address and port.
 */
public interface AddressPinger {

    /**
     * Sends a PING to the given address + {@code port} pair.
     */
    public DHTFuture<PingEntity> ping(String address, int port,
            long timeout, TimeUnit unit);
    
    /**
     * Sends a PING to the given {@link InetAddress} + {@code port} pair.
     */
    public DHTFuture<PingEntity> ping(InetAddress address, int port,
            long timeout, TimeUnit unit);
    
    /**
     * Sends a PING to the given {@link SocketAddress}.
     */
    public DHTFuture<PingEntity> ping(SocketAddress address, 
            long timeout, TimeUnit unit);
}
