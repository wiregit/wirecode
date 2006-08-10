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
}
