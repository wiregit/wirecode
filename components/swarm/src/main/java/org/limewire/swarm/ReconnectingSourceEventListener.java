package org.limewire.swarm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ReconnectingSourceEventListener implements SourceEventListener {

    private Map<SwarmSource, SwarmStatus> connectionStatus = Collections
            .synchronizedMap(new HashMap<SwarmSource, SwarmStatus>());

    public void connected(Swarmer swarmer, SwarmSource source) {

    }

    public void connectFailed(Swarmer swarmer, SwarmSource source) {
        // bad uri, or server does not like us
    }

    public void connectionClosed(Swarmer swarmer, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);
        if (status.isOk()) {
            swarmer.addSource(source);
        }
    }

    public void responseProcessed(Swarmer swarmer, SwarmSource source, SwarmStatus status) {
        connectionStatus.put(source, status);
    }

}
