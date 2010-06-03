package com.limegroup.gnutella.dht;

import java.net.SocketAddress;

import org.limewire.mojito.ValueKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTValueFuture;
import org.limewire.mojito.entity.CollisionException;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.Value;

import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

/**
 * An implementation of {@link Controller} that is inactive 
 * (i.e. it does nothing).
 */
@Singleton
public class InactiveController implements Controller {

    public static final InactiveController CONTROLLER = new InactiveController();
    
    private InactiveController() {
    }
    
    @Override
    public void addActiveNode(SocketAddress address) {
    }

    @Override
    public void addPassiveNode(SocketAddress address) {
    }

    @Override
    public void addressChanged() {
    }

    @Override
    public Contact[] getActiveContacts(int max) {
        return new Contact[0];
    }

    @Override
    public DHTMode getMode() {
        return DHTMode.INACTIVE;
    }

    @Override
    public void handleCollision(CollisionException ex) {
    }

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
    }

    @Override
    public boolean isMode(DHTMode other) {
        return getMode() == other;
    }
    
    @Override
    public MojitoDHT getMojitoDHT() {
        return null;
    }
    
    @Override
    public boolean isRunning() {
        return false;
    }
    
    @Override
    public boolean isReady() {
        return false;
    }
    
    @Override
    public boolean isBooting() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void close() {
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    @Override
    public DHTFuture<ValueEntity> get(ValueKey key) {
        return new DHTValueFuture<ValueEntity>(new UnsupportedOperationException());
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, Value value) {
        return new DHTValueFuture<StoreEntity>(new UnsupportedOperationException());
    }
    
    @Override
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value) {
        return new DHTValueFuture<StoreEntity>(new UnsupportedOperationException());
    }
    
    @Override
    public DHTFuture<ValueEntity[]> getAll(ValueKey key) {
        return new DHTValueFuture<ValueEntity[]>(new UnsupportedOperationException());
    }
}
