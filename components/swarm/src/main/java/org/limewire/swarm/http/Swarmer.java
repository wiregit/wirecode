package org.limewire.swarm.http;


public interface Swarmer {
    
    void addSource(Source source, SourceEventListener sourceEventListener);
    
    void start();
    

}
