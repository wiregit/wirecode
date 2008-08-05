package org.limewire.swarm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReconnectingSourceEventListener implements SourceEventListener {

    private Map<SwarmSource, SwarmStatus> connectionStatus = Collections
            .synchronizedMap(new HashMap<SwarmSource, SwarmStatus>());

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        // bad uri, or server does not like us
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);
        if (!swarmSourceHandler.isComplete()) {
            if (status == null || status.isOk()) {
                System.out.println("reconnecting: " + source);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                swarmSourceHandler.addSource(source);
            } else {
                System.out.println("error, not reconnecting: " + source + " status: " + status);
            }
        }
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        connectionStatus.put(source, status);
    }

}
