/**
 * 
 */
package org.limewire.swarm;

import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinatorListener;
import org.limewire.swarm.SwarmFileSystem;

/**
 * Class used to track what is happening inside of the swarm coordinator. It
 * echos out events as they happen.
 * 
 * 
 */
public final class LoggingSwarmCoordinatorListener implements SwarmCoordinatorListener {
    public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block leased: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verification failed: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block verified: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block written: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unleased: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void downloadCompleted(SwarmCoordinator swarmCoordinator, SwarmFileSystem swarmDownload) {
        System.out.println("download complete");
        System.out.println(swarmCoordinator.toString());
    }

    public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block pending: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }

    public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
        System.out.println("block unpending: " + block.toString());
        System.out.println(swarmCoordinator.toString());
    }
}