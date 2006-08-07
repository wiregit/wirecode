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

package com.limegroup.mojito.event;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.db.DHTValue;

public class StoreEvent {
    
    private Collection<Contact> nodes;
    
    private Collection<DHTValue> values;
    
    private Collection<DHTValue> failed;
    
    @SuppressWarnings("unchecked")
    public StoreEvent(List<? extends Contact> nodes, 
            Collection<? extends DHTValue> values, 
            Collection<? extends DHTValue> failed) {
        
        this.nodes = (Collection<Contact>)nodes;
        this.values = (Collection<DHTValue>)values;
        this.failed = (Collection<DHTValue>)failed;
    }
    
    public Collection<Contact> getNodes() {
        return nodes;
    }
    
    public Collection<DHTValue> getValues() {
        return values;
    }
    
    public Collection<DHTValue> getFailed() {
        return failed;
    }
    
    public Collection<DHTValue> getSucceeded() {
        Set<DHTValue> succeeded = new HashSet<DHTValue>(getValues());
        succeeded.removeAll(getFailed());
        return succeeded;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("SUCEEDED").append("\n");
        int i = 0;
        for (DHTValue value : getSucceeded()) {
            buffer.append("  ").append(i++).append(": ").append(value).append("\n");
        }
        
        buffer.append("FAILED:").append("\n");
        i = 0;
        for (DHTValue value : getFailed()) {
            buffer.append("  ").append(i++).append(": ").append(value).append("\n");
        }
        
        buffer.append("NODES:").append("\n");
        i = 0;
        for (Contact node : nodes) {
            buffer.append("  ").append(i++).append(": ").append(node).append("\n");
        }
        return buffer.toString();
    }
}
