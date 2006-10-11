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
 
package com.limegroup.mojito.routing;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.List;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.concurrent.DHTFuture;
import com.limegroup.mojito.event.PingEvent;

/**
 * RouteTable interface that all LimeDHT route table implementations
 * must implement.
 */
public interface RouteTable extends Serializable {
    
    /**
     * Adds a new Contact or if it's already in the RouteTable updates 
     * its contact information.
     * 
     * @param node the Contact we would like to add
     */
    public void add(Contact node);
    
    /**
     * Returns a Contact from the local RoutingTable if such Contact exists 
     * and null if it doesn't.
     */
    public Contact get(KUID nodeId);
    
    /**
     * Selects the best matching Contact for the provided KUID.
     * This method will gueanteed return a non-null value if the
     * RoutingTable is not empty.
     */
    public Contact select(KUID nodeId);
    
    /**
     * Selects the best matching k Contacts for the provided KUID. The returned
     * Contacts are sorted by their closeness to the lookup Key from closest to
     * least closest Contact. Use {@link com.limegroup.mojito.util.BucketUtils#sort(List)}
     * to sort the list from least-recently-seen to most-recently-seen Contact.
     * 
     * @param nodeId the lookup KUID
     * @param count the number of Contact (maybe less if RoutingTable has less than 'count' entries!)
     * @param aliveContacts whether or not only alive Contacts should be in the result set
     * @return list of Contacts sorted by closeness
     */
    public List<Contact> select(KUID nodeId, int count, boolean aliveContacts);
    
    /**
     * Notifies the RoutingTable that the Contact with the provided
     * KUID has failed to answert to a request.
     */
    public void handleFailure(KUID nodeId, SocketAddress address);
    
    /**
     * Returns all Contacts as List
     */
    public List<Contact> getContacts();
    
    /**
     * Returns Contacts that are actively used for routing
     */
    public List<Contact> getActiveContacts();
    
    /**
     * Returns cached Contacts that are in the replacement cache
     */
    public List<Contact> getCachedContacts();
    
    /**
     * Returns a List of KUIDs that need to be looked up in order
     * to refresh (or bootstrap) the RouteTable.
     * 
     * @param bootstrapping Whether or not this refresh is done during bootstrap
     */
    public List<KUID> getRefreshIDs(boolean bootstrapping);
    
    /**
     * Returns the Bucket ID for the given Node ID
     */
    public KUID getBucketID(KUID nodeId);
    
    /**
     * Clears all elements from the RoutingTable
     */
    public void clear();
    
    /**
     * Clears all unknown and dead nodes from the routing table
     */
    public void purge();
    
    /**
     * Rebuilds the RouteTable. Meant to be called after a local
     * Node ID change.
     */
    public void rebuild();
    
    /**
     * Returns the number of live and cached Contacts in the Route Table
     */
    public int size();
    
    /**
     * Returns whether or not the given Contact is the local
     * Node
     */
    public boolean isLocalNode(Contact node);
    
    /**
     * Returns the local Node
     */
    public Contact getLocalNode();
    
    /**
     * Sets the RouteTable PingCallback
     */
    public void setPingCallback(PingCallback callback);
    
    /**
     * An interface utilized by the RouteTable to access 
     * external resources
     */
    public static interface PingCallback {
        
        /** Sends a PING to the given Node */
        public DHTFuture<PingEvent> ping(Contact node);
    }
}
