package org.limewire.mojito.entity;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.routing.Contact;

public class DefaultPingEntity extends AbstractEntity implements PingEntity {

    private final Contact contact;
    
    private final SocketAddress externalAddress;
    
    private final BigInteger estimatedSize;
    
    public DefaultPingEntity(Contact contact, 
            SocketAddress externalAddress, 
            BigInteger estimatedSize, 
            long time, TimeUnit unit) {
        super(time, unit);
        
        this.contact = contact;
        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public BigInteger getEstimatedSize() {
        return estimatedSize;
    }

    @Override
    public SocketAddress getExternalAddress() {
        return externalAddress;
    }
}
