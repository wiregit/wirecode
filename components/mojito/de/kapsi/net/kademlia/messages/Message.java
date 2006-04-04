/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
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
