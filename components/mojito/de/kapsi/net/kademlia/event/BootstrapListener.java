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
 
package de.kapsi.net.kademlia.event;

import java.util.Collection;

import de.kapsi.net.kademlia.KUID;

public interface BootstrapListener {
    
    /**
     * Called after the inital bootstrap phase has finished. In the inital
     * phase we are looking for the K closest Nodes.
     * 
     * @param nodeId The NodeID we are looking for
     * @param nodes Collection of K closest ContactNodes sorted by closeness
     * @param time Time in milliseconds
     */
    public void initialPhaseComplete(KUID nodeId, Collection nodes, long time);
    
    /**
     * Called after the second and final bootstrap phase has finished. In 
     * the second phase we are refreshing the furthest away Buckets. 
     * 
     * @param time Time in milliseconds
     * @param foundNodes wheather or not Nodes were found
     */
    public void secondPhaseComplete(long time, boolean foundNodes);
}
