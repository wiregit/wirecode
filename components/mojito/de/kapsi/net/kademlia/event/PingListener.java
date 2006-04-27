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
 
package de.kapsi.net.kademlia.event;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;

/**
 * The PingListener is called by {@see de.kapsi.net.kademlia.handler.response.PingResponseHandler} 
 * on successful pings (i.e. we received a pong) or on failures (which can only be a timeout).
 */
public interface PingListener {
    
    /**
     * Called after a PING succeeded
     * @param node The ContactNode that responded to our Ping
     * @param time Time in milliseconds
     */
    public void pingSuccess(ContactNode node, long time);
    
    /**
     * Called on a PING failure (timeout)
     * 
     * @param nodeId Might be null if ID was unknown
     * @param address Address of the host we tried to ping
     */
    public void pingTimeout(KUID nodeId, SocketAddress address);
}
