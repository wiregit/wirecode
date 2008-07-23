/**
 * 
 */
package org.limewire.swarm;

import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;
import org.limewire.swarm.SwarmFileSystem;

public final class EchoSwarmCoordinatorListener implements SwarmCoordinatorListener {
    public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block leased: " + block.toString());

    }

    public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verification failed: " + block.toString());

    }

    public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verified: " + block.toString());

    }

    public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block written: " + block.toString());
    }

    public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unleased: " + block.toString());

    }

    public void downloadCompleted(SwarmCoordinator fileCoordinator, SwarmFileSystem swarmDownload) {
        System.out.println("download complete");
    }

    public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block pending: " + block.toString());

    }

    public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unpending: " + block.toString());

    }
}