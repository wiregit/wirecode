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

package de.kapsi.net.kademlia.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.NoOpResponseHandler;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StatsRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StatsResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;

class Tag {
    
    private KUID nodeId;
    private SocketAddress dst;
    
    private Message message;
    
    private ByteBuffer data;
    private int size;
    
    private ResponseHandler responseHandler;
    
    private long sent = -1L;
    
    public Tag(ContactNode node, ResponseMessage message) 
            throws IOException {
        
        data = ByteBuffer.wrap(InputOutputUtils.serialize(message));
        size = data.limit();
        
        if (size >= Message.MAX_MESSAGE_SIZE) {
            throw new IOException("Packet is too large: " + size + " >= " + Message.MAX_MESSAGE_SIZE);
        }
        
        this.nodeId = node.getNodeID();
        this.dst = node.getSocketAddress();
        
        this.message = message;
    }
    
    public Tag(SocketAddress dst, RequestMessage message, ResponseHandler handler) 
            throws IOException {
        this(null, dst, message, handler);
    }
    
    public Tag(ContactNode node, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        this(node.getNodeID(), node.getSocketAddress(), message, responseHandler);
    }
    
    public Tag(KUID nodeId, SocketAddress dst, RequestMessage message, ResponseHandler responseHandler) 
            throws IOException {
        
        data = ByteBuffer.wrap(InputOutputUtils.serialize(message));
        size = data.limit();
        
        if (size >= Message.MAX_MESSAGE_SIZE) {
            throw new IOException("Packet is too large: " + size + " >= " + Message.MAX_MESSAGE_SIZE);
        }
        
        this.nodeId = nodeId;
        this.dst = dst;
        
        this.message = message;
        
        if (responseHandler == null) {
            responseHandler = new NoOpResponseHandler();
        }
        
        this.responseHandler = responseHandler;
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
    
    public Message getMessage() {
        return message;
    }
    
    public boolean send(DatagramChannel channel) throws IOException {
        if (channel.send(data, dst) != 0) {
            sent = System.currentTimeMillis();
            data = null;
            return true;
        }
        return false;
    }
    
    public Receipt getReceipt() throws IllegalStateException {
        if (sent < 0L) {
            throw new IllegalStateException("Message has not been sent yet!");
        }
        
        if (responseHandler != null 
                && message instanceof RequestMessage) {
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
        
        public void received() {
            received = System.currentTimeMillis();
        }
        
        public long time() {
            return received - sent;
        }
        
        public boolean timeout() {
            return System.currentTimeMillis() - sent >= responseHandler.timeout();
        }
        
        private boolean compareNodeID(ResponseMessage response) {
            if (nodeId == null) {
                return (message instanceof PingRequest);
            } else {
                return nodeId.equals(response.getNodeID());
            }
        }
        
        // This is actually not really necessary. The QueryKey in
        // MessageID should take care of it.
        private boolean compareSocketAddress(ResponseMessage response) {
            return Tag.this.dst.equals(response.getSocketAddress());
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
