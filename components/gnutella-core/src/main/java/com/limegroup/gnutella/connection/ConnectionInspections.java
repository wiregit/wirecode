package com.limegroup.gnutella.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.util.Clock;

import com.google.inject.Inject;
import com.limegroup.gnutella.ConnectionManager;



/**
 * Measures how long it takes to get a single ultrapeer connection.
 */
@EagerSingleton
public class ConnectionInspections implements ConnectionLifecycleListener {
    
    private final Clock clock;

    private final AtomicLong startedConnecting = new AtomicLong(0);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private ConnectionManager connectionManager;
    
    @SuppressWarnings("unused")
    @InspectablePrimitive("time to connect")
    private long connectionTime;
    
    @SuppressWarnings("unused")
    @InspectablePrimitive("time to load")
    private long loadTime;

    @Inject
    public ConnectionInspections(Clock clock) {
        this.clock = clock;
    }

    @Inject
    public void register(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.connectionManager.addEventListener(this);
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        switch (evt.getType()) {
            case CONNECTING:
                startedConnecting.compareAndSet(0, clock.now());
                break;
            case CONNECTION_INITIALIZED:  
                // TODO use CONNECTED event instead?
                if(!connected.getAndSet(true)) {
                    connectionTime = clock.now() - startedConnecting.get();
                    connectionManager.removeEventListener(this);
                }
                break;
        }
    }
    
    public void setLoadTime(long loadTime) {
        this.loadTime = loadTime;
    }
}
