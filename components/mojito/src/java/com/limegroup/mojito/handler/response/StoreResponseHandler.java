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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.KeyValue;
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
public class StoreResponseHandler extends AbstractResponseHandler<Contact> {
    
    private static final Log LOG = LogFactory.getLog(StoreResponseHandler.class);
        
    private QueryKey queryKey;
    
    private int index = 0;
    private List<KeyValue> keyValues;
    
    public StoreResponseHandler(Context context, QueryKey queryKey, KeyValue keyValue) {
        this(context, queryKey, Arrays.asList(keyValue));
    }
    
    public StoreResponseHandler(Context context, QueryKey queryKey, List<KeyValue> keyValues) {
        super(context);
        
        this.queryKey = queryKey;
        this.keyValues = keyValues;
    }
    
    public QueryKey getQueryKey() {
        return queryKey;
    }
    
    public List<KeyValue> getKeyValues() {
        return keyValues;
    }
    
    public void store(Contact node) throws IOException {
        if (index < keyValues.size() && !isCancelled()) {
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
                
                fireStoreFailed(message.getContact());
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
                
                fireStoreFailed(message.getContact());
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
    
    protected void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException {
        fireTimeoutException(nodeId, dst, message, time);
    }
    
    public void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a store request to " + ContactUtils.toString(nodeId, dst) + " failed", e);
        }
        
        setException(new Exception(message.toString(), e));
    }
    
    private void fireStoreSucceeded(Contact node) {
        setReturnValue(node);
    }
    
    public void fireStoreFailed(Contact node) {
        setException(new StoreException(node));
    }
    
    public static class StoreException extends Exception {
        private static final long serialVersionUID = 7739569179285045326L;

        public StoreException(Contact node) {
            super(node.toString());
        }
    }
}
