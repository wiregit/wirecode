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
 
package de.kapsi.net.kademlia.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.DatabaseSettings;

/**
 * Currently unused. Could be used to ACK/NACK store
 * requests
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private int index = 0;
    private int length = 0;
    
    private QueryKey queryKey;
    private List keyValues;
    
    public StoreResponseHandler(Context context, QueryKey queryKey, List keyValues) {
        super(context);
        
        if (queryKey == null) {
            throw new NullPointerException("QueryKey is null");
        }
        
        this.queryKey = queryKey;
        this.keyValues = keyValues;
    }

    public void response(ResponseMessage message, long time) throws IOException {

        StoreResponse response = (StoreResponse)message;
        
        int maxOnce = DatabaseSettings.MAX_STORE_FORWARD_ONCE.getValue();
        int requesting = Math.min(maxOnce, response.getRequestCount());
        
        // TODO: What to do with the staus?
        Collection storeStatus = response.getStoreStatus();
        
        if (requesting > 0 && index < keyValues.size()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(response.getContactNode()
                        + " is requesting from us " + requesting + " KeyValues");
            }
            
            index += length;
            length = Math.min(requesting, keyValues.size()-index);
            
            List toSend = new ArrayList(length);
            for(int i = 0; i < length; i++) {
                toSend.add(keyValues.get(index + i));
            }
            
            int remaining = keyValues.size()-index-length;
            
            RequestMessage request 
                = context.getMessageFactory()
                    .createStoreRequest(response.getSocketAddress(), remaining, queryKey, toSend);
            
            context.getMessageDispatcher().send(response.getContactNode(), request, this);
        }
        
        // reset the error counter
        resetErrors();
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
