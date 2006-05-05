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
 
package de.kapsi.net.kademlia.messages.response;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class PingResponse extends ResponseMessage {
    
    private SocketAddress externalAddress;
    private int estimatedSize;
    
    public PingResponse(int vendor, int version, ContactNode node, 
            KUID messageId, SocketAddress address, int estimatedSize) {
        super(vendor, version, node, messageId);
        
        this.externalAddress = address;
        this.estimatedSize = estimatedSize;
    }
    
    public PingResponse(int vendor, int version, ContactNode node, 
            KUID messageId, SocketAddress externalAddress, int estimatedSize, byte[] signature) {
        super(vendor, version, node, messageId, signature);
        
        this.externalAddress = externalAddress;
        this.estimatedSize = estimatedSize;
    }
    
    /** My external address */
    public SocketAddress getExternalSocketAddress() {
        return externalAddress;
    }
    
    public int getEstimatedSize() {
        return estimatedSize;
    }
    
    public String toString() {
        return "Pong: " + externalAddress;
    }
}
