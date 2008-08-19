package org.limewire.swarm;

import java.net.SocketAddress;

import org.limewire.collection.IntervalSet;

/**
 * Represents a source to download from.
 */
public interface SwarmSource {

    /**
     * Returns the socket address for the underlying download source.
     */
    SocketAddress getAddress();

    /**
     * Returns the path to the download source.
     */
    String getPath();

    /**
     * Returns the available ranges the source has to download from.
     */
    IntervalSet getAvailableRanges();

    /**
     * Returns the swarmSourceType for this swarmSource.
     */
    SwarmSourceType getType();

    /**
     * Returns true if we are finished with this swarmSource.
     */
    public boolean isFinished();

    /**
     * Adds listener to this swarmsource.
     */
    void addListener(SwarmSourceListener listener);

    /**
     * Removes a listener from this swarmSource.
     */
    void removeListener(SwarmSourceListener listener);

    /**
     * Fires connectFailed event to registerd Listeners.
     */
    void connectFailed(SwarmSourceHandler swarmSourceHandler);

    /**
     * Fires connected event to registerd Listeners.
     */
    void connected(SwarmSourceHandler swarmSourceHandler);

    /**
     * Fires connectionClosed event to registerd Listeners.
     */
    void connectionClosed(SwarmSourceHandler swarmSourceHandler);

    /**
     * Fires responseProcessed event to registerd Listeners.
     */
    void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmStatus status);

    /**
     * Fires finished event to registerd Listeners.
     */
    void finished(SwarmSourceHandler swarmSourceHandler);

}
