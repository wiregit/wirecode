package org.limewire.swarm.http;

public interface SourceEventListener {

    void connectFailed(Swarmer swarmer, Source source);

    void connected(Swarmer swarmer, Source source);

    void connectionClosed(Swarmer swarmer, Source source);

    void responseProcessed(Swarmer swarmer, Source source, int statusCode);

}
