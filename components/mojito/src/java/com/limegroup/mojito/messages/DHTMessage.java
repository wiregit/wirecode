/*
 * Mojito Distributed Hash Tabe (DHT)
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
 
package com.limegroup.mojito.messages;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;

/**
 * This is an abstract base class for all DHT messages.
 */
public abstract class DHTMessage {
    
    public static final int PING_REQUEST = 0x01;
    public static final int PING_RESPONSE = 0x02;
    
    public static final int STORE_REQUEST = 0x03;
    public static final int STORE_RESPONSE = 0x04;
    
    public static final int FIND_NODE_REQUEST = 0x05;
    public static final int FIND_NODE_RESPONSE = 0x06;
    
    public static final int FIND_VALUE_REQUEST = 0x07;
    public static final int FIND_VALUE_RESPONSE = 0x08;
    
    public static final int STATS_REQUEST = 0x0A;
    public static final int STATS_RESPONSE = 0x0B;
    
    private int vendor;
    private int version;

    private ContactNode contactNode;
    private KUID messageId;
    
    private byte[] signature;
    
    public DHTMessage(int vendor, int version, 
            ContactNode contactNode, KUID messageId) {
        this(vendor, version, contactNode, messageId, null);
    }
    
    public DHTMessage(int vendor, int version, 
            ContactNode contactNode, KUID messageId, byte[] signature) {
        
        if (contactNode == null) {
            throw new NullPointerException("ContactNode is null");
        }
        
        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }
        
        if (!messageId.isMessageID()) {
            throw new IllegalArgumentException("MessageID is of wrong type: " + messageId);
        }
        
        this.vendor = vendor;
        this.version = version;
        this.contactNode = contactNode;
        this.messageId = messageId;
        this.signature = signature;
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
    
    public ContactNode getContactNode() {
        return contactNode;
    }
    
    public byte[] getSignature() {
        return signature;
    }
}
