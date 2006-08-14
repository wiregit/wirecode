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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;

/**
 * 
 */
public final class ContactUtils {

    private static final Log LOG = LogFactory.getLog(ContactUtils.class);
    
    private ContactUtils() {}
    
    /**
     * 
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
     * 
     */
    public static boolean isLocalContact(Contact node) {
        try {
            return NetworkUtils.isLocalHostAddress(node.getContactAddress());
        } catch (IOException e) {
            LOG.error("IOException", e);
        }
        return false;
    }
    
    /**
     * 
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
        } catch (IOException e) {
            LOG.error("IOException", e);
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
        // if ConnectionSettings.LOCAL_IS_PRIVATE is true!
        if (NetworkUtils.isPrivateAddress(node.getContactAddress())) {
            if (LOG.isErrorEnabled()) {
                LOG.error(src + " sent us a Contact with a private IP:Port " + node);
            }
            return false;
        }
        
        return true;
    }
    
    
    /**
     * 
     */
    public static boolean isLocalNode(Context context, Contact node, 
            CollisionVerifyer verifyer) throws IOException {
        
        if (context.isLocalNodeID(node.getNodeID())) {
            // If same address then just skip it
            if (context.isLocalContactAddress(node.getContactAddress())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Skipping local node");
                }
            } else { // there might be a NodeID collision
                if (LOG.isWarnEnabled()) {
                    LOG.warn(node + " seems to collide with " + context.getLocalNode());
                }
                
                // Continue with the lookup but run in parallel a
                // collision check.
                if (verifyer != null) {
                    verifyer.doCollisionCheck(node);
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
    
    public static interface CollisionVerifyer {
        public void doCollisionCheck(Contact node) throws IOException;
    }
}
