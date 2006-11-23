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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.ResponseHandler;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageID;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.routing.Contact;
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
        this.dst = contact.getContactAddress();
        
        this.message = message;
    }
    
    Tag(SocketAddress dst, RequestMessage message, ResponseHandler handler) 
            throws IOException {
        this(null, dst, message, handler, -1L);
    }
    
    Tag(Contact contact, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(contact.getNodeID(), contact.getContactAddress(), message, responseHandler, contact.getAdaptativeTimeout());
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
        
        this.responseHandler = responseHandler;
        this.timeout = timeout;
    }
    
    /**
     * Retruns true if this is a reuqest
     */
    public boolean isRequest() {
        return responseHandler != null 
                && (message instanceof RequestMessage);
    }
    
    /**
     * Returns the size of the serialized Message in bytes
     */
    public int getSize() {
        if (size < 0) {
            throw new IllegalStateException("Data is not set and the size is unknown");
        }
        return size;
    }
    
    /**
     * Returns the MessageID
     */
    public MessageID getMessageID() {
        return message.getMessageID();
    }
    
    /**
     * Returns the remote Node's ID. Might be null
     * if it's unknown
     */
    public KUID getNodeID() {
        return nodeId;
    }
    
    /**
     * Returns the remote Node's SocketAddress.
     */
    public SocketAddress getSocketAddress() {
        return dst;
    }
    
    /**
     * Returns the DHTMessage instance
     */
    public DHTMessage getMessage() {
        return message;
    }
    
    /**
     * Sets the serialized message data
     */
    public void setData(ByteBuffer data) {
        this.size = data.remaining();
        this.data = data;
    }
    
    /**
     * Returns serialized message data
     */
    public ByteBuffer getData() {
        if (data == null) {
            throw new IllegalStateException("Data is null");
        }
        return data;
    }
    
    /**
     * Marks this Message as sent and returns a Receipt
     * if this is a request
     */
    public Receipt sent() {
        sent = System.currentTimeMillis();
        data = null;
        return getReceipt();
    }
    
    /**
     * Creates and returns a Receipt if this is a request
     */
    private Receipt getReceipt() throws IllegalStateException {
        if (sent < 0L) {
            throw new IllegalStateException("Message has not been sent yet!");
        }
        
        if (isRequest()) {
            return new Receipt();
        } else {
            return null;
        }
    }
    
    /**
     * A delegate method to notify the ResponseHandler that
     * an error occured
     */
    public void handleError(IOException e) {
        if (responseHandler != null) {
            responseHandler.handleError(nodeId, dst, (RequestMessage)message, e);
        }
    }
    
    /**
     * Returns true if this Message was cancelled
     */
    public boolean isCancelled() {
        if (responseHandler != null) {
            return responseHandler.isCancelled();
        }
        return false;
    }
    
    /**
     * The Receipt class keeps track of requests we've sent and 
     * handles the response messages.
     */
    public class Receipt {
        
        private long received = -1L;
        
        private Receipt() {
            
        }
        
        /**
         * Returns the remote Node's ID to which this Message was
         * sent or null if it's unknown (certain PingRequests)
         */
        public KUID getNodeID() {
            return Tag.this.getNodeID();
        }
        
        /**
         * Returns the remote Node's SocketAddress to which this
         * Message was sent
         */
        public SocketAddress getSocketAddress() {
            return Tag.this.getSocketAddress();
        }
        
        /**
         * Returns the MessageID
         */
        public MessageID getMessageID() {
            return Tag.this.getMessageID();
        }
        
        /**
         * Returns the RequestMessage that was sent to the remote Node
         */
        public RequestMessage getRequestMessage() {
            return (RequestMessage)Tag.this.getMessage();
        }
        
        /**
         * Returns the size of the sent Message
         */
        public int getSentMessageSize() {
            return getSize();
        }
        
        /**
         * Sets the received time marker
         */
        public void received() {
            received = System.currentTimeMillis();
        }
        
        /**
         * Returns the Round Trip Time (RTT)
         */
        public long time() {
            if (received < 0L) {
                throw new IllegalStateException("The RTT is unknown as we have not received a response yet");
            }
            
            return received - sent;
        }
        
        /**
         * Returns the amount of time that has elapsed since
         * the request was sent
         */
        private long elapsedTime() {
            return System.currentTimeMillis() - sent;
        }
        
        /**
         * Returns whether or not this request has timedout
         */
        public boolean timeout() {
            long elapsed = elapsedTime();
            
            if(timeout < 0L) {
                long t = responseHandler.getTimeout();
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Default timeout: " + t + "ms for " + ContactUtils.toString(nodeId, dst));
                }
                return elapsed >= t;
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Timeout: " + timeout + "ms for " + ContactUtils.toString(nodeId, dst));
                }
                return elapsed >= timeout;
            }
        }
        
        /**
         * Checks if the response is coming from the expected Node.
         */
        private boolean compareNodeID(ResponseMessage response) {
            if (nodeId == null) {
                return (message instanceof PingRequest)
                    || (message instanceof StatsRequest);
            } else {
                Contact node = response.getContact();
                return nodeId.equals(node.getNodeID());
            }
        }
        
        /**
         * Checks if the response is coming from the expected Node.
         */
        // This is actually not really necessary. The QueryKey in
        // MessageID should take care of it.
        private boolean compareAddresses(ResponseMessage response) {
            Contact node = response.getContact();
            InetAddress dstAddr = ((InetSocketAddress)dst).getAddress();
            InetAddress srcAddr = ((InetSocketAddress)node.getContactAddress()).getAddress();
            return dstAddr.equals(srcAddr);
        }
        
        /**
         * Checks if the response is of the right type. That means
         * it's not possible to send a PONG for a FIND_NODE request
         * and so on.
         */
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
        
        /**
         * Checks if the ResponseMessage fulfils all exepcted
         * requirements
         */
        public boolean sanityCheck(ResponseMessage response) {
            return compareNodeID(response) 
                && compareAddresses(response)
                && compareResponseType(response);
        }
        
        /**
         * Returns the ResponseHandler instance
         */
        public ResponseHandler getResponseHandler() {
            return responseHandler;
        }
        
        /**
         * A delegate method to notify the ResponseHandler that 
         * an error occured
         */
        public void handleError(IOException e) {
            if (responseHandler != null) {
                responseHandler.handleError(nodeId, dst, (RequestMessage)message, e);
            }
        }
        
        /**
         * A delegate method to notify the ResponseHandler that
         * a tick has passed
         */
        public void handleTick() {
            if (responseHandler != null) {
                responseHandler.handleTick();
            }
        }
        
        public boolean isCancelled() {
            return Tag.this.isCancelled();
        }
    }
}
