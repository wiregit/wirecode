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
 
package com.limegroup.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.DHTException;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;

/**
 * The PingResponseHandler handles ping responses from Nodes
 * that we have pinged.
 */
public class PingResponseHandler extends AbstractResponseHandler<Contact> {
    
    private static final Log LOG = LogFactory.getLog(PingResponseHandler.class);
    
    public PingResponseHandler(Context context) {
        super(context);
    }

    protected void response(ResponseMessage message, long time) throws IOException {
        
        PingResponse response = (PingResponse)message;
        SocketAddress externalAddress = response.getExternalAddress();
        
        Contact node = response.getContact();
        if (node.getSocketAddress().equals(externalAddress)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is trying to set our external address to its address!");
            }
            return;
        }
        
        context.setExternalSocketAddress(externalAddress);
        context.addEstimatedRemoteSize(response.getEstimatedSize());
        
        setReturnValue(message.getContact());
    }

    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        setException(new DHTException(nodeId, dst, message, -1L, e));
    }
}
