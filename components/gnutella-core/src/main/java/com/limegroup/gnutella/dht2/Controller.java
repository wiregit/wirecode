package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.io.IOException;

import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

abstract class Controller implements Closeable, ConnectionLifecycleListener {

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
    
    public abstract boolean isRunning();
    
    public abstract void start() throws IOException;
}
