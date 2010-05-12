package org.limewire.mojito2;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.PingEntity;

/**
 * 
 */
public interface AddressPinger {

    /**
     * 
     */
    public DHTFuture<PingEntity> ping(String address, int port,
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(InetAddress address, int port,
            long timeout, TimeUnit unit);
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(SocketAddress address, 
            long timeout, TimeUnit unit);
}
