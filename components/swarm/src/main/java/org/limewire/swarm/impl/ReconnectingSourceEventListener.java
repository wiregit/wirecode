package org.limewire.swarm.impl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;

public class ReconnectingSourceEventListener implements SwarmSourceEventListener {

    private final Map<SwarmSource, SwarmStatus> connectionStatus;

    public ReconnectingSourceEventListener() {
        connectionStatus = Collections.synchronizedMap(new WeakHashMap<SwarmSource, SwarmStatus>());
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        // connected!
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        // bad uri, or server does not like us
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);

        if (status != null && status.isFinished() || swarmSourceHandler.isComplete()) {
            System.out.println("finished, not reconnecting: " + source + " status: " + status);
        } else if (status != null && status.isError()) {
            System.out.println("error, not reconnecting: " + source + " status: " + status);
        } else if (status == null || status.isOk()) {
            System.out.println("reconnecting: " + source + " status: " + status);
            try {
                Thread.sleep(500);// wait a little before connecting
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // re-add this source back to the swarmer
            swarmSourceHandler.addSource(source);
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
