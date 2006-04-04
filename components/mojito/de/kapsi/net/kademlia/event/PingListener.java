/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;

/**
 * The PingListener is called by {@see de.kapsi.net.kademlia.handler.response.PingResponseHandler} 
 * on successful pings (i.e. we received a pong) or on failures (which can only be a timeout).
 */
public interface PingListener {
    
    /**
     * Called after a PING succeeded
     * 
     * @param nodeId NodeID of the host that replied
     * @param address Address of the host that replied
     * @param time Time in milliseconds
     */
    public void pingSuccess(KUID nodeId, SocketAddress address, long time);
    
    /**
     * Called on a PING failure (timeout)
     * 
     * @param nodeId Might be null if ID was unknown
     * @param address Address of the host we tried to ping
     */
    public void pingTimeout(KUID nodeId, SocketAddress address);
}
