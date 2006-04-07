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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.AbstractResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.StoreResponse;
import de.kapsi.net.kademlia.security.QueryKey;
import de.kapsi.net.kademlia.settings.NetworkSettings;

/**
 * Currently unused. Could be used to ACK/NACK store
 * requests
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
    
    private int errors = 0;
    private boolean done = false;
    
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

    public void handleResponse(KUID nodeId, SocketAddress src, 
            Message message, long time) throws IOException {
        
        if (done) {
            return;
        }
        
        StoreResponse response = (StoreResponse)message;
        
        int requesting = response.getRequestCount();
        
        // TODO: What to do with the staus?
        Collection storeStatus = response.getStoreStatus();
        
        if (requesting > 0 && index < keyValues.size()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(ContactNode.toString(nodeId, src) 
                        + " is requesting from us " + requesting + " KeyValues");
            }
            
            index += length;
            length = Math.min(requesting, keyValues.size()-index);
            
            List toSend = new ArrayList(length);
            for(int i = 0; i < length; i++) {
                toSend.add(keyValues.get(index + i));
            }
            
            int remaining = keyValues.size()-index-length;
            StoreRequest request 
                = (StoreRequest)context.getMessageFactory()
                    .createStoreRequest(remaining, queryKey, toSend);
            
            context.getMessageDispatcher().send(nodeId, src, request, this);
            
            done = (remaining == 0);
        }
        
        // reset the error counter
        errors = 0;
    }

    public void handleTimeout(KUID nodeId, SocketAddress dst, long time) 
            throws IOException {
        
        if (done) {
            return;
        }
        
        if (++errors >= NetworkSettings.MAX_ERRORS.getValue()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Max number of errors occured. Giving up!");
            }
            return;
        }
        
        int remaining = keyValues.size()-index;
        
        if (remaining > 0) {
            StoreRequest request 
                = (StoreRequest)context.getMessageFactory()
                    .createStoreRequest(remaining, queryKey, Collections.EMPTY_LIST);
        
            context.getMessageDispatcher().send(nodeId, dst, request, this);
        }
    }
}
