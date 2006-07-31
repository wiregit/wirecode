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

import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;

public class StoreEvent {
    
    private DHTValue value;
    
    private List<Contact> nodes;
    
    @SuppressWarnings("unchecked")
    public StoreEvent(DHTValue value, List<? extends Contact> nodes) {
        this.value = value;
        this.nodes = (List<Contact>)nodes;
    }
    
    public KUID getValueID() {
        return value.getValueID();
    }
    
    public DHTValue getValue() {
        return value;
    }
    
    public List<Contact> getNodes() {
        return nodes;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(value).append("\n");
        
        int i = 0;
        for (Contact node : nodes) {
            buffer.append("  ").append(i++).append(": ").append(node).append("\n");
        }
        return buffer.toString();
    }
}
