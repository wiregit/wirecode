package org.limewire.swarm;

import org.limewire.collection.Range;


public interface SwarmListener {

    void downloadCompleted(SwarmCoordinator fileCoordinator, SwarmFileSystem swarmDownload);

    void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range failedRange);

    void blockVerified(SwarmCoordinator swarmCoordinator, Range block);

    void blockWritten(SwarmCoordinator swarmCoordinator, Range block);

    void blockLeased(SwarmCoordinator swarmCoordinator, Range block);
    
    

}
