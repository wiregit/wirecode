package org.limewire.swarm;

/**
 * Abstract Swarm Coordinator that will hold some common functionality accross Coordinators.
 * 
 * @author pvertenten
 */
public abstract class AbstractSwarmCoordinator implements SwarmCoordinator {
    
    /** List of listeners. */
    private final SwarmCoordinatorListenerList listeners = new SwarmCoordinatorListenerList(this);

    
    /**
     * Adds a Listener to this Coordinator.
     */
    public void addListener(SwarmCoordinatorListener swarmListener) {
        listeners.add(swarmListener);
    }

    /**
     * Returns the listeners for this coordinator.
     * @return
     */
    protected SwarmCoordinatorListenerList listeners() {
        return listeners;
    }
}
