package org.limewire.swarm;

/**
 * Representation of status for a swarm source download.
 */
public interface SwarmStatus {

    /**
     * True is the status is good to continue.
     */
    public boolean isOk();

    /**
     * True if there has been an error.
     */
    public boolean isError();

    /**
     * True if we are finished downloading from this source.
     */
    public boolean isFinished();
}
