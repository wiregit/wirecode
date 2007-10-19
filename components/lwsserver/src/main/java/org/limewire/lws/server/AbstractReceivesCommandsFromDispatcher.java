package org.limewire.lws.server;

import java.util.ArrayList;
import java.util.List;


/**
 * This is what receives actual commands from an {@link LWSDispatcherSupport}.
 * This class abstract out the
 * {@link #addConnectionListener(ConnectionListener adding},
 * {@link #removeConnectionListener(ConnectionListener removing}, and
 * {@link #setConnected(boolean notifying} of {@link ConnectionListener}s and
 * leaves subclasses responsible for implementing the
 * {@link ReceivesCommandsFromDispatcher#receiveCommand(String, java.util.Map command handling).
 */
public abstract class AbstractReceivesCommandsFromDispatcher implements ReceivesCommandsFromDispatcher {

    private final List<ConnectionListener> connectionListeners = new ArrayList<ConnectionListener>();
    private boolean isConnected;

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
