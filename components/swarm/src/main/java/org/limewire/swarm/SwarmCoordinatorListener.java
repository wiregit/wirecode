package org.limewire.swarm;

import org.limewire.collection.Range;

/**
 * Listener for events in the SwarmCoordinator.
 * 
 * @author pvertenten
 */
public interface SwarmCoordinatorListener {

    /**
     * When the download is completed the SwarmCoordinator will fire
     * downloadCompleted to all the attached listeners.
     * 
     * @param fileCoordinator
     * @param fileSystem
     */
    void downloadCompleted(SwarmCoordinator fileCoordinator, SwarmFileSystem fileSystem);

    /**
     * When a block fails verification the blockVerificationFailed event will
     * fire to the attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block passes verification the blockVerified event will fire to the
     * attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockVerified(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is written to disk the blockWritten event will fire to the
     * attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockWritten(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is leased the blockLeased event will fire to the attached
     * listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockLeased(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is unLeased the blockUnleased event will fire to the
     * attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockUnleased(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is unpended the blockUnpending event will fire to the
     * attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockUnpending(SwarmCoordinator swarmCoordinator, Range block);

    /**
     * When a block is pending write the blockPending event will fire to the
     * attached listeners.
     * 
     * @param swarmCoordinator
     * @param block
     */
    void blockPending(SwarmCoordinator swarmCoordinator, Range block);
}
