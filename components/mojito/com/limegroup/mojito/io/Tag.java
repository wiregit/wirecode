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
 
package com.limegroup.mojito.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.ContactNode;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.NoOpResponseHandler;
import com.limegroup.mojito.handler.ResponseHandler;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.request.FindNodeRequest;
import com.limegroup.mojito.messages.request.FindValueRequest;
import com.limegroup.mojito.messages.request.PingRequest;
import com.limegroup.mojito.messages.request.StatsRequest;
import com.limegroup.mojito.messages.request.StoreRequest;
import com.limegroup.mojito.messages.response.FindNodeResponse;
import com.limegroup.mojito.messages.response.FindValueResponse;
import com.limegroup.mojito.messages.response.PingResponse;
import com.limegroup.mojito.messages.response.StatsResponse;
import com.limegroup.mojito.messages.response.StoreResponse;


class Tag {
    
    private static final Log LOG = LogFactory.getLog(Tag.class);
    
    private KUID nodeId;
    private SocketAddress dst;
    
    private DHTMessage message;
    
    private ByteBuffer data;
    private int size;
    
    private ResponseHandler responseHandler;
    
    private long sent = -1L;
    
    private long timeout = -1L;
    
    public Tag(ContactNode node, ResponseMessage message) 
            throws IOException {
        
        data = ByteBuffer.wrap(InputOutputUtils.serialize(message));
        size = data.limit();
        
        if (size >= DHTMessage.MAX_MESSAGE_SIZE) {
            throw new IOException("Packet is too large: " + size + " >= " + DHTMessage.MAX_MESSAGE_SIZE);
        }
        
        this.nodeId = node.getNodeID();
        this.dst = node.getSocketAddress();
        
        this.message = message;
    }
    
    public Tag(SocketAddress dst, RequestMessage message, ResponseHandler handler) 
            throws IOException {
        this(null, dst, message, handler, -1L);
    }
    
    public Tag(ContactNode node, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(node.getNodeID(), node.getSocketAddress(), message, responseHandler, node.getAdaptativeTimeOut());
    }
    
    public Tag(KUID nodeId, SocketAddress dst, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(nodeId, dst, message, responseHandler, -1L);
    }
    
    public Tag(KUID nodeId, SocketAddress dst, RequestMessage message, ResponseHandler responseHandler, long timeout) 
            throws IOException {
        
        data = ByteBuffer.wrap(InputOutputUtils.serialize(message));
        size = data.limit();
        
        if (size >= DHTMessage.MAX_MESSAGE_SIZE) {
            throw new IOException("Packet is too large: " + size + " >= " + DHTMessage.MAX_MESSAGE_SIZE);
        }
        
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
    
    public ByteBuffer getData() {
        return data;
    }
    
    public boolean send(DatagramChannel channel) throws IOException {
        if (channel.send(data, dst) != 0) {
            sent();
            return true;
        }
        return false;
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
                    LOG.debug("Default timeout: " + t + "ms for " + ContactNode.toString(nodeId, dst));
                }
                return time >= t;
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Timeout: " + timeout + "ms for " + ContactNode.toString(nodeId, dst));
                }
                return time >= timeout;
            }
        }
        
        private boolean compareNodeID(ResponseMessage response) {
            if (nodeId == null) {
                return (message instanceof PingRequest)
                    || (message instanceof StatsRequest);
            } else {
                return nodeId.equals(response.getSourceNodeID());
            }
        }
        
        // This is actually not really necessary. The QueryKey in
        // MessageID should take care of it.
        private boolean compareSocketAddress(ResponseMessage response) {
            return Tag.this.dst.equals(response.getSourceAddress());
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
