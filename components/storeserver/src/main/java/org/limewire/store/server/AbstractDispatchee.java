package org.limewire.store.server;

import java.util.ArrayList;
import java.util.List;


/**
 * This is what receives actual commands from an {@link DispatcherSupport}.
 */
abstract class AbstractDispatchee implements Dispatchee {

    private final Dispatcher dispatcher;
    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();
    private boolean isConnected;

    public AbstractDispatchee(final Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public final Dispatcher getDispatcher() {
        return this.dispatcher;
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
