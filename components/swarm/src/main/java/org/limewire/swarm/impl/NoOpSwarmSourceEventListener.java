package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;

public class NoOpSwarmSourceEventListener implements SwarmSourceListener {

    public NoOpSwarmSourceEventListener() {
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {

    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
    }

}
