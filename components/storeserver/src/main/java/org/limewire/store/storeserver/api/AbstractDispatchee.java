package org.limewire.store.storeserver.api;

import java.util.ArrayList;
import java.util.List;

/**
 * This is what receives actual commands from an {@link IServer}.
 * 
 * @author jpalm
 */
public abstract class AbstractDispatchee implements IDispatchee {

    private final org.limewire.store.storeserver.api.IServer server;

    private final List<IConnectionListener> connectionListeners = new ArrayList<IConnectionListener>();

    private boolean isConnected;

    public AbstractDispatchee(final IServer server) {
        this.server = server;
    }

    public final IServer getServer() {
        return this.server;
    }

    public final void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
        connectionChanged(this.isConnected);
        if (!connectionListeners.isEmpty()) {
            for (IConnectionListener lis : connectionListeners) {
                lis.connectionChanged(isConnected);
            }
        }
    }

    public final boolean addConnectionListener(IConnectionListener lis) {
        return connectionListeners.contains(lis) ? false : connectionListeners
                .add(lis);
    }

    public final boolean removeConnectionListener(IConnectionListener lis) {
        return !connectionListeners.contains(lis) ? false : connectionListeners
                .remove(lis);
    }

    /**
     * Override this to receive notification when the connection has changed.
     * 
     * @param isConnected <tt>true</tt> if we're connected, <tt>false</tt>
     *        otherwise
     */
    protected abstract void connectionChanged(boolean isConnected);

}
