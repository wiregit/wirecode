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
import java.util.List;
import java.util.Map.Entry;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;

/**
 * 
 */
public class FindValueEvent {
    
    public KUID lookupId;
    
    private List<Entry<Contact, Collection<KeyValue>>> values;
    
    @SuppressWarnings("unchecked")
    public FindValueEvent(KUID lookupId, List<? extends Entry<Contact,Collection<KeyValue>>> values) {
        this.lookupId = lookupId;
        this.values = (List<Entry<Contact,Collection<KeyValue>>>)values;
    }
    
    public KUID getLookupID() {
        return lookupId;
    }
    
    public List<Entry<Contact,Collection<KeyValue>>> getValues() {
        return values;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(lookupId).append("\n");
        
        int i = 0;
        for (Entry<Contact, Collection<KeyValue>> entry : values) {
            buffer.append(entry.getKey()).append("\n");
            for (KeyValue keyValue : entry.getValue()) {
                buffer.append("  ").append(i++).append(": ").append(keyValue).append("\n");
            }
        }
        return buffer.toString();
    }
}
