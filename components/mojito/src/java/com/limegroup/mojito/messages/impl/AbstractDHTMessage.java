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

package com.limegroup.mojito.messages.impl;

import java.net.SocketAddress;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * An abstract implementation of DHTMessage
 */
public abstract class AbstractDHTMessage implements DHTMessage {

    private int opcode;
    
    private int vendor;
    private int version;

    private ContactNode contactNode;
    private KUID messageId;
    
    public AbstractDHTMessage(int opcode, int vendor, int version,
            ContactNode contactNode, KUID messageId) {

        switch(opcode) {
            case PING_REQUEST:
            case PING_RESPONSE:
            case STORE_REQUEST:
            case STORE_RESPONSE:
            case FIND_NODE_REQUEST:
            case FIND_NODE_RESPONSE:
            case FIND_VALUE_REQUEST:
            case FIND_VALUE_RESPONSE:
            case STATS_REQUEST:
            case STATS_RESPONSE:
                // OK
                break;
            default:
                throw new IllegalArgumentException("Unknown opcode: " + opcode); 
        }
        
        if (contactNode == null) {
            throw new NullPointerException("ContactNode is null");
        }

        if (messageId == null) {
            throw new NullPointerException("MessageID is null");
        }

        if (!messageId.isMessageID()) {
            throw new IllegalArgumentException("MessageID is of wrong type: " + messageId);
        }

        if (version < 0 || version > 0xFFFF) {
            throw new IllegalArgumentException("Version must be between 0x00 and 0xFFFF: " + version);
        }
        
        this.opcode = opcode;
        this.vendor = vendor;
        this.version = version;
        this.contactNode = contactNode;
        this.messageId = messageId;
    }

    public int getOpCode() {
        return opcode;
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
    
    public KUID getSourceNodeID() {
        return contactNode.getNodeID();
    }
    
    public SocketAddress getSourceAddress() {
        return contactNode.getSocketAddress();
    }
}
