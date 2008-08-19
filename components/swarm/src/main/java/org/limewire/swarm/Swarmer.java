package org.limewire.swarm;

/**
 * The swarm is responsible for registering various SwarmSourceHandlers and
 * providing an interface to download a file from multiple sources in a swarm.
 */
public interface Swarmer {
    /**
     * Adds the given source to the swarm.
     * 
     * @throws NullPointerException if there is no register swarmHandler for the
     *         given SwarmSource.
     */
    void addSource(SwarmSource source);

    /**
     * Registers a swarmSourceHandler to the given swarmSourceType
     */
    void register(SwarmSourceType type, SwarmSourceHandler sourceHandler);

    /**
     * Starts the swarmer and its handlers.
     */
    void start();

    /**
     * Shuts down the swarmer and its handlers.
     */
    void shutdown();

    /**
     * Returns the aggregate measures bandwidth from its handler.
     * 
     * @param downstream if true return downstream bandwidth, otherwise upstream
     */
    float getMeasuredBandwidth(boolean downstream);

    /**
     * Returns true if there is a handler for the given SwarmSourceType.
     * 
     * @param type
     */
    boolean hasHandler(SwarmSourceType type);

}
