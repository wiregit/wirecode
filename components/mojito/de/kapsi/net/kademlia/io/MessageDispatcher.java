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
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.NetworkStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.handler.DefaultMessageHandler;
import de.kapsi.net.kademlia.handler.RequestHandler;
import de.kapsi.net.kademlia.handler.ResponseHandler;
import de.kapsi.net.kademlia.handler.request.LookupRequestHandler;
import de.kapsi.net.kademlia.handler.request.PingRequestHandler;
import de.kapsi.net.kademlia.handler.request.StoreRequestHandler;
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
import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public class MessageDispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    private static final long CLEANUP_INTERVAL = 3L * 1000L;
    private static final long SLEEP = 50L;
    
    private LinkedList outputQueue = new LinkedList();
    private ReceiptMap messageMap = new ReceiptMap(1024);
    
    private Selector selector;
    private DatagramChannel channel;
    
    private Context context;
    
    private DefaultMessageHandler defaultHandler;
    private PingRequestHandler pingHandler;
    private LookupRequestHandler lookupHandler;
    private StoreRequestHandler storeHandler;
    
    private Filter filter;
    
    private NetworkStatisticContainer networkStats;
    
    private long lastCleanup = 0L;
    
    public MessageDispatcher(Context context) {
        this.context = context;
        networkStats = context.getNetworkStats();
        
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        lookupHandler = new LookupRequestHandler(context);
        storeHandler = new StoreRequestHandler(context);
        
        filter = new Filter();
    }
    
    public int getReceivedMessagesCount() {
        return (int)networkStats.RECEIVED_MESSAGES_COUNT.getTotal();
    }
    
    public long getReceivedMessagesSize() {
        return (long)networkStats.RECEIVED_MESSAGES_SIZE.getTotal();
    }
    
    public int getSentMessagesCount() {
        return (int)networkStats.SENT_MESSAGES_COUNT.getTotal();
    }
    
    public long getSentMessagesSize() {
        return (long)networkStats.SENT_MESSAGES_SIZE.getTotal();
    }
    
    public void stop() throws IOException {
        if (context.isRunning()) {
            close();
        }
    }
    
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("Already open");
        }
        
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        
        DatagramSocket socket = channel.socket();
        socket.setReuseAddress(false);
        socket.setReceiveBufferSize(Receipt.MAX_PACKET_SIZE);
        socket.setSendBufferSize(Receipt.MAX_PACKET_SIZE);
        
        socket.bind(address);
    }
    
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }
    
    private boolean isRunning() {
        return isOpen() && channel.isRegistered();
    }
    
    public void close() throws IOException {
        if (!isOpen()) {
            return;
        }
        
        channel.close();
        
        try { 
            selector.close(); 
        } catch (IOException ignore) {}
        
        messageMap.clear();
        
        synchronized (outputQueue) {
            outputQueue.clear();
        }
    }
    
    public SocketAddress getLocalSocketAddress() {
        return (channel != null ? channel.socket().getLocalSocketAddress() : null);
    }
    
    public void send(SocketAddress dst, Message message, 
            ResponseHandler handler) throws IOException {
        send(null, dst, message, handler);
    }
    
    public void send(ContactNode dst, Message message, 
            ResponseHandler handler) throws IOException {
        send(dst.getNodeID(), dst.getSocketAddress(), message, handler);
    }
    
    public void send(KUID nodeId, SocketAddress dst, Message message, 
            ResponseHandler handler) throws IOException {
        
        if (!isOpen()) {
            throw new IOException("Channel is not bound");
        }
        
        // Make sure we're not sending messages to ourself.
        // The only exception are Pings/Pongs
        if (context.isLocalNodeID(nodeId)) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send message of type " + message.getClass().getName() 
                        + " to ourself " + ContactNode.toString(nodeId, dst));
            }
            return;
        }
        
        if (handler == null) {
            handler = defaultHandler;
        }
        
        Receipt receipt = new Receipt(nodeId, dst, message, handler);
        
        synchronized(outputQueue) {
            outputQueue.add(receipt);
            interestWrite(true);
        }
    }
    
    public void processWrite() throws IOException {
        synchronized (outputQueue) {
            while(!outputQueue.isEmpty()) {
                Receipt receipt = (Receipt)outputQueue.removeFirst();
                
                if (receipt.send(channel)) {
                    // Wohoo! Message was sent!
                    receipt.sent();
                    networkStats.SENT_MESSAGES_COUNT.incrementStat();
                    networkStats.SENT_MESSAGES_SIZE.addData(receipt.size()); // compressed size
                    
                    if (receipt.isRequest()) {
                        messageMap.put(receipt.getMessageID(), receipt);
                    }
                } else {
                    // Dang! Re-Try next time!
                    outputQueue.addFirst(receipt);
                    break;
                }
            }
            
            interestWrite(!outputQueue.isEmpty());
        }
    }
    
    private ByteBuffer buffer = ByteBuffer.allocate(Receipt.MAX_PACKET_SIZE);
    
    private void processRead() throws IOException {
        while(isRunning()) {
            Message message = null;
            try {
                message = readMessage();
            } catch (MessageFormatException err) {
                LOG.error("Message Format Exception: ", err);
                continue;
            }
            
            if (message == null) {
                break;
            }
            
            Receipt receipt = null;
            if (message instanceof ResponseMessage) {
                receipt = (Receipt)messageMap.remove(message.getMessageID());
                
                if (receipt != null) {
                    receipt.received();
                }
            }
            
            processMessage(receipt, message);
        }
        
        interestRead(true); // We're always interested in reading
    }
    
    private Message readMessage() throws MessageFormatException, IOException {
        SocketAddress src = channel.receive((ByteBuffer)buffer.clear());
        if (src != null) {
            int length = buffer.position();
            byte[] data = new byte[length];
            buffer.flip();
            buffer.get(data, 0, length);
            
            Message message = InputOutputUtils.deserialize(src, data);
            networkStats.RECEIVED_MESSAGES_COUNT.incrementStat();
            networkStats.RECEIVED_MESSAGES_SIZE.addData(data.length); // compressed size!
            return message;
        }
        return null;
    }
    
    private void processMessage(Receipt receipt, Message message) {
        
        KUID nodeId = message.getNodeID();
        SocketAddress src = message.getSocketAddress();
        
        // Make sure we're not receiving messages from ourself.
        // The only exception are Pings/Pongs
        if (context.isLocalNodeID(nodeId)
                || src.equals(context.getSocketAddress())) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Received a message of type " + message.getClass().getName() 
                        + " from ourself " + ContactNode.toString(nodeId, src));
            }
            return;
        }
        
        if (message instanceof ResponseMessage) {
            processResponse(receipt, (ResponseMessage)message);
        } else if (message instanceof RequestMessage) {
            processRequest((RequestMessage)message);
        } else if (LOG.isFatalEnabled()) {
            LOG.fatal(message + " is neither a Request nor a Response. Fix the code!");
        }
    }
    
    private void processResponse(Receipt receipt, ResponseMessage response) {
        
        // Check if we ever sent a such request...
        if (!response.verify()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(response.getContactNode() + " sent us an unrequested response!");
            }
            
            // IMPORTANT: We cannot add a penalty here! A malicious
            // Node could fake its NodeID and SocketAddress, send us
            // responses and we'd penalize a honest Node!
            return;
        }
        
        // A null Receipt means it timed out and we're no
        // longer handling the response. But it's nice to know
        // the Node is still alive! Do something with the info!
        
        try {
            if (receipt != null) {
                defaultHandler.handleResponse(response, receipt.time()); // BEFORE!
                if (receipt.getHandler() != defaultHandler) {
                    receipt.handleResponse(response);
                }
                
            } else {
                // Make sure a singe Node cannot monopolize our resources
                if (!filter.allow(response.getSocketAddress())) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(response.getContactNode() + " refused");
                    }
                    networkStats.FILTERED_MESSAGES.incrementStat();
                    // return;
                }
                
                handleLateResponse(response);
            }
        } catch (Throwable t) {
            LOG.error("Response handler error: ", t);
        }
    }
    
    private void processRequest(RequestMessage request) {
        // Make sure a singe Node cannot monopolize our resources
        if (!filter.allow(request.getSocketAddress())) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(request.getContactNode() + " refused");
            }
            networkStats.FILTERED_MESSAGES.incrementStat();
            // return;
        }
        
        RequestHandler requestHandler = null;
        
        if (request instanceof PingRequest) {
            requestHandler = pingHandler;
        } else if (request instanceof FindNodeRequest
                || request instanceof FindValueRequest) {
            requestHandler = lookupHandler;
        } else if (request instanceof StoreRequest) {
            requestHandler = storeHandler;
        }
        
        if (requestHandler != null) {
            try {
                requestHandler.handleRequest(request);
                defaultHandler.handleRequest(request); // AFTER!
            } catch (Throwable t) {
                LOG.error("Request handler error: ",t);
            }
        }
    }
    
    private void handleLateResponse(ResponseMessage response) {
        
        ContactNode node = response.getContactNode();
        
        if (LOG.isTraceEnabled()) {
            if (response instanceof PingResponse) {
                LOG.trace("Received a late Pong from " + node);
            } else if (response instanceof FindNodeResponse) {
                LOG.trace("Received a late FindNode response from " + node);
            } else if (response instanceof FindValueResponse) {
                LOG.trace("Received a late FindValue response from " + node);
            } else if (response instanceof StoreResponse) {
                LOG.trace("Received a late Store response from " + node);
            }
        }
        
        networkStats.LATE_MESSAGES_COUNT.incrementStat();
        context.getRouteTable().add(node, true);
    }
    
    private void interest(int ops, boolean on) {
        try {
            SelectionKey sk = channel.keyFor(selector);
            if (sk != null && sk.isValid()) {
                synchronized(channel.blockingLock()) {
                    if (on) {
                        sk.interestOps(sk.interestOps() | ops);
                    } else {
                        sk.interestOps(sk.interestOps() & ~ops);
                    }
                }
            }
        } catch (CancelledKeyException ignore) {}
    }

    private void interestRead(boolean on) {
        interest(SelectionKey.OP_READ, on);
    }
    
    private void interestWrite(boolean on) {
        interest(SelectionKey.OP_WRITE, on);
    }
    
    private void processCleanup() {
        if ((System.currentTimeMillis()-lastCleanup) >= CLEANUP_INTERVAL) {
            messageMap.cleanup();
            lastCleanup = System.currentTimeMillis();
        }
    }
    
    public void run() {

        networkStats.SENT_MESSAGES_COUNT.clearData();
        networkStats.RECEIVED_MESSAGES_COUNT.clearData();
        
        while(isRunning()) {
            try {
                selector.select(SLEEP);
                
                // READ
                processRead();
                
                // CLEANUP
                processCleanup();
                
                // WRITE
                processWrite();
            } catch (ClosedChannelException err) {
                // thrown as close() is called asynchron
                //LOG.error(err);
            } catch (IOException err) {
                LOG.fatal("MessageHandler IO exception: ",err);
            }
        }
    }
    
    private class ReceiptMap extends FixedSizeHashMap {
        
        private static final long serialVersionUID = -3084244582682726933L;

        public ReceiptMap(int maxSize) {
            super(maxSize);
        }

        public void cleanup() {
            Iterator it = entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                Receipt receipt = (Receipt)entry.getValue();
                
                if (receipt.timeout()) {
                    receipt.received();
                    it.remove();
                    networkStats.RECEIPTS_TIMEOUT.incrementStat();
                    
                    try {
                        receipt.handleTimeout();
                    } catch (Throwable t) {
                        LOG.error("ReceiptMap cleanup error: ",t);
                    }
                }
            }
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            Receipt receipt = (Receipt)eldest.getValue();
            if (super.removeEldestEntry(eldest) 
                    || receipt.timeout()) {
                receipt.received();
                if(receipt.timeout()) {
                    networkStats.RECEIPTS_TIMEOUT.incrementStat();
                } else {
                    networkStats.RECEIPTS_EVICTED.incrementStat();
                }
                try {
                    receipt.handleTimeout();
                } catch (Throwable t) {
                    LOG.error("ReceiptMap removeEldestEntry error: ",t);
                }
                return true;
            }
            return false;
        }
    }
}
