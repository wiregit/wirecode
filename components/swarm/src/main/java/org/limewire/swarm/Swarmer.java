package org.limewire.swarm;

public interface Swarmer {
    void addSource(SwarmSource source);

    void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener);

    void register(Class clazz, SwarmSourceHandler sourceHandler);

    void start();

    void shutdown();
}
