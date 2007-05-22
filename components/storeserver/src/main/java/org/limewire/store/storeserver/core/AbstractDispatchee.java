package org.limewire.store.storeserver.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is what receives actual commands from the local server.
 * 
 * @author jpalm
 */
public abstract class AbstractDispatchee implements Dispatchee {

    private final LocalServer server;
    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();

    private boolean isConnected;

    public AbstractDispatchee(final LocalServer server) {
        this.server = server;
    }

    public final LocalServer getServer() {
        return this.server;
    }

    public final void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
        connectionChanged(this.isConnected);
        if (!connectionListeners.isEmpty()) {
            for (ConnectionListener lis : connectionListeners) {
                lis.connectionChanged(isConnected);
            }
        }
    }
    
    public final boolean addConnectionListener(ConnectionListener lis) {
        return connectionListeners.contains(lis) ? false : connectionListeners.add(lis);
    }

    public final boolean removeConnectionListener(ConnectionListener lis) {
        return !connectionListeners.contains(lis) ? false : connectionListeners.remove(lis);    
    }
    

    /**
     * Override this to receive notification when the connection has changed.
     * 
     * @param isConnected <tt>true</tt> if we're connected, <tt>false</tt>
     *        otherwise
     */
    protected abstract void connectionChanged(boolean isConnected);

}
