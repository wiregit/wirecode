package org.limewire.swarm;

public abstract class AbstractSwarmCoordinator implements SwarmCoordinator {
    
    /** List of listeners. */
    private final SwarmListenerList listeners = new SwarmListenerList(this);

    public void addListener(SwarmListener swarmListener) {
        listeners.add(swarmListener);
    }

    protected SwarmListenerList listeners() {
        return listeners;
    }
}
