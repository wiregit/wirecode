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

package com.limegroup.mojito.exceptions;

import java.net.SocketAddress;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;

@SuppressWarnings("serial")
public class DHTException extends Exception {

    private KUID nodeId;
    
    private SocketAddress address;
    
    private RequestMessage request;
    
    private long time;
    
    public DHTException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time, Throwable cause) {
        super(cause);
        
        this.nodeId = nodeId;
        this.address = address;
        this.request = request;
        this.time = time;
    }
    
    public DHTException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time, String msg, Throwable cause) {
        super(msg, cause);
        
        this.nodeId = nodeId;
        this.address = address;
        this.request = request;
        this.time = time;
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddress() {
        return address;
    }
    
    public RequestMessage getRequestMessage() {
        return request;
    }
    
    public long getTime() {
        return time;
    }
}
