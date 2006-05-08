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
import de.kapsi.net.kademlia.handler.request.StatsRequestHandler;
import de.kapsi.net.kademlia.handler.request.StoreRequestHandler;
import de.kapsi.net.kademlia.io.Tag.Receipt;
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
import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public abstract class MessageDispatcher {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    private LinkedList outputQueue = new LinkedList();
    
    private ReceiptMap receiptMap = new ReceiptMap(512);
    
    private NetworkStatisticContainer networkStats;
    
    private Context context;
    
    private DefaultMessageHandler defaultHandler;
    private PingRequestHandler pingHandler;
    private LookupRequestHandler lookupHandler;
    private StoreRequestHandler storeHandler;
    private StatsRequestHandler statsHandler;
    
    private DatagramChannel channel;
    
    private ByteBuffer buffer;
    
    public MessageDispatcher(Context context) {
        this.context = context;
        
        networkStats = context.getNetworkStats();
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        lookupHandler = new LookupRequestHandler(context);
        storeHandler = new StoreRequestHandler(context);
        statsHandler = new StatsRequestHandler(context);
        
        buffer = ByteBuffer.allocate(Message.MAX_MESSAGE_SIZE);
    }
    
    public abstract void bind(SocketAddress addr) throws IOException;
    
    public abstract void start();
    
    public abstract void stop();
    
    public abstract boolean isRunning();
    
    public void setDatagramChannel(DatagramChannel channel) {
        this.channel = channel;
    }
    
    public DatagramChannel getDatagramChannel() {
        return channel;
    }
    
    public boolean isOpen() {
        DatagramChannel c = channel;
        return c != null && c.isOpen();
    }
    
    public SocketAddress getLocalSocketAddress() {
        DatagramChannel c = channel;
        if (c != null) {
            return c.socket().getLocalSocketAddress();
        }
        return null;
    }
    
    public boolean send(ContactNode node, ResponseMessage response) 
            throws IOException {
        return send(new Tag(node, response)); 
    }
    
    public boolean send(SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(dst, request, responseHandler));
    }
    
    public boolean send(KUID nodeId, SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(nodeId, dst, request, responseHandler));
    }
    
    public boolean send(ContactNode node, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(node, request, responseHandler));
    }
    
    private boolean send(Tag tag) throws IOException {
        if (!isOpen()) {
            throw new IOException("Channel is not open!");
        }
        
        KUID nodeId = tag.getNodeID();
        SocketAddress dst = tag.getSocketAddres();
        Message message = tag.getMessage();
        
        // Make sure we're not sending messages to ourself
        if (context.isLocalNodeID(nodeId)
                || context.isLocalAddress(dst)) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send message of type " + message.getClass().getName() 
                        + " to ourself " + ContactNode.toString(nodeId, dst));
            }
            return false;
        }
        
        synchronized(outputQueue) {
            outputQueue.add(tag);
            interestWrite(true);
        }
        
        return true;
    }
    
    public void handleRead() throws IOException {
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
            
            // Make sure we're not receiving messages from ourself.
            KUID nodeId = message.getNodeID();
            SocketAddress src = message.getSocketAddress();
            if (context.isLocalNodeID(nodeId)
                    || context.isLocalAddress(src)) {
                
                if (LOG.isErrorEnabled()) {
                    LOG.error("Received a message of type " + message.getClass().getName() 
                            + " from ourself " + ContactNode.toString(nodeId, src));
                }
                return;
            }
            
            if (message instanceof ResponseMessage) {
                Receipt receipt = null;
                
                synchronized(receiptMap) {
                    receipt = (Receipt)receiptMap.remove(message.getMessageID());
                }
                
                if (receipt != null) {
                    receipt.received();
                }
                
                processResponse(receipt, (ResponseMessage)message);
                
            } else if (message instanceof RequestMessage) {
                processRequest((RequestMessage)message);
                
            } else if (LOG.isFatalEnabled()) {
                LOG.fatal(message + " is neither a Request nor a Response. Fix the code!");
            }
        }
        
        // We're always interested in reading!
        interestRead(true);
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
    
    private void processResponse(Receipt receipt, ResponseMessage response) {
        process(new ResponseProcessor(receipt, response));
    }
    
    private void processRequest(RequestMessage request) {
        process(new RequestProcessor(request));
    }
    
    public void handleWrite() throws IOException {
        synchronized (outputQueue) {
            while(!outputQueue.isEmpty() && isRunning()) {
                Tag tag = (Tag)outputQueue.removeFirst();
                
                if (tag.send(channel)) {
                    // Wohoo! Message was sent!
                    networkStats.SENT_MESSAGES_COUNT.incrementStat();
                    networkStats.SENT_MESSAGES_SIZE.addData(tag.getSize()); // compressed size
                    
                    Receipt receipt = tag.getReceipt();
                    if (receipt != null) {
                        synchronized (receiptMap) {
                            receiptMap.add(receipt);
                        }
                    }
                } else {
                    // Dang! Re-Try next time!
                    outputQueue.addFirst(tag);
                    break;
                }
            }
            
            interestWrite(!outputQueue.isEmpty());
        }
    }
    
    protected void handleClenup() {
        process(new Runnable() {
            public void run() {
                synchronized (receiptMap) {
                    receiptMap.cleanup();
                }
            }
        });
    }
    
    protected abstract void interestRead(boolean on);
    
    protected abstract void interestWrite(boolean on);
    
    protected abstract void process(Runnable runnable);
    
    protected abstract boolean allow(Message message);
    
    private class ReceiptMap extends FixedSizeHashMap {
        
        private static final long serialVersionUID = -3084244582682726933L;

        public ReceiptMap(int maxSize) {
            super(maxSize);
        }

        public void add(Receipt receipt) {
            put(receipt.getMessageID(), receipt);
        }
        
        public void cleanup() {
            for(Iterator it = entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Receipt receipt = (Receipt)entry.getValue();
                
                if (receipt.timeout()) {
                    receipt.received();
                    it.remove();
                    networkStats.RECEIPTS_TIMEOUT.incrementStat();
                    
                    process(new TimeoutProcessor(receipt));
                }
            }
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            Receipt receipt = (Receipt)eldest.getValue();
            
            boolean timeout = receipt.timeout();
            if (super.removeEldestEntry(eldest) || timeout) {
                receipt.received();
                
                if(timeout) {
                    networkStats.RECEIPTS_TIMEOUT.incrementStat();
                } else {
                    networkStats.RECEIPTS_EVICTED.incrementStat();
                }
                
                process(new TimeoutProcessor(receipt));
                return true;
            }
            return false;
        }
    }
    
    private class ResponseProcessor implements Runnable {
        
        private Receipt receipt;
        private ResponseMessage response;
        
        private ResponseProcessor(Receipt receipt, ResponseMessage response) {
            this.receipt = receipt;
            this.response = response;
        }
        
        public void run() {
            // Check the QueryKey in the message ID to figure
            // out weather or not we have ever sent a Request
            // to that Host!
            
            if (response.verifyQueryKey()) {
                // A null Receipt means it timed out and we're no
                // longer handling the response. But it's nice to know
                // the Node is still alive! Do something with the info!
                
                if (receipt != null) {
                    
                    // The QueryKey check should catch all malicious
                    // and some buggy Nodes. Do some additional sanity
                    // checks to make sure the NodeID, IP:Port and 
                    // response type have the extpected values.
                    if (receipt.sanityCheck(response)) {
                        try {
                            processResponse();
                        } catch (IOException err) {
                            LOG.error("", err);
                        }
                    } else {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Response from " + response.getContactNode() 
                                    + " did not pass the sanity check");
                        }
                        
                        try {
                            processError();
                        } catch (IOException err) {
                            LOG.error("", err);
                        }
                    }
                } else {
                    processLateResponse();
                }
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(response.getContactNode() + " sent us an unrequested response!");
                }
            }
        }
        
        private void processResponse() throws IOException {
            defaultHandler.handleResponse(response, receipt.time());
            if (receipt.getResponseHandler() != defaultHandler) {
                receipt.getResponseHandler().handleResponse(response, receipt.time());
            }
        }
        
        // TODO: currently handled like a timeout but that's
        // not quite right.
        private void processError() throws IOException {
            KUID nodeId = receipt.getNodeID();
            SocketAddress dst = receipt.getSocketAddress();
            RequestMessage msg = receipt.getRequestMessage();
            long time = receipt.time();
            
            defaultHandler.handleTimeout(nodeId, dst, msg, time);
            if (receipt.getResponseHandler() != defaultHandler) {
                receipt.getResponseHandler().handleTimeout(nodeId, dst, msg, time);
            }
        }
        
        private void processLateResponse() {
            
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
                } else if (response instanceof StatsResponse) {
                    LOG.trace("Received a late Stats response from " + node);
                }
            }
            
            networkStats.LATE_MESSAGES_COUNT.incrementStat();
            context.getRouteTable().add(node, true);
        }
    }
    
    private class RequestProcessor implements Runnable {
        
        private RequestMessage request;
        
        private RequestProcessor(RequestMessage request) {
            this.request = request;
        }
        
        public void run() {
            // Make sure a singe Node cannot monopolize our resources
            if (!allow(request)) {
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
            } else if (request instanceof StatsRequest) {
                requestHandler = statsHandler;
            }
            
            if (requestHandler != null) {
                try {
                    requestHandler.handleRequest(request);
                    defaultHandler.handleRequest(request); // AFTER!
                } catch (Exception e) {
                    LOG.error("An exception occured during processing the request", e);
                }
            }
        }
    }
    
    private class TimeoutProcessor implements Runnable {
        
        private Receipt receipt;
        
        private TimeoutProcessor(Receipt receipt) {
            this.receipt = receipt;
        }
        
        public void run() {
            try {
                KUID nodeId = receipt.getNodeID();
                SocketAddress dst = receipt.getSocketAddress();
                RequestMessage msg = receipt.getRequestMessage();
                long time = receipt.time();
                
                defaultHandler.handleTimeout(nodeId, dst, msg, time);
                if (receipt.getResponseHandler() != defaultHandler) {
                    receipt.getResponseHandler().handleTimeout(nodeId, dst, msg, time);
                }
            } catch (Exception e) {
                LOG.error("ReceiptMap removeEldestEntry error: ", e);
            }
        }
    }
}
