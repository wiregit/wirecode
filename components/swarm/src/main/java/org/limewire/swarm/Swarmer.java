package org.limewire.swarm;


public interface Swarmer {
    void addSource(SwarmSource source);

    void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener);

    void register(SwarmSourceType type, SwarmSourceHandler sourceHandler);

    void start();

    void shutdown();

    float getMeasuredBandwidth(boolean downstream);
    
    boolean hasHandler(SwarmSourceType type);
}
