package org.limewire.core.impl.connection;

import java.util.Map;
import java.util.WeakHashMap;

import org.limewire.core.api.connection.ConnectionLifeCycleListener;
import org.limewire.core.api.connection.ConnectionManager;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;

@Singleton
public class ConnectionManagerImpl implements ConnectionManager {

    private final com.limegroup.gnutella.ConnectionManager connectionManager;

    private final Map<ConnectionLifeCycleListener, ConnectionLifecycleListener> listeners;

    @Inject
    public ConnectionManagerImpl(com.limegroup.gnutella.ConnectionManager connectionManager) {
        this.connectionManager = Objects.nonNull(connectionManager, "connectionManager");
        this.listeners = new WeakHashMap<ConnectionLifeCycleListener, ConnectionLifecycleListener>();
    }

    /**
     * Wraps provided listener in core interface and manages the listener
     * mapping in the listeners map.
     */
    public void addEventListener(final ConnectionLifeCycleListener listener) {
        ConnectionLifecycleListener connectionLifecycleListener = new ConnectionLifecycleListener() {
            @Override
            public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
                listener.handleEvent(evt.getType());
            }
        };
        listeners.put(listener, connectionLifecycleListener);
        connectionManager.addEventListener(connectionLifecycleListener);
    }

    public boolean isConnected() {
        return connectionManager.isConnected();
    }

    public boolean isFullyConnected() {
        return connectionManager.isFullyConnected();
    }

    public boolean isSuperNode() {
        return connectionManager.isSupernode();
    }
    
    /**
     * Finds a managed wrapped listener in the listeners map and delegates the
     * remove listener call to the core connection manager.
     */
    public void removeEventListener(ConnectionLifeCycleListener listener) {
        ConnectionLifecycleListener connectionLifecycleListener = listeners.get(listener);
        if (connectionLifecycleListener != null) {
            listeners.remove(listener);
            connectionManager.removeEventListener(connectionLifecycleListener);
        }
    }

    @Override
    public void restart() {
       connectionManager.disconnect(true);
       connectionManager.connect();
    }
}
