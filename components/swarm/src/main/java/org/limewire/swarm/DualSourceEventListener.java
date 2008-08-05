package org.limewire.swarm;


public class DualSourceEventListener implements SourceEventListener {
    
    private final SourceEventListener a;
    private final SourceEventListener b;
    
    public DualSourceEventListener(SourceEventListener a, SourceEventListener b) {
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
