package com.limegroup.gnutella.dht2;

import java.io.IOException;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

class LeafController extends Controller {

    public LeafController() {
        super(DHTMode.PASSIVE_LEAF);
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
}
