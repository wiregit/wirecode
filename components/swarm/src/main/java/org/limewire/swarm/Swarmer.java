package org.limewire.swarm;

public interface Swarmer {
    void addSource(SwarmSource source);

    void register(SwarmSourceType type, SwarmSourceHandler sourceHandler);

    void start();

    void shutdown();

    float getMeasuredBandwidth(boolean downstream);

    boolean hasHandler(SwarmSourceType type);
}
