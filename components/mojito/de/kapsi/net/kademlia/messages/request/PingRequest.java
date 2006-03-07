/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages.request;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.RequestMessage;

public class PingRequest extends RequestMessage {
    
    public PingRequest(int vendor, int version, KUID nodeId, KUID messageId/*, long time*/) {
        super(vendor, version, nodeId, messageId);
    }
}
