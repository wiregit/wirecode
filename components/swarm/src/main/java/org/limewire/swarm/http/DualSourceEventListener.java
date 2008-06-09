package org.limewire.swarm.http;

public class DualSourceEventListener implements SourceEventListener {
    
    private final SourceEventListener a;
    private final SourceEventListener b;
    
    public DualSourceEventListener(SourceEventListener a, SourceEventListener b) {
        this.a = a;
        this.b = b;
    }

    public void connected(Swarmer swarmer, Source source) {
        a.connected(swarmer, source);
        b.connected(swarmer, source);
    }
    
    public void connectFailed(Swarmer swarmer, Source source) {
        a.connectFailed(swarmer, source);
        b.connectFailed(swarmer, source);
    }
    
    public void connectionClosed(Swarmer swarmer, Source source) {
        a.connectionClosed(swarmer, source);
        b.connectionClosed(swarmer, source);
    }
    
    public void responseProcessed(Swarmer swarmer, Source source, int statusCode) {
        a.responseProcessed(swarmer, source, statusCode);
        b.responseProcessed(swarmer, source, statusCode);
    }
    

}
