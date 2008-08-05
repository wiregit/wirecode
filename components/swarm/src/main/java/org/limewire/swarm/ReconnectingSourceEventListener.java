package org.limewire.swarm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.swarm.http.SwarmSourceEventListener;

public class ReconnectingSourceEventListener implements SourceEventListener,
        SwarmSourceEventListener {

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
        if (status == null || status.isOk()) {
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

    public void connectFailed(Swarmer swarmer, SwarmSource source) {
        // TODO Auto-generated method stub

    }

    public void connected(Swarmer swarmer, SwarmSource source) {
        // TODO Auto-generated method stub

    }

    public void connectionClosed(Swarmer swarmer, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);
        // if (!swarmer.isComplete()) {
        if (status == null || status.isOk()) {
            System.out.println("reconnecting: " + source);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
            swarmer.addSource(source);
        } else {
            System.out.println("error, not reconnecting: " + source + " status: " + status);
        }
        // }

    }

    public void responseProcessed(Swarmer swarmer, SwarmSource source, SwarmStatus status) {
        connectionStatus.put(source, status);

    }

}
