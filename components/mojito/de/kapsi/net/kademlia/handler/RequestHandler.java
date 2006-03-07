/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.Message;

public interface RequestHandler {
    public void handleRequest(KUID nodeId, SocketAddress src, Message message) throws IOException;
}
