package de.kapsi.net.kademlia.event;

import java.net.SocketAddress;
import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface StatsListener {

    /**
     * Called after a Stats request succeeded
     * 
     * @param nodeId NodeID of the host that replied
     * @param statistics The node statistics
     * @param address Address of the host that replied
     * @param time Time in milliseconds
     */
    public void nodeStatsResponse(KUID nodeId, SocketAddress address,
            String statistics, long time);
    

    /**
     * Called on a stats request failure (timeout)
     * 
     * @param nodeId Might be null if ID was unknown
     * @param address Address of the host 
     */
    public void nodeStatsTimeout(KUID nodeId, SocketAddress address);
}
