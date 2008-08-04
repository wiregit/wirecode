package org.limewire.swarm.http;

import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;

public class SwarmHttpSessionRequestCallback implements SessionRequestCallback {
    private final Swarmer swarmer;
    private final SwarmSource source;
    private final SourceEventListener listener;
    
    public SwarmHttpSessionRequestCallback(Swarmer swarmer, SwarmSource source, SourceEventListener listener) {
        this.swarmer = swarmer;
        this.source = source;
        this.listener = listener;
    }

    public void cancelled(SessionRequest request) {
        listener.connectFailed(swarmer, source);
    };

    public void completed(SessionRequest request) {
        listener.connected(swarmer, source);
    };

    public void failed(SessionRequest request) {
        listener.connectFailed(swarmer, source);
    };

    public void timeout(SessionRequest request) {
        listener.connectFailed(swarmer, source);
    };

}
