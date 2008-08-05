package org.limewire.swarm;

import org.limewire.swarm.http.SwarmSourceEventListener;

public interface Swarmer {
    
    void addSource(SwarmSource source);
    
    void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener);
    
    void start();
    
    void shutdown();
  
    public boolean isActive();

    void register(Class clazz, SwarmSourceHandler sourceHandler);

}
