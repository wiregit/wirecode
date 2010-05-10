package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;

import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.routing.Contact;

import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

abstract class AbstractController implements Controller {
    
    private final DHTMode mode;
    
    public AbstractController(DHTMode mode) {
        this.mode = mode;
    }
    
    @Override
    public DHTMode getMode() {
        return mode;
    }
    
    @Override
    public boolean isMode(DHTMode other) {
        return mode == other;
    }
    
    @Override
    public void addressChanged() {
        
    }
    
    @Override
    public void addActiveNode(SocketAddress address) {
        
    }
    
    @Override
    public void addPassiveNode(SocketAddress address) {
        
    }
    
    @Override
    public Contact[] getActiveContacts(int max) {
        return new Contact[0];
    }
    
    @Override
    public void handleCollision(CollisionException ex) {
        
    }
}
