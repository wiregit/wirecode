package com.limegroup.gnutella.dht;

import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.routing.Contact;

/**
 * Callback interface for the {@link BootstrapWorker}.
 */
public interface BootstrapListener {
    
    /**
     * Called every time the {@link BootstrapWorker} is attempting to 
     * bootstrap the localhost Node.
     */
    public void handleConnecting();
    
    /**
     * Called on success or error.
     */
    public void handleConnected(boolean success);
    
    /**
     * Called if an another {@link Contact} in the DHT collides
     * with out localhost Node.
     */
    public void handleCollision(CollisionException ex);
}