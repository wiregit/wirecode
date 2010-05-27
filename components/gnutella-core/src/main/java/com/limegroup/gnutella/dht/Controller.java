package com.limegroup.gnutella.dht;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * 
 */
public interface Controller extends Closeable, ConnectionLifecycleListener {

    /**
     * 
     */
    public DHTMode getMode();
    
    /**
     * 
     */
    public boolean isMode(DHTMode other);
    
    /**
     * 
     */
    public MojitoDHT getMojitoDHT();
    
    /**
     * 
     */
    public boolean isRunning();
    
    /**
     * 
     */
    public boolean isReady();
    
    /**
     * 
     */
    public void start() throws IOException;
    
    /**
     * 
     */
    public void addressChanged();
    
    /**
     * 
     */
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity> get(EntityKey key);
    
    /**
     * 
     */
    public DHTFuture<ValueEntity[]> getAll(EntityKey key);
    
    /**
     * 
     */
    public Contact[] getActiveContacts(int max);
    
    /**
     * 
     */
    public void addActiveNode(SocketAddress address);
    
    /**
     * 
     */
    public void addPassiveNode(SocketAddress address);
    
    /**
     * 
     */
    public void handleCollision(CollisionException ex);
    
    /**
     * 
     */
    public void handleContactsMessage(DHTContactsMessage msg);
}
