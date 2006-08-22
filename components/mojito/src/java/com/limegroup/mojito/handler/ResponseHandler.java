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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * Interface to handle incoming responses
 */
public interface ResponseHandler {
    
    /**
     * Returns the total time that has elapsed since a
     * request has been sent to Node
     */
    public long time();
    
    /**
     * Returns the timeout (in milliseconds) of this handler.
     */
    public long timeout();
    
    /**
     * Returns whether or not this handler has been cancelled
     */
    public boolean isCancelled();
    
    /**
     * Called to handle a response
     * 
     * @param message The Response Message
     * @param time The Round Trip Time
     * @throws IOException 
     */
    public void handleResponse(ResponseMessage message, long time) throws IOException;
    
    /**
     * Called after the timeout time has elapsed and no response has arrived
     * 
     * @param nodeId The Node ID of the Contact (can be null)
     * @param dst The Address where we sent the request
     * @param message The Request Message
     * @param time The total time that has elapsed
     * @throws IOException
     */
    public void handleTimeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException;
    
    /**
     * Called if an error occured in handleResponse() or handleTimeout()
     * 
     * @param nodeId
     * @param dst
     * @param message
     * @param e
     */
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e);
    
    /**
     * 
     */
    public void handleTick();
}
