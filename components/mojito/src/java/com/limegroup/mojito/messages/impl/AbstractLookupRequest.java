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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageOutputStream;
import com.limegroup.mojito.messages.LookupRequest;

public abstract class AbstractLookupRequest extends AbstractRequestMessage
        implements LookupRequest {

    protected final KUID lookupId;
    
    public AbstractLookupRequest(int opcode, int vendor, int version, 
            ContactNode node, KUID messageId, KUID lookupId) {
        	super(opcode, vendor, version, node, messageId);
        	
        	this.lookupId = lookupId;
    }
    
    public AbstractLookupRequest(int opcode, SocketAddress src, ByteBuffer data) throws IOException {
        super(opcode, src, data);
        
        switch(opcode) {
            case FIND_NODE_REQUEST:
                lookupId = KUID.createNodeID(data);
                break;
            case FIND_VALUE_REQUEST:
                lookupId = KUID.createValueID(data);
                break;
            default:
                throw new IOException("Unknown opcode for lookup request: " + opcode);
        }
    }

    public KUID getLookupID() {
        return lookupId;
    }

    protected void writeBody(MessageOutputStream out) throws IOException {
        out.writeKUID(lookupId);
    }
}
