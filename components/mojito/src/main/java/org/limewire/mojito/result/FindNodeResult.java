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
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.QueryKey;


/**
 * The FindNodeResult is fired when a FIND_NODE lookup
 * finishes
 */
public class FindNodeResult implements Result {
    
    private final KUID lookupId;
    
    private final Map<? extends Contact, ? extends QueryKey> nodes;
    
    private final Collection<? extends Contact> collisions;
    
    private final long time;
    
    private final int hop;
    
    private final int failures;
    
    public FindNodeResult(KUID lookupId, 
            Map<? extends Contact, ? extends QueryKey> nodes, 
            Collection<? extends Contact> collisions,
            long time, int hop, int failures) {
    	
        this.lookupId = lookupId;
        this.nodes = nodes;
        this.collisions = collisions;
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
    public Map<? extends Contact, ? extends QueryKey> getNodes() {
        return nodes;
    }
    
    /**
     * 
     */
    public Collection<? extends Contact> getCollisions() {
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
        for (Entry<? extends Contact, ? extends QueryKey> entry : nodes.entrySet()) {
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