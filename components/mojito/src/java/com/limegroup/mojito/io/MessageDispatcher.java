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
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.handler.DefaultMessageHandler;
import com.limegroup.mojito.handler.RequestHandler;
import com.limegroup.mojito.handler.ResponseHandler;
import com.limegroup.mojito.handler.request.LookupRequestHandler;
import com.limegroup.mojito.handler.request.PingRequestHandler;
import com.limegroup.mojito.handler.request.StatsRequestHandler;
import com.limegroup.mojito.handler.request.StoreRequestHandler;
import com.limegroup.mojito.io.Tag.Receipt;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.DHTSecureMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFormatException;
import com.limegroup.mojito.messages.PingRequest;
import com.limegroup.mojito.messages.PingResponse;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.messages.StatsRequest;
import com.limegroup.mojito.messages.StatsResponse;
import com.limegroup.mojito.messages.StoreRequest;
import com.limegroup.mojito.messages.StoreResponse;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.statistics.NetworkStatisticContainer;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.FixedSizeHashMap;

/**
 * MessageDispatcher is an abstract class that takes care of
 * all Mojito's communication needs.
 */
public abstract class MessageDispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    protected static final int INPUT_BUFFER_SIZE = 64 * 1024;
    protected static final int OUTPUT_BUFFER_SIZE = 64 * 1024;
    
    /** The maximum size of a serialized Message we can send */
    private static final int MAX_MESSAGE_SIZE
        = NetworkSettings.MAX_MESSAGE_SIZE.getValue();
    
    /** The recommended interval to call handleCleanup() */
    protected static final long CLEANUP = 100L;
    
    /** Queue of things we have to send */
    private LinkedList<Tag> outputQueue = new LinkedList<Tag>();
    
    /** Map of Messages (responses) we're awaiting */
    private ReceiptMap receiptMap = new ReceiptMap(512);
    
    private NetworkStatisticContainer networkStats;
    
    protected final Context context;
    
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
        
        buffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
    }
    
    public abstract void bind(SocketAddress address) throws IOException;
    
    public abstract void stop();
    
    public abstract boolean isRunning();
    
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
    
    public boolean send(Contact contact, ResponseMessage response) 
            throws IOException {
        return send(new Tag(contact, response)); 
    }
    
    public boolean send(SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(dst, request, responseHandler));
    }
    
    public boolean send(KUID nodeId, SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(nodeId, dst, request, responseHandler));
    }
    
    public boolean send(Contact contact, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(contact, request, responseHandler));
    }
    
    protected boolean send(Tag tag) throws IOException {
        if (!isOpen()) {
            throw new IOException("Channel is not open!");
        }
        
        KUID nodeId = tag.getNodeID();
        SocketAddress dst = tag.getSocketAddres();
        DHTMessage message = tag.getMessage();
        
        // Make sure we're not sending messages to ourself
        if (context.isLocalNodeID(nodeId)
                || context.isLocalAddress(dst)) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send message of type " + message.getClass().getName() 
                        + " to ourself " + ContactUtils.toString(nodeId, dst));
            }
            tag.handleError(new IOException("Cannot send message to yourself"));
            return false;
        }
        
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            if (LOG.isErrorEnabled()) {
                LOG.error("The IP/Port of " + ContactUtils.toString(nodeId, dst) + " is not valid");
            }
            
            tag.handleError(new IllegalSocketAddressException("Invalid IP/Port: " + ContactUtils.toString(nodeId, dst)));
            return false;
        }
        
        // Serialize the Message
        ByteBuffer data = serialize(dst, message);
        int size = data.remaining();
        if (size >= MAX_MESSAGE_SIZE) {
            IOException iox = new IOException("Message (" + message.getClass().getName()  + ") is too large: " 
                    + size + " >= " + MAX_MESSAGE_SIZE);
            //tag.handleError(iox);
            //return false;
            throw iox;
        }
        
        tag.setData(data);
        return enqueueOutput(tag);
    }
    
    /**
     * Enqueues Tag to the Output queue
     */
    protected boolean enqueueOutput(Tag tag) {
        synchronized(outputQueue) {
            outputQueue.add(tag);
            interestWrite(true);
        }
        return true;
    }
    
    /**
     * Reads all available Message from Network and processes them.
     */
    public void handleRead() throws IOException {
        while(isRunning()) {
            DHTMessage message = null;
            try {
                message = readMessage();
            } catch (MessageFormatException err) {
                LOG.error("Message Format Exception: ", err);
                continue;
            }
            
            if (message == null) {
                break;
            }
            
            handleMessage(message);
        }
        
        // We're always interested in reading!
        interestRead(true);
    }
    
    /**
     * A helper method to serialize DHTMessage(s)
     */
    protected final ByteBuffer serialize(SocketAddress dst, DHTMessage message) throws IOException {
        return context.getMessageFactory().writeMessage(dst, message);
    }
    
    /**
     * A helper method to deserialize DHTMessage(s)
     */
    protected final DHTMessage deserialize(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        return context.getMessageFactory().createMessage(src, data);
    }
    
    /**
     * The raw read-method.
     */
    protected SocketAddress read(ByteBuffer b) throws IOException {
        return channel.receive(b);
    }
    
    /**
     * Reads and returns a single DHTMessage from Network or null
     * if no Messages were in the input queue.
     */
    private DHTMessage readMessage() throws MessageFormatException, IOException {
        SocketAddress src = read((ByteBuffer)buffer.clear());
        if (src != null) {
            buffer.flip();
            int length = buffer.remaining();

            ByteBuffer data = ByteBuffer.allocate(length);
            data.put(buffer);
            data.rewind();
            
            DHTMessage message = deserialize(src, data/*.asReadOnlyBuffer()*/);
            networkStats.RECEIVED_MESSAGES_COUNT.incrementStat();
            networkStats.RECEIVED_MESSAGES_SIZE.addData(length);
            return message;
        }
        return null;
    }
    
    /**
     * Checks if we have ever sent a Request to the Node that
     * sent us the Response.
     */
    private boolean verifyQueryKey(ResponseMessage response) {
        KUID messageId = response.getMessageID();
        Contact node = response.getContact();
        return messageId.verifyQueryKey(node.getSocketAddress());
    }
    
    /**
     * Handles a DHTMessage as read from Network
     */
    protected void handleMessage(DHTMessage message) throws IOException {
        // Make sure we're not receiving messages from ourself.
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        SocketAddress src = node.getSocketAddress();
        
        if (context.isLocalNodeID(nodeId)
                || context.isLocalAddress(src)) {
            
            if (LOG.isErrorEnabled()) {
                LOG.error("Received a message of type " + message.getClass().getName() 
                        + " from ourself " + ContactUtils.toString(nodeId, src));
            }
            return;
        }
        
        if (!NetworkUtils.isValidSocketAddress(src)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(ContactUtils.toString(nodeId, src) + " has an invalid IP/Port");
            }
            return;
        }
        
        if (!accept(message)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + ContactUtils.toString(nodeId, src));
            }
            return;
        }
        
        if (message instanceof ResponseMessage) {
            ResponseMessage response = (ResponseMessage)message;
            
            // Check the QueryKey in the message ID to figure
            // out whether or not we have ever sent a Request
            // to that Host!
            if (!verifyQueryKey(response)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(response.getContact() + " sent us an unrequested response!");
                }
                return;
            }
             
            Receipt receipt = null;
            
            synchronized(receiptMap) {
                receipt = receiptMap.get(message.getMessageID());
                
                if (receipt != null) {
                    receipt.received();
            
                    // The QueryKey check should catch all malicious
                    // and some buggy Nodes. Do some additional sanity
                    // checks to make sure the NodeID, IP:Port and 
                    // response type have the extpected values.
                    if (!receipt.sanityCheck(response)) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Response from " + response.getContact() 
                                    + " did not pass the sanity check");
                        }
                        return;
                    }
                    
                    // Set the Round Trip Time (RTT)
                    message.getContact().setRoundTripTime(receipt.time());
                    
                    // OK, all checks passed. We can remove the receipt now!
                    receiptMap.remove(message.getMessageID());
                }
            }
            
            processResponse(receipt, response);
            
        } else if (message instanceof RequestMessage) {
            processRequest((RequestMessage)message);
            
        } else if (LOG.isFatalEnabled()) {
            LOG.fatal(message + " is neither a Request nor a Response. Fix the code!");
        }
    }
    
    /**
     * Starts a new ResponseProcessor
     */
    private void processResponse(Receipt receipt, ResponseMessage response) {
        ResponseProcessor processor = new ResponseProcessor(receipt, response);
        if (response instanceof DHTSecureMessage) {
            if (context.getMasterKey() != null) {
                verify((DHTSecureMessage)response, processor);
            } else if (LOG.isInfoEnabled()) {
                LOG.info("Dropping secure response " 
                        + response + " because PublicKey is not set");
            }
        } else {
            process(processor);
        }
    }
    
    /**
     * Starts a new RequestProcessor
     */
    private void processRequest(RequestMessage request) {
        RequestProcessor processor = new RequestProcessor(request);
        if (request instanceof DHTSecureMessage) {
            if (context.getMasterKey() != null) {
                verify((DHTSecureMessage)request, processor);
            } else if (LOG.isInfoEnabled()) {
                LOG.info("Dropping secure request " 
                        + request + " because PublicKey is not set");
            }
        } else {
            process(processor);
        }
    }
    
    /**
     * Writes all Messages (if possible) from the output
     * queue to the Network and returns whether or not some
     * Messages were left in the output queue.
     */
    public boolean handleWrite() throws IOException {
        synchronized (outputQueue) {
            while(!outputQueue.isEmpty() && isRunning()) {
                Tag tag = outputQueue.removeFirst();
                
                try {
                    SocketAddress dst = tag.getSocketAddres();
                    ByteBuffer data = tag.getData();
                    assert (data != null);
                    
                    if (send(channel, dst, data)) {
                        // Wohoo! Message was sent!
                        registerInput(tag);
                    } else {
                        // Dang! Re-Try next time!
                        outputQueue.addFirst(tag);
                        break;
                    }
                } catch (SocketException err) {
                    LOG.error("Failed to send a message due to a SocketException", err);
                    tag.handleError(err);
                }
            }
            
            interestWrite(!outputQueue.isEmpty());
            return !outputQueue.isEmpty();
        }
    }
    
    /**
     * The actual send method. Returns true if the data was
     * sent or false if there was insufficient space in the
     * output buffer (that means you'll have to re-try it later
     * again).
     */
    protected boolean send(DatagramChannel channel, 
            SocketAddress dst, ByteBuffer data) throws IOException {
        return channel.send(data, dst) > 0;
    }
    
    /**
     * Called right after a Message has been sent to register
     * its ResponseHandler (if it's a RequestMessage).
     */
    protected void registerInput(Tag tag) {
        Receipt receipt = tag.sent();
        if (receipt != null) {
            synchronized (receiptMap) {
                receiptMap.add(receipt);
            }
        }
        
        networkStats.SENT_MESSAGES_COUNT.incrementStat();
        networkStats.SENT_MESSAGES_SIZE.addData(tag.getSize());
    }
    
    /**
     * Starts a cleanup process
     */
    protected void handleCleanup() {
        process(new Runnable() {
            public void run() {
                synchronized (receiptMap) {
                    receiptMap.cleanup();
                }
            }
        });
    }
    
    /** Called to indicate an interest in reading */
    protected abstract void interestRead(boolean on);
    
    /** Called to indicate an interest in writing */
    protected abstract void interestWrite(boolean on);
    
    /** Called to process a Task */
    protected abstract void process(Runnable runnable);
    
    /** Called to verify a SecureMessage */
    protected abstract void verify(SecureMessage secureMessage, SecureMessageCallback smc);
    
    /** Called to check whether or not the Message should be processed */
    protected boolean allow(DHTMessage message) {
        return true;
    }
    
    /** */
    protected boolean accept(DHTMessage message) {
        return true;
    }
    
    /**
     * Clears the output queue and receipt map
     */
    protected void clear() {
        synchronized (outputQueue) {
            outputQueue.clear();
        }
        
        synchronized (receiptMap) {
            receiptMap.clear();
        }
    }
    
    /**
     * A map of MessageID -> Receipts
     */
    private class ReceiptMap extends FixedSizeHashMap<KUID, Receipt> {
        
        private static final long serialVersionUID = -3084244582682726933L;

        public ReceiptMap(int maxSize) {
            super(maxSize);
        }

        public void add(Receipt receipt) {
            put(receipt.getMessageID(), receipt);
        }
        
        public void cleanup() {
            for(Iterator<Receipt> it = values().iterator(); it.hasNext(); ) {
                Receipt receipt = it.next();
                
                if (receipt.timeout()) {
                    receipt.received();
                    it.remove();
                    networkStats.RECEIPTS_TIMEOUT.incrementStat();
                    
                    process(new TimeoutProcessor(receipt));
                }
            }
        }
        
        protected boolean removeEldestEntry(Map.Entry<KUID, Receipt> eldest) {
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
    
    /**
     * An implementation of Runnable to handle Response Messages.
     */
    private class ResponseProcessor implements Runnable, SecureMessageCallback {
        
        private Receipt receipt;
        private ResponseMessage response;
        
        private ResponseProcessor(Receipt receipt, ResponseMessage response) {
            this.receipt = receipt;
            this.response = response;
        }
        
        public void handleSecureMessage(SecureMessage sm, boolean passed) {
            if (passed) {
                process(this);
            } else if (LOG.isErrorEnabled()) {
                LOG.error(response.getContact() 
                        + " send us a signed Response but the signatures do not match!");
            }
        }
        
        public void run() {
            if (receipt != null) {
                processResponse();
            } else {
                processLateResponse();
            }
        }
        
        private void processResponse() {
            try {
                defaultHandler.handleResponse(response, receipt.time());
                if (receipt.getResponseHandler() != defaultHandler) {
                    receipt.getResponseHandler().addTime(receipt.time());
                    receipt.getResponseHandler().handleResponse(response, receipt.time());
                }
            } catch (IOException err) {
                LOG.error("An error occured dusring processing the response", err);
            }
        }
        
        private void processLateResponse() {
            
            Contact node = response.getContact();
            
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
            context.getRouteTable().add(node);
        }
    }
    
    /**
     * An implementation of Runnable to handle Request Messages.
     */
    private class RequestProcessor implements Runnable, SecureMessageCallback {
        
        private RequestMessage request;
        
        private RequestProcessor(RequestMessage request) {
            this.request = request;
        }
        
        public void handleSecureMessage(SecureMessage sm, boolean passed) {
            if (passed) {
                process(this);
            } else if (LOG.isErrorEnabled()) {
                LOG.error(request.getContact() 
                        + " send us a signed Request but the signatures do not match!");
            }
        }
        
        public void run() {
            // Make sure a singe Node cannot monopolize our resources
            if (!allow(request)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(request.getContact() + " refused");
                }
                networkStats.FILTERED_MESSAGES.incrementStat();
                return;
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
    
    /**
     * An implementation of Runnable to handle Timeouts.
     */
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
                    receipt.getResponseHandler().addTime(time);
                    receipt.getResponseHandler().handleTimeout(nodeId, dst, msg, time);
                }
            } catch (Exception e) {
                LOG.error("ReceiptMap removeEldestEntry error: ", e);
            }
        }
    }
}
