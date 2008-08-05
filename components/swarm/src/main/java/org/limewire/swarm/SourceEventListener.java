package org.limewire.swarm;


public interface SourceEventListener {

    void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source);

    void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source, SwarmStatus status);

}
