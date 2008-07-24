package org.limewire.swarm;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.Range;

/**
 * Wrapper class to help fire various events to the SwarmCoordinatorListeners
 * 
 * 
 */
public class SwarmCoordinatorListenerList {

    /** Coordinator Listeners are attached to. */
    private final SwarmCoordinator swarmCoordinator;

    /** List of listeners. */
    private final CopyOnWriteArrayList<SwarmCoordinatorListener> listeners = new CopyOnWriteArrayList<SwarmCoordinatorListener>();

    /**
     * Constructor for SwarmCoordinatorListenerList
     * 
     * @param swarmCoordinator coordinator listeners are attached to.
     */
    public SwarmCoordinatorListenerList(SwarmCoordinator swarmCoordinator) {
        assert swarmCoordinator != null;
        this.swarmCoordinator = swarmCoordinator;
    }

    /**
     * Fires the downloadCompleted event to the attached listeners.
     * @param swarmFilesystem
     */
    public void downloadCompleted(SwarmFileSystem swarmFilesystem) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.downloadCompleted(swarmCoordinator, swarmFilesystem);
        }
    }

    /**
     * Fires the blockLeased event to the attached Listeners.
     * @param block
     */
    public void blockLeased(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockLeased(swarmCoordinator, block);
        }
    }

    /**
     * Fires the blockUnleased event to the attached Listeners.
     * @param block
     */
    public void blockUnleased(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockUnleased(swarmCoordinator, block);
        }
    }

    /**
     * Fires the blockPending event to the attached Listeners.
     * @param block
     */
    public void blockPending(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockPending(swarmCoordinator, block);
        }
    }

    /**
     * Fires the blockUnpending event to the attached Listeners.
     * @param block
     */
    public void blockUnpending(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockUnpending(swarmCoordinator, block);
        }
    }

    /**
     * Fires the blockWritten event to the attached Listeners.
     * @param block
     */
    public void blockWritten(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockWritten(swarmCoordinator, block);
        }
    }

    /**
     * Fires the blockVerificationFailed event to the attached Listeners.
     * @param block
     */
    public void blockVerificationFailed(Range failedRange) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockVerificationFailed(swarmCoordinator, failedRange);
        }
    }

    /**
     * Fires the blockVerified event to the attached Listeners.
     * @param block
     */
    public void blockVerified(Range block) {
        for (SwarmCoordinatorListener listener : listeners) {
            listener.blockVerified(swarmCoordinator, block);
        }
    }
    
    /**
     * Added listener to the list of listeners listening to events on the swarm Coordinator
     * @param swarmListener
     */
    public void add(SwarmCoordinatorListener swarmListener) {
        listeners.add(swarmListener);
    }
}