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

package com.limegroup.mojito.result;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.Contact;

/**
 * The FindNodeResult is fired when a FIND_NODE lookup
 * finishes
 */
public class FindNodeResult {
    
    private final KUID lookupId;
    
    private final Map<Contact, QueryKey> nodes;
    
    private final Collection<Contact> collisions;
    
    private final long time;
    
    private final int hop;
    
    private final int failures;
    
    @SuppressWarnings("unchecked")
    public FindNodeResult(KUID lookupId, 
            Map<? extends Contact, ? extends QueryKey> nodes, 
            Collection<? extends Contact> collisions,
            long time, int hop, int failures) {
    	
        this.lookupId = lookupId;
        this.nodes = (Map<Contact, QueryKey>)nodes;
        this.collisions = (Collection<Contact>)collisions;
        this.time = time;
        this.hop = hop;
        this.failures = failures;
    }
    
    /**
     * Returns the KUID we were looking for
     */
    public KUID getLookupID() {
        return lookupId;
    }
    
    /**
     * Returns the number of failed hosts during this lookup
     */
    public int getFailureCount() {
        return failures;
    }
    
    /**
     * Returns a Map of Contacts and their QueryKeys
     */
    public Map<Contact, QueryKey> getNodes() {
        return nodes;
    }
    
    /**
     * 
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    /**
     * Returns the amount of time it took to find the
     * k-closest Nodes
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Returns the hop at which the lookup terminated
     */
    public int getHop() {
        return hop;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(lookupId).append(" (time=").append(time).append("ms, hop=").append(hop).append(")\n");
        int i = 0;
        for (Entry<Contact, QueryKey> entry : nodes.entrySet()) {
            buffer.append(i++).append(": ").append(entry.getKey())
                .append(", qk=").append(entry.getValue()).append("\n");
        }
        
        if (!collisions.isEmpty()) {
            buffer.append("Collisions:\n");
            i = 0;
            for (Contact node : collisions) {
                buffer.append(i++).append(": ").append(node).append("\n");
            }
        }
        
        return buffer.toString();
    }
}