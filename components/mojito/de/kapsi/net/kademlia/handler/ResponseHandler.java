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
 
package de.kapsi.net.kademlia.handler;

import java.io.IOException;
import java.net.SocketAddress;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.messages.Message;

public interface ResponseHandler {
    
    public void addTime(long time);
    
    public long time();
    
    public long timeout();
    
    public void handleResponse(KUID nodeId, SocketAddress src, Message message, long time) throws IOException;
    
    public void handleTimeout(KUID nodeId, SocketAddress dst, Message message, long time) throws IOException;
}
