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

package org.limewire.mojito.routing;

import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.routing.impl.RemoteContact;


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
    public static Contact createLocalContact(Vendor vendor, Version version, boolean firewalled) {
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
    public static Contact createLocalContact(Vendor vendor, Version version, 
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
    public static Contact createLiveContact(SocketAddress sourceAddress, Vendor vendor, 
            Version version, KUID nodeId, SocketAddress contactAddress, int instanceId, int flags) {
        return new RemoteContact(sourceAddress, vendor, version, 
                nodeId, contactAddress, instanceId, flags, State.ALIVE);
    }
    
    /**
     * Creates and returns an unknown Contact
     * 
     * @param vendor The Vendor ID of the Node
     * @param version The Version
     * @param nodeId The NodeID of the Contact
     * @param contactAddress The address where to send requests and responses
     */
    public static Contact createUnknownContact(Vendor vendor, Version version, 
            KUID nodeId, SocketAddress contactAddress) {
        return new RemoteContact(null, vendor, version, 
                nodeId, contactAddress, 0, Contact.DEFAULT_FLAG, State.UNKNOWN);
    }
}
