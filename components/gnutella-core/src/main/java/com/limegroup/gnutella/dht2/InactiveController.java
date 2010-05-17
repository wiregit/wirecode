package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

public class InactiveController implements Controller {

    public static final Controller CONTROLLER = new InactiveController();
    
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
    public void start() {
    }

    @Override
    public void close() {
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return new DHTValueFuture<ValueEntity>(new UnsupportedOperationException());
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return new DHTValueFuture<StoreEntity>(new UnsupportedOperationException());
    }
}
