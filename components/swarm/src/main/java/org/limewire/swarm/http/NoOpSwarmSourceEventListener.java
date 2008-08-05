package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmStatus;
import org.limewire.swarm.Swarmer;

public class NoOpSwarmSourceEventListener implements SwarmSourceEventListener {

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

}
