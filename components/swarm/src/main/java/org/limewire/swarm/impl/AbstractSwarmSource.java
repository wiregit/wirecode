package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public abstract class AbstractSwarmSource implements SwarmSource {

    private final SwarmSourceListenerList listenerList;

    public AbstractSwarmSource() {
        listenerList = new SwarmSourceListenerList();
    }

    public void connected(SwarmSourceHandler swarmSourceHandler) {
        listenerList.connected(swarmSourceHandler, this);
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler) {
        listenerList.connectFailed(swarmSourceHandler, this);
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler) {
        listenerList.connectionClosed(swarmSourceHandler, this);
    }

    public void finished(SwarmSourceHandler swarmSourceHandler) {
        listenerList.finished(swarmSourceHandler, this);
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmStatus status) {
        listenerList.responseProcessed(swarmSourceHandler, this, status);
    }

    public void addListener(SwarmSourceListener listener) {
        listenerList.addListener(listener);
    }

}
