package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;


public class DualSourceEventListener implements SwarmSourceEventListener {
    
    private final SwarmSourceEventListener a;
    private final SwarmSourceEventListener b;
    
    public DualSourceEventListener(SwarmSourceEventListener a, SwarmSourceEventListener b) {
        this.a = a;
        this.b = b;
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        a.connected(swarmSourceHandler, source);
        b.connected(swarmSourceHandler, source);
    }
    
    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        a.connectFailed(swarmSourceHandler, source);
        b.connectFailed(swarmSourceHandler, source);
    }
    
    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        a.connectionClosed(swarmSourceHandler, source);
        b.connectionClosed(swarmSourceHandler, source);
    }
    
    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source, SwarmStatus status) {
        a.responseProcessed(swarmSourceHandler, source, status);
        b.responseProcessed(swarmSourceHandler, source, status);
    }
    

}
