package org.limewire.mojito2;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.routing.Contact;

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