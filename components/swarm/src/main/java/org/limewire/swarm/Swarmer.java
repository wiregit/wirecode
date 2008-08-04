package org.limewire.swarm;

import org.limewire.swarm.http.SourceEventListener;
import org.limewire.swarm.http.SwarmSource;




public interface Swarmer {
    
    void addSource(SwarmSource source);
    
    void addSource(SwarmSource source, SourceEventListener sourceEventListener);
    
    void start();
    
    void shutdown();
  
    public boolean finished();

}
