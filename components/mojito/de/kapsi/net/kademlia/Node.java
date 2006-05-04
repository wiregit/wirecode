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
 
package de.kapsi.net.kademlia;

import java.io.Serializable;

public abstract class Node implements Serializable {
    
    private KUID nodeId;
    
    private transient long timeStamp = 0L;
    
    public Node(KUID nodeId) {
        if (nodeId == null) {
            throw new NullPointerException("NodeID is null");
        }
        
        if (!nodeId.isNodeID()) {
            throw new IllegalArgumentException("ID must be of type NodeID");
        }

        this.nodeId = nodeId;
    }
    
    public void alive() {
        timeStamp = System.currentTimeMillis();
    }
    
    public long getTimeStamp() {
        return timeStamp;
    }
    
    public void setTimeStamp(long timestamp) {
        this.timeStamp = timestamp;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public int hashCode() {
        return nodeId.hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Node)) {
            return false;
        }
        
        return nodeId.equals(((Node)o).nodeId);
    }
    
    public String toString() {
        return nodeId.toString();
    }
}
