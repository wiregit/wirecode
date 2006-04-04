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
 
package de.kapsi.net.kademlia.routing;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.event.BootstrapListener;

public interface RoutingTable {
    
    public void clear();
    
    public boolean isEmpty();
    
    public int size();
    
    public boolean add(ContactNode node, boolean knownToBeAlive);
    
    public ContactNode get(KUID nodeId);
    
    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache);
    
    public ContactNode selectNextClosest(KUID key);

    public ContactNode select(KUID key);
    
    public List select(KUID lookup, int k, boolean onlyLiveNodes, boolean isLocalLookup);
    
    public boolean containsNode(KUID nodeId);
    
    public void handleFailure(KUID nodeId);
    
    public Collection getAllNodes();
    
    public Collection getAllBuckets();
    
    public void refreshBuckets(boolean force) throws IOException;
    
    /**
     * Refreshes the routing table's buckets
     * 
     * @param force true to refresh all buckets, false otherwise
     * @param l the BootstrapListener callback
     * @throws IOException
     */
    public void refreshBuckets(boolean force, BootstrapListener l) throws IOException;
    
    public boolean load();
    public boolean store();
}
