package org.limewire.swarm.http;

import org.limewire.swarm.SourceEventListener;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;
import org.limewire.swarm.Swarmer;

public class NoOpSwarmSourceEventListener implements SwarmSourceEventListener, SourceEventListener {

    public NoOpSwarmSourceEventListener() {
    }

    public void connected(Swarmer swarmer, SwarmSource source) {

    }

    public void connectFailed(Swarmer swarmer, SwarmSource source) {

    }

    public void connectionClosed(Swarmer swarmer, SwarmSource source) {

    }

    public void responseProcessed(Swarmer swarmer, SwarmSource source, SwarmStatus status) {

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

}
