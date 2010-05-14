package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;

import org.limewire.mojito2.Context;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.routing.Contact;

import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public abstract class AbstractController implements Controller {
    
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

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
    }
    
    /**
     * 
     */
    protected boolean isLocalhost(Contact contact) {
        MojitoDHT dht = getMojitoDHT();
        if (dht == null) {
            return false;
        }
        
        Context context = dht.getContext();
        return context.isLocalNode(contact);
    }
}
