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
import de.kapsi.net.kademlia.handler.request.FindNodeRequestHandler;
import de.kapsi.net.kademlia.handler.request.FindValueRequestHandler;
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
import de.kapsi.net.kademlia.util.InputOutputUtils;

public class MessageDispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    private static final long CLEANUP_INTERVAL = 3L * 1000L;
    private static final long SLEEP = 50L;
    
    private Object queueLock = new Object();
    private LinkedList outputQueue = new LinkedList();
    private ReceiptMap messageMap = new ReceiptMap(1024);
    
    private Selector selector;
    private DatagramChannel channel;
    
    private Context context;
    
    private DefaultMessageHandler defaultHandler;
    private PingRequestHandler pingHandler;
    private FindNodeRequestHandler findNodeHandler;
    private FindValueRequestHandler findValueHandler;
    private StoreRequestHandler storeHandler;
    
    private Filter filter;
    
    protected final NetworkStatisticContainer networkStats;
    
    public MessageDispatcher(Context context) {
        this.context = context;
        networkStats = context.getNetworkStats();
        
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        findNodeHandler = new FindNodeRequestHandler(context);
        findValueHandler = new FindValueRequestHandler(context);
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
        
        synchronized (queueLock) {
            messageMap.clear();
            outputQueue.clear();
        }
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
        if (nodeId != null 
                && nodeId.equals(context.getLocalNodeID())) {
            
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
        
        synchronized(queueLock) {
            outputQueue.add(receipt);
            interestWrite(true);
        }
    }
    
    private void handleRequest(KUID nodeId, SocketAddress src, RequestMessage msg) throws IOException {
        RequestHandler requestHandler = null;
        
        if (msg instanceof PingRequest) {
            requestHandler = pingHandler;
        } else if (msg instanceof FindNodeRequest) {
            requestHandler = findNodeHandler;
        } else if (msg instanceof FindValueRequest) {
            requestHandler = findValueHandler;
        } else if (msg instanceof StoreRequest) {
            requestHandler = storeHandler;
        }
        
        if (requestHandler != null) {
            try {
                requestHandler.handleRequest(nodeId, src, msg);
            } catch (Throwable t) {
                LOG.error("MessageHandler handle request error: ",t);
            }
        }
    }
    
    private void handleLateResponse(KUID nodeId, SocketAddress src, ResponseMessage msg) throws IOException {
        
        KUID messageId = msg.getMessageID();
        if (!messageId.verify(src)) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(ContactNode.toString(nodeId, src) + " sent us an unrequested response!");
            }
            return;
        }
        
        if (LOG.isTraceEnabled()) {
            if (msg instanceof PingResponse) {
                LOG.trace("Received a late Pong from " + ContactNode.toString(nodeId, src));
            } else if (msg instanceof FindNodeResponse) {
                LOG.trace("Received a late FindNode response from " + ContactNode.toString(nodeId, src));
            } else if (msg instanceof FindValueResponse) {
                LOG.trace("Received a late FindValue response from " + ContactNode.toString(nodeId, src));
            } else if (msg instanceof StoreResponse) {
                LOG.trace("Received a late Store response from " + ContactNode.toString(nodeId, src));
            }
        }
        networkStats.LATE_MESSAGES_COUNT.incrementStat();
        ContactNode node = new ContactNode(nodeId,src);
        context.getRouteTable().add(node, true);
    }
    
    /**
     * Returns the number of remaining Messages in
     * the output Queue
     */
    private int writeNext() throws IOException {
        synchronized (queueLock) {
            if (!outputQueue.isEmpty()) {
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
                }
            }
            return outputQueue.size();
        }
    }
    
    private ByteBuffer buffer = ByteBuffer.allocate(Receipt.MAX_PACKET_SIZE);
    
    private boolean readNext() throws IOException {
        SocketAddress src = channel.receive((ByteBuffer)buffer.clear());
        if (src != null) {
            int length = buffer.position();
            byte[] data = new byte[length];
            buffer.flip();
            buffer.get(data, 0, length);
            
            Message message = InputOutputUtils.deserialize(data);
            networkStats.RECEIVED_MESSAGES_COUNT.incrementStat();
            networkStats.RECEIVED_MESSAGES_SIZE.addData(data.length); // compressed size!
            
            Receipt receipt = null;
            
            if (message instanceof ResponseMessage) {
                synchronized (queueLock) {
                    receipt = (Receipt)messageMap.remove(message.getMessageID());
                }
                
                if (receipt != null) {
                    receipt.received();
                }
            }
            
            processMessage(receipt, src, message);
            return true;
        }
        return false;
    }
    
    private void processMessage(Receipt receipt, SocketAddress src, Message message) throws IOException {
        
        KUID nodeId = message.getNodeID();
        
        // Make sure we're not receiving messages from ourself.
        // The only exception are Pings/Pongs
        if (nodeId != null 
                && nodeId.equals(context.getLocalNodeID())
                && src.equals(context.getLocalSocketAddress())) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Received a message of type " + message.getClass().getName() 
                        + " from ourself " + ContactNode.toString(nodeId, src));
            }
            return;
        }
        
        if (message instanceof ResponseMessage) {
            
            ResponseMessage response = (ResponseMessage)message;
            
            // Check if we ever sent a such request...
            KUID messageId = response.getMessageID();
            if (!messageId.verify(src)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(ContactNode.toString(nodeId, src) + " sent us an unrequested response!");
                }
                
                // IMPORTANT: We cannot add a penalty here! A malicious
                // Node could fake its NodeID and SocketAddress, send us
                // responses and we'd penalize a honest Node!
                return;
            }
            
            // A null Receipt means it timed out and we're no
            // longer handling the response. But it's nice to know
            // the Node is still alive! Do something with the info!
            
            if (receipt != null) {
                defaultHandler.handleResponse(nodeId, src, response, receipt.time()); // BEFORE!
                if (receipt.getHandler() != defaultHandler) {
                    receipt.handleSuccess(nodeId, src, response);
                }
                
            } else {
                // Make sure a singe Node cannot monopolize our resources
                if (!filter.allow(src)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(ContactNode.toString(nodeId, src) + " refused");
                    }
                    networkStats.FILTERED_MESSAGES.incrementStat();
                    // return;
                }
                
                handleLateResponse(nodeId, src, response);
            }
        } else if (message instanceof RequestMessage) {
            
            RequestMessage request = (RequestMessage)message;
            
            // Make sure a singe Node cannot monopolize our resources
            if (!filter.allow(src)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(ContactNode.toString(nodeId, src) + " refused");
                }
                networkStats.FILTERED_MESSAGES.incrementStat();
                // return;
            }
            
            handleRequest(nodeId, src, request);
            defaultHandler.handleRequest(nodeId, src, request); // AFTER!
        } else if (LOG.isFatalEnabled()) {
            LOG.fatal(message + " is neither a Request nor a Response. Fix the code!");
        }
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
    
    public void run() {
        long lastCleanup = System.currentTimeMillis();
        
        networkStats.SENT_MESSAGES_COUNT.clearData();
        networkStats.RECEIVED_MESSAGES_COUNT.clearData();
        
        while(isRunning()) {
            try {
                
                selector.select(SLEEP);
                
                while(readNext() && isRunning());
                interestRead(true); // We're always interested in reading
                
                if ((System.currentTimeMillis()-lastCleanup) >= CLEANUP_INTERVAL) {
                    synchronized (queueLock) {
                        messageMap.cleanup();
                    }
                    lastCleanup = System.currentTimeMillis();
                }
                
                // TODO propper throtteling.
                int remaining = 0;
                for(int i = 0; i < 10 && isRunning(); i++) {
                    remaining = writeNext();
                    if (remaining == 0) {
                        break;
                    }
                }
                interestWrite(remaining > 0);
                    
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
