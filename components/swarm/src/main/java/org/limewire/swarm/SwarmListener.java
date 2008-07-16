package org.limewire.swarm;


public interface SwarmListener {

    void fileCompleted(SwarmCoordinator fileCoordinator, SwarmDownload swarmDownload);

}
