/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.Message;

public interface ResponseHandler {
    
    public long timeout();
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) throws IOException;

    public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) throws IOException;
}
