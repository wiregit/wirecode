/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.messages;

import de.kapsi.net.kademlia.KUID;

public abstract class Message {
    
    public static final int PING_REQUEST = 0x01;
    public static final int PING_RESPONSE = 0x02;
    
    public static final int STORE_REQUEST = 0x03;
    public static final int STORE_RESPONSE = 0x04;
    
    public static final int FIND_NODE_REQUEST = 0x05;
    public static final int FIND_NODE_RESPONSE = 0x06;
    
    public static final int FIND_VALUE_REQUEST = 0x08;
    public static final int FIND_VALUE_RESPONSE = 0x09;
    
    protected final int vendor;
    protected final int version;
    
    protected final KUID nodeId;
    protected final KUID messageId;
    
    public Message(int vendor, int version, 
            KUID nodeId, KUID messageId) {
        
        if (nodeId == null) {
            throw new NullPointerException("NodeID is null");
        }
        
        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        if (!nodeId.isNodeID()) {
            throw new IllegalArgumentException("NodeID is of wrong type: " + nodeId);
        }
        
        if (!messageId.isMessageID()) {
            throw new IllegalArgumentException("MessageID is of wrong type: " + messageId);
        }
        
        this.vendor = vendor;
        this.version = version;
        this.nodeId = nodeId;
        this.messageId = messageId;
    }
    
    public int getVendor() {
        return vendor;
    }
    
    public int getVersion() {
        return version;
    }
    
    public KUID getMessageID() {
        return messageId;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
}
