package com.limegroup.gnutella.dht2;

import java.io.IOException;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.DHTValueFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;

class LeafController extends AbstractController {

    public LeafController() {
        super(DHTMode.PASSIVE_LEAF);
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
    public void close() throws IOException {
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

    @Override
    public void handleContactsMessage(DHTContactsMessage msg) {
        for (Contact contact : msg.getContacts()) {
            // Add contacts to RT
        }
    }
}
