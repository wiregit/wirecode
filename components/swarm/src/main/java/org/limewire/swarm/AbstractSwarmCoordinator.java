package org.limewire.swarm;

public abstract class AbstractSwarmCoordinator implements SwarmCoordinator {
    
    /** List of listeners. */
    private final SwarmCoordinatorListenerList listeners = new SwarmCoordinatorListenerList(this);

    public void addListener(SwarmCoordinatorListener swarmListener) {
        listeners.add(swarmListener);
    }

    protected SwarmCoordinatorListenerList listeners() {
        return listeners;
    }
}
