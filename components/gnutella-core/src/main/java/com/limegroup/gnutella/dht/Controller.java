package com.limegroup.gnutella.dht;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

import org.limewire.mojito.DHT;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * A {@link Controller} controls a specific type of {@link MojitoDHT}
 * that is running in a specific {@link DHTMode} such as ACTIVE or PASSIVE.
 */
public interface Controller extends Closeable, ConnectionLifecycleListener {

    /**
     * Returns the {@link DHTMode} of the {@link Controller}.
     */
    public DHTMode getMode();
    
    /**
     * Returns {@code true} if the {@link Controller} is running 
     * in the given {@link DHTMode}.
     */
    public boolean isMode(DHTMode other);
    
    /**
     * Returns the {@link Controller}'s underlying {@link MojitoDHT}
     * instance of {@code null} if it doesn't have one.
     */
    public MojitoDHT getMojitoDHT();
    
    /**
     * Returns {@code true} if the {@link Controller} is running.
     */
    public boolean isRunning();
    
    /**
     * Returns {@code true} if the {@link Controller} is ready.
     */
    public boolean isReady();
    
    /**
     * Starts the {@link Controller}.
     */
    public void start() throws IOException;
    
    /**
     * A callback to notify the {@link Controller} that machine's 
     * IP-Address has changed.
     */
    public void addressChanged();
    
    /**
     * Stores the given Key-Value pair in the {@link DHT}.
     */
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value);
    
    /**
     * Retrieves a value from the {@link DHT}.
     */
    public DHTFuture<ValueEntity> get(EntityKey key);
    
    /**
     * Retrieves all values from the {@link DHT}.
     */
    public DHTFuture<ValueEntity[]> getAll(EntityKey key);
    
    /**
     * Returns the max number of {@link Contact}'s from the {@link DHT}'s
     * {@link RouteTable}
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
