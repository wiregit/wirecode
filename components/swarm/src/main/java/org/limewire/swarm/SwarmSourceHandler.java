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
     * Returns whether or not this handler is active.
     */
    boolean isActive();

    /**
     * Returns whether or not this handler is complete.
     */
    boolean isComplete();

    /**
     * Returns the aggregate measures bandwidth from its handler.
     * 
     * @param downstream if true return downstream bandwidth, otherwise upstream
     */
    float getMeasuredBandwidth(boolean downstream);

}
