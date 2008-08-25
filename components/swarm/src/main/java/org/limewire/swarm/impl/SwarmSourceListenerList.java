package org.limewire.swarm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmStatus;

/**
 * Acts as a delegate for dispatching events received in the SwarmSource to all added listeners.
 */
public class SwarmSourceListenerList implements SwarmSourceListener {
    private final List<SwarmSourceListener> listeners;

    public SwarmSourceListenerList() {
        this.listeners = Collections.synchronizedList(new ArrayList<SwarmSourceListener>());
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectFailed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connectFailed(swarmSourceHandler, source);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connected(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connected(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connected(swarmSourceHandler, source);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectionClosed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connectionClosed(swarmSourceHandler, source);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#finished(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void finished(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.finished(swarmSourceHandler, source);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#responseProcessed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource, org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        for (SwarmSourceListener listener : listeners) {
            listener.responseProcessed(swarmSourceHandler, source, status);
        }
    }

    /**
     * Adds a listener to this list for event updates.
     */
    public void addListener(SwarmSourceListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from the event updates of this list.
     */
    public void removeListener(SwarmSourceListener listener) {
        listeners.remove(listener);
    }

}
