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
 
package com.limegroup.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Contact;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.NoOpResponseHandler;
import com.limegroup.mojito.handler.ResponseHandler;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.util.ContactUtils;

/**
 * The Tag class is a wrapper for outgoing DHTMessages. For 
 * sent Requests you may obtain a Receipt.
 */
public class Tag {
    
    private static final Log LOG = LogFactory.getLog(Tag.class);
    
    private KUID nodeId;
    private SocketAddress dst;
    
    private DHTMessage message;
    
    private ByteBuffer data;
    private int size = -1;
    
    private ResponseHandler responseHandler;
    
    private long sent = -1L;
    
    private long timeout = -1L;
    
    Tag(Contact contact, ResponseMessage message) 
            throws IOException {
        
        this.nodeId = contact.getNodeID();
        this.dst = contact.getSocketAddress();
        
        this.message = message;
    }
    
    Tag(SocketAddress dst, RequestMessage message, ResponseHandler handler) 
            throws IOException {
        this(null, dst, message, handler, -1L);
    }
    
    Tag(Contact contact, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(contact.getNodeID(), contact.getSocketAddress(), message, responseHandler, contact.getAdaptativeTimeout());
    }
    
    Tag(KUID nodeId, SocketAddress dst, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(nodeId, dst, message, responseHandler, -1L);
    }
    
    Tag(KUID nodeId, SocketAddress dst, RequestMessage message, ResponseHandler responseHandler, long timeout) 
            throws IOException {
        
        this.nodeId = nodeId;
        this.dst = dst;
        
        this.message = message;
        
        if (responseHandler == null) {
            responseHandler = new NoOpResponseHandler();
        }
        
        this.responseHandler = responseHandler;
        this.timeout = timeout;
    }
    
    public boolean isRequest() {
        return responseHandler != null 
                && (message instanceof RequestMessage);
    }
    
    public int getSize() {
        if (size < 0) {
            throw new IllegalStateException("Data is not set and the size is unknown");
        }
        return size;
    }
    
    public KUID getMessageID() {
        return message.getMessageID();
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getSocketAddres() {
        return dst;
    }
    
    public DHTMessage getMessage() {
        return message;
    }
    
    public void setData(ByteBuffer data) {
        this.size = data.remaining();
        this.data = data;
    }
    
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalStateException("Data is null");
        }
        return data;
    }
    
    public Receipt sent() {
        sent = System.currentTimeMillis();
        data = null;
        return getReceipt();
    }
    
    public Receipt getReceipt() throws IllegalStateException {
        if (sent < 0L) {
            throw new IllegalStateException("Message has not been sent yet!");
        }
        
        if (isRequest()) {
            return new Receipt();
        } else {
            return null;
        }
    }
    
    public void handleError(Exception e) {
        if (responseHandler != null) {
            responseHandler.handleError(nodeId, dst, (RequestMessage)message, e);
        }
    }
    
    /**
     * The Receipt class keeps track of requests we've sent and 
     * handles the response messages.
     */
    public class Receipt {
        
        private long received = -1L;
        
        private Receipt() {
            
        }
        
        public KUID getNodeID() {
            return Tag.this.getNodeID();
        }
        
        public SocketAddress getSocketAddress() {
            return Tag.this.getSocketAddres();
        }
        
        public KUID getMessageID() {
            return Tag.this.getMessageID();
        }
        
        public RequestMessage getRequestMessage() {
            return (RequestMessage)Tag.this.getMessage();
        }
        
        public int getSentMessageSize() {
            return getSize();
        }
        
        public void received() {
            received = System.currentTimeMillis();
        }
        
        public long time() {
            return received - sent;
        }
        
        public boolean timeout() {
            long time = System.currentTimeMillis() - sent;
            if(timeout < 0L) {
                long t = responseHandler.timeout();
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Default timeout: " + t + "ms for " + ContactUtils.toString(nodeId, dst));
                }
                return time >= t;
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Timeout: " + timeout + "ms for " + ContactUtils.toString(nodeId, dst));
                }
                return time >= timeout;
            }
        }
        
        private boolean compareNodeID(ResponseMessage response) {
            if (nodeId == null) {
                return (message instanceof PingRequest)
                    || (message instanceof StatsRequest);
            } else {
                Contact node = response.getContact();
                return nodeId.equals(node.getNodeID());
            }
        }
        
        // This is actually not really necessary. The QueryKey in
        // MessageID should take care of it.
        private boolean compareSocketAddress(ResponseMessage response) {
            Contact node = response.getContact();
            return Tag.this.dst.equals(node.getSocketAddress());
        }
        
        private boolean compareResponseType(ResponseMessage response) {
            if (message instanceof PingRequest) {
                return response instanceof PingResponse;
            } else if (message instanceof FindNodeRequest) {
                return response instanceof FindNodeResponse;
            } else if (message instanceof FindValueRequest) {
                return (response instanceof FindNodeResponse) 
                    || (response instanceof FindValueResponse);
            } else if (message instanceof StoreRequest) {
                return response instanceof StoreResponse;
            } else if (message instanceof StatsRequest) {
                return response instanceof StatsResponse;
            }
            
            return false;
        }
        
        public boolean sanityCheck(ResponseMessage response) {
            return compareNodeID(response) 
                && compareSocketAddress(response)
                && compareResponseType(response);
        }
        
        public ResponseHandler getResponseHandler() {
            return responseHandler;
        }
        
        public void handleError(Exception e) {
            if (responseHandler != null) {
                responseHandler.handleError(nodeId, dst, (RequestMessage)message, e);
            }
        }
    }
}
