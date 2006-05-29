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
 * An abstract implementation of DHTMessage
 */
public abstract class AbstractDHTMessage implements DHTMessage {
    
    private int vendor;
    private int version;

    private ContactNode contactNode;
    private KUID messageId;
    
    private byte[] signature;
    
    public AbstractDHTMessage(int vendor, int version, ContactNode source, KUID messageId) {
        this(vendor, version, source, messageId, null);
    }
    
    public AbstractDHTMessage(int vendor, int version, 
            ContactNode source, KUID messageId, byte[] signature) {
        
        if (source == null) {
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
        this.contactNode = source;
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
