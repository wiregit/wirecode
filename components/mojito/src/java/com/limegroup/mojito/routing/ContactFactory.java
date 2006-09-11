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

import java.net.SocketAddress;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.Contact.State;
import com.limegroup.mojito.routing.impl.ContactNode;
import com.limegroup.mojito.routing.impl.LocalContact;

/**
 * A Factory class to create Contacts
 */
public class ContactFactory {
    
    private ContactFactory() {}
    
    /**
     * Creates and returns a local Contact
     * 
     * @param vendor Our vendor ID
     * @param version The version
     * @param firewalled whether or not we're firewalled
     */
    public static Contact createLocalContact(int vendor, int version, boolean firewalled) {
        return createLocalContact(vendor, version, KUID.createRandomID(), 0, firewalled);
    }
    
    /**
     * Creates and returns a local Contact
     * 
     * @param vendor Our vendor ID
     * @param version The version
     * @param nodeId Our Node ID
     * @param firewalled whether or not we're firewalled
     */
    public static Contact createLocalContact(int vendor, int version, 
            KUID nodeId, int instanceId, boolean firewalled) {
        return new LocalContact(vendor, version, nodeId, instanceId, firewalled);
    }
    
    /**
     * Creates and returns a live Contact. A live Contact is a Node
     * that send us a Message
     * 
     * @param sourceAddress The source address
     * @param vendor The Vendor ID of the Node
     * @param version The Version
     * @param nodeId The NodeID of the Contact
     * @param contactAddress The address where to send requests and responses
     * @param instanceId The instanceId of the Node
     * @param whether or not the Node is firewalled
     */
    public static Contact createLiveContact(SocketAddress sourceAddress, int vendor, 
            int version, KUID nodeId, SocketAddress contactAddress, int instanceId, boolean firewalled) {
        return new ContactNode(sourceAddress, vendor, version, 
                nodeId, contactAddress, instanceId, firewalled, State.ALIVE);
    }
    
    /**
     * Creates and returns an unknown Contact
     * 
     * @param vendor The Vendor ID of the Node
     * @param version The Version
     * @param nodeId The NodeID of the Contact
     * @param contactAddress The address where to send requests and responses
     */
    public static Contact createUnknownContact(int vendor, int version, 
            KUID nodeId, SocketAddress contactAddress) {
        return new ContactNode(null, vendor, version, 
                nodeId, contactAddress, 0, false, State.UNKNOWN);
    }
}
