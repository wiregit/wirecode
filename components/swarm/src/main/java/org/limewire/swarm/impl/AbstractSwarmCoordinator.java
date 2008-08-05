package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;


/**
 * Abstract Swarm Coordinator that will hold some common functionality accross Coordinators.
 * 
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
