/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.messages.Message;
import de.kapsi.net.kademlia.util.InputOutputUtils;

class Receipt {
    
    public static final int MAX_PACKET_SIZE = 8192;
    
    private final KUID nodeId;
    private final SocketAddress dst;
    
    private ByteBuffer data;
    
    private final KUID messageId;
    private final ResponseHandler handler;
    
    private long sent = 0L;
    private long received = 0L;
    
    public Receipt(KUID nodeId, SocketAddress dst, 
            Message message, ResponseHandler handler) throws IOException {
        this(nodeId, dst, InputOutputUtils.serialize(message), 
                message.getMessageID(), handler);
    }
    
    public Receipt(KUID nodeId, SocketAddress dst, byte[] data, 
            KUID messageId, ResponseHandler handler) throws IOException {
        
        if (data.length >= MAX_PACKET_SIZE) {
            throw new IOException("Packet is too large: " + data.length);
        }
        
        this.nodeId = nodeId;
        this.dst = dst;
        
        this.data = ByteBuffer.wrap(data);
        
        this.messageId = messageId;
        this.handler = handler;
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
        
        if (this.nodeId != null 
                && nodeId != null
                && !this.nodeId.equals(nodeId)) {
        }
        
        /*if (!dst.equals(src)) {
            throw new IOException(ContactNode.toString(nodeId, src) + " claims to be " 
                    + ContactNode.toString(this.nodeId, dst));
        }*/
        
        if (handler != null) {
            handler.handleResponse(nodeId, src, message, time());
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