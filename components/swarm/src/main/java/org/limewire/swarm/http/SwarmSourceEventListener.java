package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.Swarmer;

public interface SwarmSourceEventListener {

    void connectFailed(Swarmer swarmer, SwarmSource source);

    void connected(Swarmer swarmer, SwarmSource source);

    void connectionClosed(Swarmer swarmer, SwarmSource source);

    void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode);

}
