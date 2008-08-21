package org.limewire.swarm;

/**
 * Listener for events with a swarm source.
 * 
 */
public interface SwarmSourceListener {

    /**
     * Notification of a connection failure.
     * 
     * @param swarmSourceHandler The handler responsible for connection and
     *        using this source.
     * @param source The source that the notification is generated for.
     */
    void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    /**
     * Notification of a connection passing.
     * 
     * @param swarmSourceHandler The handler responsible for connection and
     *        using this source.
     * @param source The source that the notification is generated for.
     */
    void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    /**
     * Notification of a connection closure.
     * 
     * @param swarmSourceHandler The handler responsible for connection and
     *        using this source.
     * @param source The source that the notification is generated for.
     */
    void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    /**
     * Notification of a response being processed.
     * 
     * @param swarmSourceHandler The handler responsible for connection and
     *        using this source.
     * @param source The source that the notification is generated for.
     */
    void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status);

    /**
     * Notification of finishing.
     * 
     * @param swarmSourceHandler The handler responsible for connection and
     *        using this source.
     * @param source The source that the notification is generated for.
     */
    void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

}
