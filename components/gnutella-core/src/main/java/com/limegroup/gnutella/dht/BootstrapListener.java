package com.limegroup.gnutella.dht;

import org.limewire.mojito.entity.CollisionException;

/**
 * 
 */
public interface BootstrapListener {
    
    /**
     * 
     */
    public void handleConnecting();
    
    /**
     * 
     */
    public void handleConnected(boolean success);
    
    /**
     * 
     */
    public void handleCollision(CollisionException ex);
}