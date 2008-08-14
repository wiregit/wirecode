package org.limewire.swarm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;

public class SwarmSourceListenerList implements SwarmSourceListener {
    private final List<SwarmSourceListener> listeners;

    public SwarmSourceListenerList() {
        this.listeners = Collections.synchronizedList(new ArrayList<SwarmSourceListener>());
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connectFailed(swarmSourceHandler, source);
        }
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connected(swarmSourceHandler, source);
        }
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.connectionClosed(swarmSourceHandler, source);
        }
    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        for (SwarmSourceListener listener : listeners) {
            listener.finished(swarmSourceHandler, source);
        }
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        for (SwarmSourceListener listener : listeners) {
            listener.responseProcessed(swarmSourceHandler, source, status);
        }
    }

    public void addListener(SwarmSourceListener listener) {
        listeners.add(listener);
    }

}
