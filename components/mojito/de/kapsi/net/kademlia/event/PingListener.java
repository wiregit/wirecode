/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.event;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;

public interface PingListener {
    public void pingResponse(KUID nodeId, SocketAddress address, long time);
}
