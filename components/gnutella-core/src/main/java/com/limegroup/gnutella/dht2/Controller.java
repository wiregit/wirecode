package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.io.IOException;

import org.limewire.mojito2.EntityKey;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.storage.DHTValue;

import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

public abstract class Controller implements Closeable, ConnectionLifecycleListener {

    private final DHTMode mode;
    
    public Controller(DHTMode mode) {
        this.mode = mode;
    }
    
    public DHTMode getMode() {
        return mode;
    }
    
    public boolean isMode(DHTMode other) {
        return mode == other;
    }
    
    public abstract MojitoDHT getMojitoDHT();
    
    public abstract boolean isRunning();
    
    public abstract boolean isReady();
    
    public abstract void start() throws IOException;
    
    public void addressChanged() {
        
    }
    
    public abstract DHTFuture<StoreEntity> put(KUID key, DHTValue value);
    
    public abstract DHTFuture<ValueEntity> get(EntityKey key);
}
