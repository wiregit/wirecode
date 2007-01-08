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
 
package com.limegroup.mojito.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.routing.Contact;
import com.limegroup.mojito.routing.ContactFactory;

/**
 * Miscellaneous untilities for Contacts
 */
public final class ContactUtils {

    private static final Log LOG = LogFactory.getLog(ContactUtils.class);
    
    private ContactUtils() {}
    
    /**
     * Returns the nodeId and address as a formatted String
     */
    public static String toString(KUID nodeId, SocketAddress address) {
        if (nodeId != null) {
            if (address != null) {
                return nodeId + " (" + address + ")";
            } else {
                return nodeId.toString();
            }
        } else if (address != null) {
            return address.toString();
        } else {
            return "null";
        }
    }
    
    /**
     * Returns true if the given Contact's address is any of
     * localhost's addresses
     */
    public static boolean isLocalHostAddress(Contact node) {
        try {
            return NetworkUtils.isLocalHostAddress(node.getContactAddress());
        } catch (SocketException e) {
            LOG.error("SocketException", e);
        }
        return false;
    }
    
    /**
     * Returns true if the given Contacts have both a localhost address
     */
    public static boolean areLocalContacts(Contact existing, Contact node) {
        try {
            // Huh? The addresses are not equal but both belong
            // obviously to this local machine!?
            InetSocketAddress newAddress = (InetSocketAddress) node.getContactAddress();
            InetSocketAddress oldAddress = (InetSocketAddress) existing.getContactAddress();
            if (NetworkUtils.isLocalHostAddress(newAddress)
                    && NetworkUtils.isLocalHostAddress(oldAddress)
                    && newAddress.getPort() == oldAddress.getPort()) {
                return true;
            }
        } catch (SocketException e) {
            LOG.error("SocketException", e);
        }
        
        return false;
    }
    
    /**
     * Checks whether or not 'node' is a valid Contact. Valid 
     * in sense of having a correct IP:Port and depending on
     * the ConnectionSettings.LOCAL_IS_PRIVATE setting whether or
     * not its IP is a non-private Address.
     * 
     * @param src The source that send the 'node'
     * @param node The Contact to verify
     * @return Whether or not 'node' is a valid Contact
     */
    public static boolean isValidContact(Contact src, Contact node) {
        if (!NetworkUtils.isValidSocketAddress(node.getContactAddress())) {
            if (LOG.isErrorEnabled()) {
                LOG.error(src + " sent us a Contact with an invalid IP:Port " + node);
            }
            return false;
        }
        
        // NOTE: NetworkUtils.isPrivateAddress() is checking internally
        // if ConnectionSettings.LOCAL_IS_PRIVATE is true! If you're planning
        // to run the DHT on a Local Area Network (LAN) you want to set
        // LOCAL_IS_PRIVATE to false!
        if (NetworkUtils.isPrivateAddress(node.getContactAddress())) {
            if (src.getNodeID().equals(node.getNodeID())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(src + " does not know its external address");
                }
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error(src + " sent us a Contact with a private IP:Port " + node);
                }
            }
            
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Checks whether or not 'node' is the local Node and
     * triggers Node ID collision verification respectively.
     */
    public static boolean isLocalContact(Context context, Contact node, 
            Collection<Contact> collisions) {
        
        if (context.isLocalNodeID(node.getNodeID())) {
            // If same address then just skip it
            if (context.isLocalContactAddress(node.getContactAddress())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local Node");
                }
            } else { // there might be a NodeID collision
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " seems to collide with " + context.getLocalNode());
                }
                
                if (collisions != null) {
                    collisions.add(node);
                }
            }
            
            return true;
        }
        
        // Imagine you have two Nodes that have each other in
        // their RouteTable. The first Node quits and restarts
        // with a new Node ID. The second Node pings the first
        // Node and we add it to the RouteTable. The first Node
        // starts a lookup and we get a Set of contacts from
        // the second Node which contains our old Contact (different 
        // Node ID but same IPP). So what happens now is that
        // we're sending a lookup to that Node which is the same
        // as sending the lookup to ourself (loopback).
        if (context.isLocalContactAddress(node.getContactAddress())) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(node + " has the same Contact addess as we do " + context.getLocalNode());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns true if both Contacts have an IPv4 or IPv6 address
     */
    public static boolean isSameAddressSpace(Contact a, Contact b) {
        return NetworkUtils.isSameAddressSpace(
                    a.getContactAddress(), 
                    b.getContactAddress());
    }

    /**
     * 
     */
    public static Contact createCollisionPingSender(Contact localNode) {
        // The idea is to invert our local Node ID so that the
        // other Node doesn't get the impression we're trying
        // to spoof anything and we don't want that the other
        // guy adds this Contact to its RouteTable. To do so
        // we're creating a firewalled version of our local Node
        // (with the inverted Node ID of course).
        int vendor = localNode.getVendor();
        int version = localNode.getVersion();
        KUID nodeId = localNode.getNodeID().invert();
        SocketAddress addr = localNode.getContactAddress();
        Contact sender = ContactFactory.createLiveContact(
                addr, vendor, version, nodeId, addr, 0, Contact.FIREWALLED_FLAG);
        
        return sender;
    }

    public static boolean isCollisionPingSender(KUID nodeId, Contact sender) {
        // The sender must be firewalled!
        if (!sender.isFirewalled()) {
            return false;
        }
        
        // See createCollisionPingSender(...)
        KUID expectedSenderId = nodeId.invert();
        return expectedSenderId.equals(sender.getNodeID());
    }
}
