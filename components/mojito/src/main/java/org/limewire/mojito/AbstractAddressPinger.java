package org.limewire.mojito;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;

public abstract class AbstractAddressPinger implements AddressPinger {

    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }
}
