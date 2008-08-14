/**
 * 
 */
package org.limewire.swarm.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;
import org.limewire.swarm.SwarmFileSystem;

/**
 * Class used to track what is happening inside of the swarm coordinator.
 * 
 */
public final class LoggingSwarmCoordinatorListener implements SwarmCoordinatorListener {
    private static final Log LOG = LogFactory.getLog(LoggingSwarmCoordinatorListener.class);

    public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block leased: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block verification failed: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block verified: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block written: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block unleased: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void downloadCompleted(SwarmCoordinator swarmCoordinator, SwarmFileSystem swarmDownload) {
        LOG.debug("download complete");
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block pending: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }

    public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
        LOG.debug("block unpending: " + block.toString() + "/" + block.getLength());
        LOG.trace(swarmCoordinator.toString());
    }
}