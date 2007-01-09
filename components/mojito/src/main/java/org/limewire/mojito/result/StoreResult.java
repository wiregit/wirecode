/*
 * Mojito Distributed Hash Table (Mojito DHT)
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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.routing.Contact;


/**
 * The StoreResult is fired when a STORE request has finished
 */
public class StoreResult implements Result {
    
    private final Collection<? extends Contact> nodes;
    
    private final Collection<? extends DHTValueEntity> values;

    private final Collection<? extends DHTValueEntity> failed;
    
    public StoreResult(Collection<? extends Contact> nodes, 
            Collection<? extends DHTValueEntity> values, 
            Collection<? extends DHTValueEntity> failed) {
        
        this.nodes = nodes;
        this.values = values;
        this.failed = failed;
    }
    
    /**
     * Returns a Collection Nodes where the DHTValue(s) were
     * stored
     */
    public Collection<? extends Contact> getNodes() {
        return nodes;
    }
    
    /**
     * Returns a Collection of DHTValue(s) that were stored
     */
    public Collection<? extends DHTValueEntity> getValues() {
        return values;
    }
    
    /**
     * Returns a Collection of DHTValue(s) that couldn't
     * be stored on the DHT
     */
    public Collection<? extends DHTValueEntity> getFailed() {
        return failed;
    }
    
    /**
     * Returns a Collection of DHTValue(s) that were successfully 
     * stored on the DHT
     */
    public Collection<DHTValueEntity> getSucceeded() {
        Set<DHTValueEntity> succeeded = new HashSet<DHTValueEntity>(getValues());
        succeeded.removeAll(getFailed());
        return succeeded;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("SUCEEDED").append("\n");
        int i = 0;
        for (DHTValueEntity value : getSucceeded()) {
            buffer.append("  ").append(i++).append(": ").append(value).append("\n");
        }
        
        Collection<? extends DHTValueEntity> failed = getFailed();
        if (!failed.isEmpty()) {
            buffer.append("FAILED:").append("\n");
            i = 0;
            for (DHTValueEntity value : failed) {
                buffer.append("  ").append(i++).append(": ").append(value).append("\n");
            }
        }
        
        buffer.append("NODES:").append("\n");
        i = 0;
        for (Contact node : nodes) {
            buffer.append("  ").append(i++).append(": ").append(node).append("\n");
        }
        return buffer.toString();
    }
}
