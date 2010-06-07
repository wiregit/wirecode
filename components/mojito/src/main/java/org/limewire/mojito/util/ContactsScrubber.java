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
import java.util.Set;

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
    
    private ContactsScrubber() {}
    
    public static Scrubbed scrub(Context context, Contact src, 
            Contact[] contacts, float requiredRatio) {
        return scrub(context.getLocalhost(), src, contacts, requiredRatio);
    }
    
    public static Scrubbed scrub(Contact localhost, Contact src, 
            Contact[] contacts, float requiredRatio) {
        
        assert (0 < contacts.length);
        assert (requiredRatio >= 0f && requiredRatio <= 1f);
        
        Map<KUID, Contact> scrubbed 
            = new LinkedHashMap<KUID, Contact>(contacts.length);
        
        Set<Contact> collisions 
            = new LinkedHashSet<Contact>(1);
        
        SameClassFilter filter = new SameClassFilter(src);
        
        boolean containsLocal = false;
        for (Contact contact : contacts) {
            // Make sure the SocketAddress is OK
            if (!ContactUtils.isValidSocketAddress(contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(src + " sent us a Contact with an invalid IP:Port " + contact);
                }
                continue;
            }
            
            // Make sure the Contact has not a private IP:Port
            // if it's not permitted
            if (ContactUtils.isPrivateAddress(contact)) {
                if (LOG.isInfoEnabled()) {
                    if (ContactUtils.isSameNodeID(src, contact)) {
                        LOG.info(src + " does not know its external address");
                    } else {
                        LOG.info(src + " sent a Contact with a private IP:Port: " + contact);
                    }
                }
                continue;
            }
            
            // Make sure we're not mixing IPv4 and IPv6 addresses.
            // See RouteTableImpl.add() for more Info!
            if (!ContactUtils.isSameAddressSpace(localhost, contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(contact + " is from a different IP address space than local Node");
                }
                continue;
            }
            
            // IPv4-compatible addresses are 'tricky'. Two IPv6 aware systems
            // may communicate with each other by using IPv4 infrastructure.
            // This works only if both are dual-stack systems. In an IPv6 DHT
            // we may have the situation that some systems don't understand
            // IPv4 and they can't do anything with these Contacts.
            if (NetworkSettings.DROP_PUBLIC_IPV4_COMPATIBLE_ADDRESSES.getValue()
                    && ContactUtils.isIPv4CompatibleAddress(contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(contact + " has an IPv4-compatible address");
                }
                continue;
            }
            
            // Same as above but somewhat undefined. It's unclear whether or not
            // an address such as ::0000:192.168.0.1 is a site-local addresses
            // or not. On one side it's an IPv6 address and therefore not a
            // site-local address but if you read it as an IPv4 address then
            // it is.
            if (NetworkSettings.DROP_PRIVATE_IPV4_COMPATIBLE_ADDRESSES.getValue()
                    && ContactUtils.isPrivateIPv4CompatibleAddress(contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(contact + " has a private IPv4-compatible address");
                }
                continue;
            }
            
            // Make sure the IPs are from different Networks. Don't apply
            // this filter if the sender is in the response Set though!
            if (NetworkSettings.FILTER_CLASS_C.getValue()
                    && ContactUtils.isIPv4Address(contact)
                    && !ContactUtils.isSameNodeID(src, contact)
                    && filter.isSameNetwork(contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(src + " sent one or more Contacts from the same Network-Class: " + contact);
                }
                continue;
            }
            
            // Check if the Node collides with the local Node
            if (ContactUtils.isCollision(localhost, contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(contact + " seems to collide with " + localhost);
                }
                
                collisions.add(contact);
                continue;
            }
            
            // Check if it's the local Node
            if (ContactUtils.isLocalContact(localhost, contact)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node");
                }
                containsLocal = true;
                continue;
            }
            
            // All tests passed! Add the Contact to our Set
            // of filtered Contacts!
            scrubbed.put(contact.getContactId(), contact);
        }
        
        boolean valid = true;
        if (0f < requiredRatio) {
            int total = scrubbed.size() + collisions.size();
            if (containsLocal) {
                total++;
            }
            
            float ratio = (float)total / contacts.length;
            valid = (ratio >= requiredRatio);
        }
        
        return new Scrubbed(contacts, toArray(scrubbed), toArray(collisions), valid);
    }
    
    private static Contact[] toArray(Map<?, ? extends Contact> m) {
        return toArray(m.values());
    }
    
    private static Contact[] toArray(Collection<? extends Contact> c) {
        return c.toArray(new Contact[0]);
    }
    
    /**
     * 
     */
    public static class Scrubbed {
        
        private final Contact[] contacts;
        
        private final Contact[] scrubbed;
        
        private final Contact[] collisions;
        
        private final boolean valid;
        
        private Scrubbed(Contact[] contacts, 
                Contact[] scrubbed, 
                Contact[] collisions,
                boolean valid) {
            
            this.contacts = contacts;
            this.scrubbed = scrubbed;
            this.collisions = collisions;
            this.valid = valid;
        }

        /**
         * Returns all {@link Contact}s.
         */
        public Contact[] getContacts() {
            return contacts;
        }

        /**
         * Returns scrubbed {@link Contact}s.
         */
        public Contact[] getScrubbed() {
            return scrubbed;
        }

        /**
         * Returns {@link Contact}s that collide with the 
         * localhost {@link Contact}.
         */
        public Contact[] getCollisions() {
            return collisions;
        }

        /**
         * Returns {@code true} if the scrubbed {@link Contact}s are valid.
         */
        public boolean isValid() {
            return valid;
        }
    }
}
