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
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;

/**
 * 
 */
public class FindNodeEvent {
    
    private KUID lookupId;
    
    private List<Entry<Contact, QueryKey>> nodes;
    
    private long time;
    
    private int hop;
    
    @SuppressWarnings("unchecked")
    public FindNodeEvent(KUID lookupId, List<? extends Entry<Contact, QueryKey>> nodes, long time, int hop) {
        this.lookupId = lookupId;
        this.nodes = (List<Entry<Contact, QueryKey>>)nodes;
        this.time = time;
        this.hop = hop;
    }
    
    public KUID getLooupID() {
        return lookupId;
    }
    
    public List<Entry<Contact, QueryKey>> getNodes() {
        return nodes;
    }
    
    public long getTime() {
        return time;
    }
    
    public int getHop() {
        return hop;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(lookupId).append(" (time=").append(time).append("ms, hop=").append(hop).append(")\n");
        int i = 0;
        for (Entry<Contact, QueryKey> entry : nodes) {
            buffer.append(i++).append(": ").append(entry.getKey())
                .append(", qk=").append(entry.getValue()).append("\n");
        }
        return buffer.toString();
    }
}
