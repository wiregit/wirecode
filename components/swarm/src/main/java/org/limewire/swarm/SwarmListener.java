package org.limewire.swarm;

import org.limewire.collection.Range;


public interface SwarmListener {

    void downloadCompleted(SwarmCoordinator fileCoordinator, SwarmFileSystem swarmDownload);

    void verificationFailed(SwarmCoordinator swarmCoordinator, Range failedRange);

}
