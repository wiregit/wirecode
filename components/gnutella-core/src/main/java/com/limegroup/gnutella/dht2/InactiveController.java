package com.limegroup.gnutella.dht2;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht2.DHTManager.DHTMode;

public class InactiveController extends Controller {

    public static final Controller CONTROLLER = new InactiveController();
    
    private InactiveController() {
        super (DHTMode.INACTIVE);
    }
    
    @Override
    public boolean isRunning() {
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
}
