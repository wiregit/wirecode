package org.limewire.mojito;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.routing.Contact;

/**
 * An interface that provides a facility to PING nodes with their 
 * {@link Contact} information.
 */
public interface ContactPinger {
    
    /**
     * Sends a PING to the given {@link Contact}
     */
    public DHTFuture<PingEntity> ping(Contact dst, 
            long timeout, TimeUnit unit);
}