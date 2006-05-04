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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.messages.ResponseMessage;
import de.kapsi.net.kademlia.messages.request.FindNodeRequest;
import de.kapsi.net.kademlia.messages.request.FindValueRequest;
import de.kapsi.net.kademlia.messages.request.PingRequest;
import de.kapsi.net.kademlia.messages.request.StoreRequest;
import de.kapsi.net.kademlia.messages.response.FindNodeResponse;
import de.kapsi.net.kademlia.messages.response.FindValueResponse;
import de.kapsi.net.kademlia.messages.response.PingResponse;
import de.kapsi.net.kademlia.messages.response.StoreResponse;

public class Receipt {
    
    private static final Log LOG = LogFactory.getLog(Receipt.class);
    
    public static final int MAX_PACKET_SIZE = 8192;
    
    private KUID nodeId;
    private SocketAddress dst;
    
    private Message message;
    private ByteBuffer data;
    private int size;
    
    private ResponseHandler handler;
    
    private long sent = 0L;
    private long received = 0L;
    
    Receipt(KUID nodeId, SocketAddress dst, 
            Message message, ResponseHandler handler) throws IOException {
        
        data = ByteBuffer.wrap(InputOutputUtils.serialize(message));
        size = data.limit();
        
        if (size >= MAX_PACKET_SIZE) {
            throw new IOException("Packet is too large: " + size + " >= " + MAX_PACKET_SIZE);
        }
        
        this.nodeId = nodeId;
        this.dst = dst;
        
        this.message = message;
        this.handler = handler;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public boolean isRequest() {
        return (message instanceof RequestMessage);
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getDestination() {
        return dst;
    }
    
    public KUID getMessageID() {
        return message.getMessageID();
    }
    
    public ResponseHandler getHandler() {
        return handler;
    }
    
    public long time() {
        return received-sent;
    }
    
    public boolean timeout() {
        if (handler != null) {
            long delta = System.currentTimeMillis()-sent;
            return delta >= handler.timeout();
        }
        return false;
    }
    
    public int size() {
        return size;
    }
    
    public int hashCode() {
        return getMessageID().hashCode();
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Receipt)) {
            return false;
        }
        
        Receipt other = (Receipt)o;
        return getMessageID().equals(other.getMessageID());
    }
    
    public String toString() {
        return ContactNode.toString(nodeId, dst) + "/" + getMessageID();
    }
    
    boolean compareNodeID(KUID nodeId) {
        return this.nodeId == null || this.nodeId.equals(nodeId);
    }
    
    boolean compareResponseType(ResponseMessage response) {
        if (message instanceof PingRequest) {
            return response instanceof PingResponse;
        } else if (message instanceof FindNodeRequest) {
            return response instanceof FindNodeResponse;
        } else if (message instanceof FindValueRequest) {
            return (response instanceof FindNodeResponse) 
                || (response instanceof FindValueResponse);
        } else if (message instanceof StoreRequest) {
            return response instanceof StoreResponse;
        }
        
        return false;
    }
    
    boolean send(DatagramChannel channel) throws IOException {
        if (channel.send(data, dst) != 0) {
            freeData();
            return true;
        }
        return false;
    }
    
    private void freeData() {
        data = null;
    }
    
    void sent() {
        sent = System.currentTimeMillis();
    }
    
    void received() {
        received = System.currentTimeMillis();
    }
    
    void handleResponse(ResponseMessage response) throws IOException {
        
        // A sends B a Ping
        // But B is offline and there is C who got B's IP from the ISP
        // C will respond to A's Ping
        // But C's NodeID is different
        // Make sure B is not C
        if (!compareNodeID(nodeId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Wrong NodeID! Expected " + this.nodeId + " but got " 
                        + response.getNodeID() + " from " + response.getSocketAddress());
            }
            
            handleTimeout();
            return;
        } else if (!compareResponseType(response)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Wrong response type! Got " + response.getClass().getName() 
                        + " for " + message.getClass().getName());
            }
            
            handleTimeout();
            return;
        }
        
        if (handler != null) {
            handler.addTime(time());
            handler.handleResponse(response, time());
        }
    }
    
    void handleTimeout() throws IOException {
        if (handler != null) {
            handler.addTime(time());
            handler.handleTimeout(nodeId, dst, (RequestMessage)message, time());
        }
    }
}
