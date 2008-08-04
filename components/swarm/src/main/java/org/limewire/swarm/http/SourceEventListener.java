package org.limewire.swarm.http;

import org.limewire.swarm.SwarmStatus;

public interface SourceEventListener {

    void connectFailed(Swarmer swarmer, SwarmSource source);

    void connected(Swarmer swarmer, SwarmSource source);

    void connectionClosed(Swarmer swarmer, SwarmSource source);

    void responseProcessed(Swarmer swarmer, SwarmSource source, SwarmStatus status);

}
