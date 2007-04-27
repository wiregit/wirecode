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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.NetworkSettings;

/**
 * The ContactsScrubber is a pre-processing tool to ensure that
 * all Contacts that are returned with FIND_NODE responses are
 * correct and valid.
 */
public class ContactsScrubber {
    
    private static final Log LOG = LogFactory.getLog(ContactsScrubber.class);
    
    private final Collection<? extends Contact> nodes;
    
    private final Map<KUID, Contact> scrubbed;
    
    private final Collection<Contact> collisions;
    
    private final boolean isValidResponse;
    
    /*public static ContactsScrubber scrub(Context context, Contact sender, 
            Collection<? extends Contact> nodes) {
        return scrub(context, sender, nodes, 0f);
    }*/
    
    /**
     * Creates and returns a ContactsScrubber for the given arguments
     */
    public static ContactsScrubber scrub(Context context, Contact sender, 
            Collection<? extends Contact> nodes, float requiredRatio) {
        
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        return new ContactsScrubber(context, sender, nodes, requiredRatio);
    }
    
    private ContactsScrubber(Context context, Contact sender, 
            Collection<? extends Contact> nodes, float requiredRatio) {
        
        assert (!nodes.isEmpty());
        assert (requiredRatio >= 0f && requiredRatio <= 1f);
        
        this.nodes = nodes;
        this.scrubbed = new LinkedHashMap<KUID, Contact>(nodes.size());
        this.collisions = new LinkedHashSet<Contact>(1);
        
        SameClassFilter filter = new SameClassFilter(sender);
        
        boolean containsLocal = false;
        for (Contact node : nodes) {
            // Make sure the SocketAddress is OK
            if (!ContactUtils.isValidSocketAddress(node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(sender + " sent us a Contact with an invalid IP:Port " + node);
                }
                continue;
            }
            
            // Make sure the Contact has not a private IP:Port
            // if it's not permitted
            if (ContactUtils.isPrivateAddress(node)) {
                if (LOG.isInfoEnabled()) {
                    if (ContactUtils.isSameNodeID(sender, node)) {
                        LOG.info(sender + " does not know its external address");
                    } else {
                        LOG.info(sender + " sent a Contact with a private IP:Port: " + node);
                    }
                }
                continue;
            }
            
            // Make sure the IPs are from different Networks. Don't apply
            // this filter if the sender is in the response Set though!
            if (!ContactUtils.isSameNodeID(sender, node)
                    && NetworkSettings.FILTER_CLASS_C.getValue() 
                    && filter.isSameNetwork(node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(sender + " sent one or more Contacts from the same Network-Class: " + node);
                }
                continue;
            }
            
            // Make sure we're not mixing IPv4 and IPv6 addresses.
            // See RouteTableImpl.add() for more Info!
            if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(node + " is from a different IP address space than local Node");
                }
                continue;
            }
            
            // Check if the Node collides with the local Node
            if (ContactUtils.isCollision(context, node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(node + " seems to collide with " + context.getLocalNode());
                }
                
                collisions.add(node);
                continue;
            }
            
            // Check if it's the local Node
            if (ContactUtils.isLocalContact(context, node)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node");
                }
                containsLocal = true;
                continue;
            }
            
            // All tests passed! Add the Contact to our Set
            // of filtered Contacts!
            scrubbed.put(node.getNodeID(), node);
        }
        
        if (requiredRatio > 0f) {
            int total = scrubbed.size() + collisions.size();
            if (containsLocal) {
                total++;
            }
            
            float ratio = (float)total / nodes.size();
            this.isValidResponse = (ratio >= requiredRatio);
        } else {
            this.isValidResponse = true;
        }
    }
    
    /**
     * Returns all Contacts
     */
    public Collection<? extends Contact> getContacts() {
        return nodes;
    }
    
    /**
     * Returns a Collection of scrubbed Contacts. Scrubbed Contacts
     * are Contacts that passed our filters and are OK to be added
     * to the RouteTable.
     */
    public Collection<Contact> getScrubbed() {
        return scrubbed.values();
    }
    
    /**
     * Returns a Collection of Contacts that seem to collide with
     * the local Node.
     */
    public Collection<Contact> getCollisions() {
        return collisions;
    }
    
    /**
     * Returns true if the response contains any or rather the 
     * response itself can be considered as valid
     */
    public boolean isValidResponse() {
        return isValidResponse;
    }
}
