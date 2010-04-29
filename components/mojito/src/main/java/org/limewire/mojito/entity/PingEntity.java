package org.limewire.mojito.entity;

import java.math.BigInteger;
import java.net.SocketAddress;

import org.limewire.mojito2.routing.Contact;

public interface PingEntity extends Entity {

    public Contact getContact();
    
    public SocketAddress getExternalAddress();
    
    public BigInteger getEstimatedSize();
}
