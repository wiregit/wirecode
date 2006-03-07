/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.response;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class PingResponse extends ResponseMessage {
    
    private final SocketAddress address;
    
    public PingResponse(int vendor, int version, KUID nodeId, 
            KUID messageId, SocketAddress address/*, long time*/) {
        super(vendor, version, nodeId, messageId);
        this.address = address;
    }
    
    /** My external address */
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public String toString() {
        return "Pong: " + address;
    }
}
