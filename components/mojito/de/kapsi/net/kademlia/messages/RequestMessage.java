/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages;

import de.kapsi.net.kademlia.KUID;

public abstract class RequestMessage extends Message {

    public RequestMessage(int vendor, int version, 
            KUID nodeId, KUID messageId) {
        super(vendor, version, nodeId, messageId);
    }
}
