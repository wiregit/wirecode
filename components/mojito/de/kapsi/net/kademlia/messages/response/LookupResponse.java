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
import java.util.Collections;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.ResponseMessage;

public abstract class LookupResponse extends ResponseMessage {
    
    protected final Collection values;
    
    public LookupResponse(int vendor, int version, KUID nodeId,
            KUID messageId, Collection responseValues) {
        super(vendor, version, nodeId, messageId);
        
        this.values = Collections.unmodifiableCollection(responseValues);
    }
    
    public Collection getValues() {
        return values;
    }
    
    public String toString() {
        return values.toString();
    }
}
