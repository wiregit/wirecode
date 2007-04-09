/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.Map;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.StoreResponse.StoreStatusCode;
import org.limewire.mojito.routing.Contact;


/**
 * The StoreResult is fired when a STORE request has finished
 */
public class StoreResult implements Result {
    
    private final Map<Contact, Collection<StoreStatusCode>> nodes;
    
    private final Collection<? extends DHTValueEntity> values;

    public StoreResult(Map<Contact, Collection<StoreStatusCode>> nodes, 
            Collection<? extends DHTValueEntity> values) {
        
        this.nodes = nodes;
        this.values = values;
    }
    
    /**
     * Returns a Collection Nodes where the DHTValue(s) were
     * stored
     */
    public Collection<? extends Contact> getNodes() {
        return nodes.keySet();
    }
    
    /**
     * Returns a Collection of DHTValue(s) that were stored
     */
    public Collection<? extends DHTValueEntity> getValues() {
        return values;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("VALUES").append("\n");
        int i = 0;
        for (DHTValueEntity value : getValues()) {
            buffer.append("  ").append(i++).append(": ").append(value).append("\n");
        }
        
        buffer.append("NODES:").append("\n");
        i = 0;
        for (Contact node : getNodes()) {
            buffer.append("  ").append(i++).append(": ").append(node).append("\n");
        }
        return buffer.toString();
    }
}
