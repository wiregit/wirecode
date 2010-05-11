package com.limegroup.gnutella.dht;

import java.io.IOException;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.AbstractController;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

public class DHTControllerStub extends AbstractController {
    
    private final MojitoDHT dht;
    
    public DHTControllerStub(MojitoDHT dht, DHTMode mode) {
        super(mode);
        this.dht = dht;
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        return null;
    }

    @Override
    public MojitoDHT getMojitoDHT() {
        return dht;
    }

    @Override
    public boolean isReady() {
        return dht.isReady();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        return null;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }

    @Override
    public void close() throws IOException {
    }
}
