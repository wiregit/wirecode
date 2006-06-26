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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
import com.limegroup.mojito.event.StoreResponseListener;
import com.limegroup.mojito.handler.AbstractResponseHandler;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.messages.StoreResponse.StoreStatus;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The StoreResponseHandler handles storing of one or more 
 * KeyValues at a given Node. It sends one KeyValue at once,
 * waits for the response and sends the next KeyValue until
 * all KeyValues were sent.
 */
public class StoreResponseHandler extends AbstractResponseHandler {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
        
    private QueryKey queryKey;
    
    private int index = 0;
    private List<KeyValue> keyValues;
    
    public StoreResponseHandler(Context context, QueryKey queryKey, KeyValue keyValue) {
        this(context, queryKey, Arrays.asList(new KeyValue[]{keyValue}));
    }
    
    public StoreResponseHandler(Context context, QueryKey queryKey, List<KeyValue> keyValues) {
        super(context);
        
        this.queryKey = queryKey;
        this.keyValues = keyValues;
    }
    
    public void addStoreListener(StoreResponseListener listener) {
        listeners.add(listener);
    }
    
    public void removeStoreListener(StoreResponseListener listener) {
        listeners.remove(listener);
    }

    public StoreResponseListener[] getStoreListeners() {
        return (StoreResponseListener[])listeners.toArray(new StoreResponseListener[0]);
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }
    
    public List<KeyValue> getKeyValues() {
        return keyValues;
    }
    
    public void store(Contact node) throws IOException {
        if (index < keyValues.size() && !isStopped()) {
            KeyValue keyValue = keyValues.get(index);
            
            StoreRequest request = context.getMessageHelper()
                .createStoreRequest(node.getSocketAddress(), queryKey, keyValue);
            
            context.getMessageDispatcher().send(node, request, this);
        }
    }
    
    public void response(ResponseMessage message, long time) throws IOException {

        StoreResponse response = (StoreResponse)message;
        
        KUID valueId = response.getValueID();
        StoreStatus status = response.getStatus();
        
        if (index < keyValues.size()) {
            KeyValue current = keyValues.get(index);
            
            if (!current.getKey().equals(valueId)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(message.getContact() + " is ACK'ing a KeyValue " 
                            + valueId + " we have not requested to store!");
                }
                
                Contact node = message.getContact();
                fireStoreFailed(node.getNodeID(), node.getSocketAddress());
                return;
            }
            
            if (status == StoreStatus.SUCCEEDED) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(message.getContact() + " sucessfully stored KeyValue " + valueId);
                }
            } else if (status == StoreStatus.FAILED) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(message.getContact() + " failed to store KeyValue " + valueId);
                }
            } else {
                if (LOG.isErrorEnabled()) {
                    LOG.error(message.getContact() + " returned unknown status code " 
                            + status + " for KeyValue " + valueId);
                }
                
                Contact node = message.getContact();
                fireStoreFailed(node.getNodeID(), node.getSocketAddress());
                return;
            }
            
            // Reset the error counter
            resetErrors();
            
            // Store next...
            index++;
            if (index < keyValues.size()) {
                store(message.getContact());
            } else {
                fireStoreSucceeded(message.getContact());
            }
        }
    }
    
    protected void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Max number of errors occured. Giving up!");
        }
        
        fireStoreFailed(nodeId, dst);
    }
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a store request to " + ContactUtils.toString(nodeId, dst) + " failed", e);
        }
        
        fireTimeout(nodeId, dst, message, -1L);
        fireStoreFailed(nodeId, dst);
    }
    
    public void fireStoreSucceeded(final Contact node) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        StoreResponseListener listener = (StoreResponseListener)it.next();
                        listener.storeSucceeded(node, keyValues);
                    }
                }
            }
        });
    }
    
    public void fireStoreFailed(final KUID nodeId, final SocketAddress dst) {
        context.fireEvent(new Runnable() {
            public void run() {
                if (!isStopped()) {
                    for(Iterator it = listeners.iterator(); it.hasNext(); ) {
                        StoreResponseListener listener = (StoreResponseListener)it.next();
                        listener.storeFailed(nodeId, dst, keyValues);
                    }
                }
            }
        });
    }
}
