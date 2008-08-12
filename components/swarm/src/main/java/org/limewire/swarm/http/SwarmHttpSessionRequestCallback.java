package org.limewire.swarm.http;

import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;

public class SwarmHttpSessionRequestCallback implements SessionRequestCallback {
    private final SwarmSourceHandler swarmSourceHandler;

    private final SwarmSource source;

    public SwarmHttpSessionRequestCallback(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        this.swarmSourceHandler = swarmSourceHandler;
        this.source = source;
    }

    public void cancelled(SessionRequest request) {
        source.connectFailed(swarmSourceHandler);
    };

    public void completed(SessionRequest request) {
        source.connected(swarmSourceHandler);
    };

    public void failed(SessionRequest request) {
        source.connectFailed(swarmSourceHandler);
    };

    public void timeout(SessionRequest request) {
        source.connectFailed(swarmSourceHandler);
    };

}
