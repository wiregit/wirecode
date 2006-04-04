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
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.messages.RequestMessage;
import de.kapsi.net.kademlia.util.InputOutputUtils;

class Receipt {
    
    private static final Log LOG = LogFactory.getLog(Receipt.class);
    
    public static final int MAX_PACKET_SIZE = 8192;
    
    private Context context;
    
    private final KUID nodeId;
    private final SocketAddress dst;
    
    private ByteBuffer data;
    
    private final KUID messageId;
    private final ResponseHandler handler;
    
    private long sent = 0L;
    private long received = 0L;
    
    private boolean isRequest = false;
    
    public Receipt(Context context, KUID nodeId, SocketAddress dst, 
            Message message, ResponseHandler handler) throws IOException {
        
        byte[] data = InputOutputUtils.serialize(message);
        
        if (data.length >= MAX_PACKET_SIZE) {
            throw new IOException("Packet is too large: " + data.length);
        }
        
        this.context = context;
        
        this.nodeId = nodeId;
        this.dst = dst;
        
        this.data = ByteBuffer.wrap(data);
        
        this.messageId = message.getMessageID();
        this.isRequest = (message instanceof RequestMessage);
        
        this.handler = handler;
    }
    
    public boolean isRequest() {
        return isRequest;
    }
    
    public boolean compareNodeID(KUID nodeId) {
        return this.nodeId == null || this.nodeId.equals(nodeId);
    }
    
    public KUID getNodeID() {
        return nodeId;
    }
    
    public SocketAddress getDestination() {
        return dst;
    }
    
    public KUID getMessageID() {
        return messageId;
    }
    
    public ResponseHandler getHandler() {
        return handler;
    }
    
    public boolean send(DatagramChannel channel) throws IOException {
        return channel.send(data, dst) != 0;
    }
    
    public void sent() {
        sent = System.currentTimeMillis();
    }
    
    public void received() {
        received = System.currentTimeMillis();
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
    
    public void handleSuccess(KUID nodeId, SocketAddress src, 
            Message message) throws IOException {
        
        // A sends B a Ping
        // But B is offline and there is C who got B's IP from the ISP
        // C will respond to A's Ping
        // But C's NodeID is different
        // Make sure B is not C
        if (compareNodeID(nodeId)) {
            if (handler != null) {
                handler.handleResponse(nodeId, src, message, time());
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Wrong NodeID! Expected " + this.nodeId + " but got " 
                        + nodeId + " from " + src);
            }
            
            handleTimeout();
        }
    }
    
    public void handleTimeout() throws IOException {
        if (handler != null) {
            handler.handleTimeout(nodeId, dst, time());
        }
    }
    
    public void freeData() {
        data = null;
    }
    
    public String toString() {
        return ContactNode.toString(nodeId, dst) + "/" + messageId;
    }
}
