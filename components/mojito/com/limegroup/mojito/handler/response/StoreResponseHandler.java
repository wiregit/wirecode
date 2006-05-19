/*
 * Mojito Distributed Hash Tabe (DHT)
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

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.response.StoreResponse;

/**
 * Currently unused. Could be used to ACK/NACK store
 * requests
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    public StoreResponseHandler(Context context) {
        super(context);
    }
    
    public void response(ResponseMessage message, long time) throws IOException {

        StoreResponse response = (StoreResponse)message;
        
        KUID valueId = response.getValueID();
        int status = response.getStatus();
        
        if (LOG.isTraceEnabled()) {
            if (status == StoreResponse.SUCCEEDED) {
                LOG.trace(message.getSource() + " sucessfully stored KeyValue " + valueId);
            } else if (status == StoreResponse.FAILED) {
                LOG.trace(message.getSource() + " failed to store KeyValue " + valueId);
            } else {
                LOG.trace(message.getSource() + " returned unknown status code " 
                        + status + " for KeyValue " + valueId);
            }
        }
    }
    
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Max number of errors occured. Giving up!");
        }
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a store request to " + ContactNode.toString(nodeId, dst) + " failed", e);
        }
        
        fireTimeout(nodeId, dst, message, -1L);
    }
}
