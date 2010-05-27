package org.limewire.mojito;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.routing.Contact;

/**
 * 
 */
public interface ContactPinger {
    
    /**
     * 
     */
    public DHTFuture<PingEntity> ping(Contact contact, 
            long timeout, TimeUnit unit);
}