package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.Swarmer;

public class DualSourceEventListener implements SourceEventListener {
    
    private final SourceEventListener a;
    private final SourceEventListener b;
    
    public DualSourceEventListener(SourceEventListener a, SourceEventListener b) {
        this.a = a;
        this.b = b;
    }

    public void connected(Swarmer swarmer, SwarmSource source) {
        a.connected(swarmer, source);
        b.connected(swarmer, source);
    }
    
    public void connectFailed(Swarmer swarmer, SwarmSource source) {
        a.connectFailed(swarmer, source);
        b.connectFailed(swarmer, source);
    }
    
    public void connectionClosed(Swarmer swarmer, SwarmSource source) {
        a.connectionClosed(swarmer, source);
        b.connectionClosed(swarmer, source);
    }
    
    public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
        a.responseProcessed(swarmer, source, statusCode);
        b.responseProcessed(swarmer, source, statusCode);
    }
    

}
