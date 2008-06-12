package org.limewire.swarm.http;


public interface Swarmer {
    
    void addSource(SwarmSource source, SourceEventListener sourceEventListener);
    
    void start();
    

}
