/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.NetworkSettings;

/**
 * The ContactFilter is meant to be used in combination with
 * FIND_NODE responses. It makes sure that the returned list
 * of Contacts is valid.  
 */
public class ContactFilter {
    
    private static final Log LOG = LogFactory.getLog(ContactFilter.class);
    
    private final Context context;
    
    private final Contact sender;
    
    private final SameClassFilter networkFilter;
    
    private final Set<Contact> collisions = new LinkedHashSet<Contact>();
    
    public ContactFilter(Context context, Contact sender) {
        this.context = context;
        this.sender = sender;
        this.networkFilter = new SameClassFilter(sender);
    }
    
    /**
     * Returns a Collection of Contacts that collide with the
     * local Node
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    /**
     * Returns true if the given Contact is valid and OK
     * to be contacted and added to the RouteTable
     */
    public boolean isValidContact(Contact node) {
        
        // Make sure everything is OK with the Contact
        if (!ContactUtils.isValidContact(sender, node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping invalid Contact: " + node);
            }
            return false;
        }
        
        // Make sure the IPs are from different Networks
        if (NetworkSettings.FILTER_CLASS_C.getValue() 
                && networkFilter.isSameNetwork(node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(sender + " sent one or more Contacts from the same Network-Class: " + node);
            }
            return false;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses.
        // See RouteTableImpl.add() for more Info!
        if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info(node + " is from a different IP address space than local Node");
            }
            return false;
        }
        
        // Check if the Node collides with the local Node
        if (ContactUtils.isCollision(context, node)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " seems to collide with " + context.getLocalNode());
            }
            
            collisions.add(node);
            return false;
        }
        
        // Check if it's the local Node
        if (ContactUtils.isLocalContact(context, node)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Skipping local Node");
            }
            return false;
        }
        
        return true;
    }
    
    /*public static <T extends Contact> Collection<T> filter(Context context, Contact sender, Collection<T> nodes) {
        ContactFilter filter = new ContactFilter(context, sender);
        List<T> filtered = new ArrayList<T>(nodes.size());
        for (T node : nodes) {
            if (filter.isValidContact(node)) {
                filtered.add(node);
            }
        }
        return filtered;
    }*/
}
