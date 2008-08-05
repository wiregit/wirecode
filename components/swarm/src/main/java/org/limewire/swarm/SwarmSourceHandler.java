package org.limewire.swarm;

import java.io.IOException;

public interface SwarmSourceHandler {

    void addSource(SwarmSource source);
    
    void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener);

    void start() throws IOException;

    void shutdown() throws IOException;
    
    boolean isActive(); 
    
    boolean isComplete();
}
