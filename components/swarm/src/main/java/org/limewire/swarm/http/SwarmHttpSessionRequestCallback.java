package org.limewire.swarm.http;

import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.limewire.swarm.SourceEventListener;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;

public class SwarmHttpSessionRequestCallback implements SessionRequestCallback {
    private final SwarmSourceHandler swarmSourceHandler;

    private final SwarmSource source;

    private final SourceEventListener listener;

    public SwarmHttpSessionRequestCallback(SwarmSourceHandler swarmSourceHandler,
            SwarmSource source, SourceEventListener listener) {
        this.swarmSourceHandler = swarmSourceHandler;
        this.source = source;
        this.listener = listener;
    }

    public void cancelled(SessionRequest request) {
        listener.connectFailed(swarmSourceHandler, source);
    };

    public void completed(SessionRequest request) {
        listener.connected(swarmSourceHandler, source);
    };

    public void failed(SessionRequest request) {
        listener.connectFailed(swarmSourceHandler, source);
    };

    public void timeout(SessionRequest request) {
        listener.connectFailed(swarmSourceHandler, source);
    };

}
