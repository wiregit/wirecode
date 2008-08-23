package org.limewire.swarm;

import java.io.IOException;

/**
 * Handles downloading from the given swarm source.
 * 
 */
public interface SwarmSourceHandler {

    /**
     * Adds a source for download.
     */
    void addSource(SwarmSource source);

    /**
     * Starts this source handler.
     */
    void start() throws IOException;

    /**
     * Shuts down this source handler.
     */
    void shutdown() throws IOException;

    /**
     * Returns whether has been started and not shutdown yet.
     */
    boolean isActive();

    /**
     * Returns whether all necessary data has been downloaded.
     */
    boolean isComplete();

    /**
     * Returns the measured bandwidth for its downloads.
     * 
     * @param downstream if true return downstream bandwidth, otherwise upstream
     */
    float getMeasuredBandwidth(boolean downstream);

}
