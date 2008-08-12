package org.limewire.swarm;

import java.net.SocketAddress;

import org.limewire.collection.IntervalSet;
import org.limewire.swarm.impl.SwarmSourceListenerList;

public interface SwarmSource {

    SocketAddress getAddress();

    String getPath();

    IntervalSet getAvailableRanges();

    SwarmSourceType getType();
    
    public boolean isFinished();

    void connectFailed(SwarmSourceHandler swarmSourceHandler);

    void connected(SwarmSourceHandler swarmSourceHandler);

    void connectionClosed(SwarmSourceHandler swarmSourceHandler);

    void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmStatus status);

    void finished(SwarmSourceHandler swarmSourceHandler);
    
    void addListener(SwarmSourceListener listener);

}
