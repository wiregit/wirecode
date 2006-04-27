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

import java.util.Collection;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public class StoreResponse extends ResponseMessage {
    
    public static final int SUCCEEDED = 0x00;
    public static final int FAILED = 0x01;
    
    private int requesting;
    private Collection storeStatus;

    public StoreResponse(int vendor, int version, ContactNode node, 
            KUID messageId, int requesting, Collection storeStatus) {
        super(vendor, version, node, messageId);
        
        this.requesting = requesting;
        this.storeStatus = storeStatus;
    }
    
    public int getRequestCount() {
        return requesting;
    }
    
    public Collection getStoreStatus() {
        return storeStatus;
    }
    
    public static class StoreStatus {
        
        private KUID key;
        private int status;
        
        public StoreStatus(KUID key, int status) {
            this.key = key;
            this.status = status;
        }
        
        public KUID getKey() {
            return key;
        }
        
        public int getStatus() {
            return status;
        }
    }
}
