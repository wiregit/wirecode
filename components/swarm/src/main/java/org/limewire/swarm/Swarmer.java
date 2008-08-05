package org.limewire.swarm;

public interface Swarmer {
    
    void addSource(SwarmSource source);
    
    void addSource(SwarmSource source, SourceEventListener sourceEventListener);
    
    void start();
    
    void shutdown();
  
    public boolean isActive();

    void register(Class clazz, SwarmSourceHandler sourceHandler);

}
