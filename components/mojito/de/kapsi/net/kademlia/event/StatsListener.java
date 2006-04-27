package de.kapsi.net.kademlia.event;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;

public interface StatsListener {

    /**
     * Called after a Stats request succeeded
     * @param node TODO
     * @param statistics The node statistics
     * @param time Time in milliseconds
     */
    public void nodeStatsResponse(ContactNode node, String statistics, long time);
    

    /**
     * Called on a stats request failure (timeout)
     * 
     * @param nodeId Might be null if ID was unknown
     * @param address Address of the host 
     */
    public void nodeStatsTimeout(KUID nodeId, SocketAddress address);
}
