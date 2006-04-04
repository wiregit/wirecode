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
 
package com.limegroup.gnutella.dht.tests;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.db.KeyValue;

public class DHTLookupStat {

private final KeyValue value;

    private final long latency;
    
    private final int hops;
    
    private final ContactNode node;
    
    private final boolean success;

    public DHTLookupStat(KeyValue value, long latency, int hops, ContactNode node, boolean success) {
        this.hops = hops;
        this.latency = latency;
        this.node = node;
        this.value = value;
        this.success = success;
    }

    public String toString() {
        String delim = DHTNodeStat.FILE_DELIMITER;
        return value + delim + latency + delim + hops + delim + node + delim + success;
    }

    
    
}
