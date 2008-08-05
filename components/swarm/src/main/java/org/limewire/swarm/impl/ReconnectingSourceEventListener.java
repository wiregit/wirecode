package org.limewire.swarm.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;

public class ReconnectingSourceEventListener implements SwarmSourceEventListener {

    private Map<SwarmSource, SwarmStatus> connectionStatus = Collections
            .synchronizedMap(new HashMap<SwarmSource, SwarmStatus>());

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        // bad uri, or server does not like us
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);
        // if (!swarmSourceHandler.isComplete()) {
        if (status == null || status.isOk() && !status.isFinished()) {
            System.out.println("reconnecting: " + source);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            swarmSourceHandler.addSource(source);
        } else {
            System.out.println("error, not reconnecting: " + source + " status: " + status);
            // }
        }
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        connectionStatus.put(source, status);
    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        connectionStatus.put(source, new FinishedSwarmStatus());
    }

}
