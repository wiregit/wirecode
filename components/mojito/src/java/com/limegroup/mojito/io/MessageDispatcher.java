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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.exceptions.IllegalSocketAddressException;
import com.limegroup.mojito.handler.DefaultMessageHandler;
import com.limegroup.mojito.handler.RequestHandler;
import com.limegroup.mojito.handler.ResponseHandler;
import com.limegroup.mojito.handler.request.FindNodeRequestHandler;
import com.limegroup.mojito.handler.request.FindValueRequestHandler;
import com.limegroup.mojito.handler.request.PingRequestHandler;
import com.limegroup.mojito.handler.request.StatsRequestHandler;
import com.limegroup.mojito.handler.request.StoreRequestHandler;
import com.limegroup.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import com.limegroup.mojito.io.Tag.Receipt;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.DHTSecureMessage;
import com.limegroup.mojito.messages.FindNodeRequest;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.messages.FindValueRequest;
import com.limegroup.mojito.messages.FindValueResponse;
import com.limegroup.mojito.messages.MessageFormatException;
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
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.ContactUtils;
import com.limegroup.mojito.util.FixedSizeHashMap;
import com.limegroup.mojito.util.MessageUtils;

/**
 * MessageDispatcher is an abstract class that takes care of
 * all Mojito's communication needs.
 */
public abstract class MessageDispatcher {
    
    private static final Log LOG = LogFactory.getLog(MessageDispatcher.class);
    
    protected static final int INPUT_BUFFER_SIZE 
        = NetworkSettings.INPUT_BUFFER_SIZE.getValue();
    
    protected static final int OUTPUT_BUFFER_SIZE 
        = NetworkSettings.OUTPUT_BUFFER_SIZE.getValue();
    
    /** The maximum size of a serialized Message we can send */
    private static final int MAX_MESSAGE_SIZE
        = NetworkSettings.MAX_MESSAGE_SIZE.getValue();
    
    /** Queue of things we have to send */
    private List<Tag> outputQueue = new LinkedList<Tag>();
    
    /** A lock for the outputQueue */
    private Object outputQueueLock = new Object();
    
    /** Map of Messages (responses) we're awaiting */
    private ReceiptMap receiptMap = new ReceiptMap(512);
    
    /** A lock for the ReceiptMap */
    private Object receiptMapLock = new Object();
    
    /** 
     * The CleanupTask goes periodically through the ReceiptMap 
     * and prunes out ResponseHandlers that have timed-out.
     */
    private CleanupTask cleanupTask;
    
    protected final Context context;
    
    private DefaultMessageHandler defaultHandler;
    private PingRequestHandler pingHandler;
    private FindNodeRequestHandler findNodeHandler;
    private FindValueRequestHandler findValueHandler;
    private StoreRequestHandler storeHandler;
    private StatsRequestHandler statsHandler;
    
    private ByteBuffer inputBuffer;
    
    private List<MessageDispatcherListener> listeners;
    
    private Filter filter;
    
    public MessageDispatcher(Context context) {
        this.context = context;
        
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        findNodeHandler = new FindNodeRequestHandler(context);
        findValueHandler = new FindValueRequestHandler(context, findNodeHandler);
        storeHandler = new StoreRequestHandler(context);
        statsHandler = new StatsRequestHandler(context);
        
        inputBuffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE);
    }
    
    /**
     * Adds a MessageDispatcherListener.
     * 
     * Implementation Note: The listener(s) is not called from a 
     * seperate event Thread! That means processor intensive tasks
     * that are performed straight in the listener(s) can slowdown 
     * the processing throughput significantly. Offload intensive
     * tasks to seperate Threads in necessary!
     * 
     * @param l The MessageDispatcherListener instance to add
     */
    public synchronized void addMessageDispatcherListener(MessageDispatcherListener l) {
        if (l == null) {
            throw new NullPointerException("MessageDispatcherListener is null");
        }
        
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<MessageDispatcherListener>();
        }
        
        listeners.add(l);
    }
    
    /**
     * Removes a MessageDispatcherListener
     * 
     * @param l The MessageDispatcherListener instance to remove
     */
    public synchronized void removeMessageDispatcherListener(MessageDispatcherListener l) {
        if (l == null) {
            throw new NullPointerException("MessageDispatcherListener is null");
        }
        
        if (listeners != null) {
            listeners.remove(l);
        }
    }
    
    /**
     * Sets the DHTMessage filter. Use null to unset the filter.
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }
    
    /**
     * Binds the DatagramSocket to the given SocketAddress
     */
    public abstract void bind(SocketAddress address) throws IOException;
    
    /**
     * Starts the MessageDispatcher
     */
    public void start() {
        // Start the CleanupTask
    	synchronized (receiptMapLock) {
            if (cleanupTask == null) {
                cleanupTask = new CleanupTask();
                cleanupTask.start();
            }
        }
    }
    
    /**
     * Stops the MessageDispatcher
     */
    public void stop() {
        // Stop the CleanupTask
    	synchronized (receiptMapLock) {
            if (cleanupTask != null) {
                cleanupTask.stop();
                cleanupTask = null;
            }
        }
    }
    
    /**
     * Returns whether or not the MessageDispatcher is running
     */
    public abstract boolean isRunning();
    
    /**
     * Returns whether or not the DatagramChannel is open
     */
    public abstract boolean isOpen();
    
    /**
     * Sends a ResponseMessage to the given Contact
     */
    public boolean send(Contact contact, ResponseMessage response) 
            throws IOException {
        return send(new Tag(contact, response)); 
    }
    
    /**
     * Sends a RequestMessage to the given SocketAddress and registers
     * a ResponseHandler
     */
    public boolean send(SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(dst, request, responseHandler));
    }
    
    /**
     * Sends a RequestMessage to the given SocketAddress and registers
     * a ResponseHandler
     */
    public boolean send(KUID nodeId, SocketAddress dst, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(nodeId, dst, request, responseHandler));
    }
    
    /**
     * Sends a RequestMessage to the given Contact and registers
     * a ResponseHandler
     */
    public boolean send(Contact contact, RequestMessage request, 
            ResponseHandler responseHandler) throws IOException {
        return send(new Tag(contact, request, responseHandler));
    }
    
    /**
     * The actual send method.
     */
    protected boolean send(Tag tag) throws IOException {
        if (!isOpen()) {
            throw new IOException("Channel is not open!");
        }
        
        KUID nodeId = tag.getNodeID();
        SocketAddress dst = tag.getSocketAddress();
        DHTMessage message = tag.getMessage();
        
        // Make sure we're not sending messages to ourself
        if (context.isLocalContactAddress(dst)) {
            // This can happen when the RouteTable was
            // initialized with Nodes from an external
            // source like a file. It's not really an
            // error because the Node's ID is different
            // but there's no point in sending the Msg.
            String msg = "Cannot send Message of type "
                + message.getClass().getName()
                + " to " + ContactUtils.toString(nodeId, dst)
                + " because it has the same contact address as the local Node "
                + context.getLocalNode() + " has";
            
            if (LOG.isInfoEnabled()) {
                LOG.info(msg);
            }
            
            process(new ErrorProcessor(tag, new IOException(msg)));
            return false;
        }
        
        // Like above. It makes no sense to send messages to a
        // Node that has our local Node ID but we have to permit
        // this case for ID collision test pings
        if (context.isLocalNodeID(nodeId) 
                && !MessageUtils.isCollisionPingRequest(
                        context.getLocalNodeID(), message)) {
            
            String msg = "Cannot send Message of type " 
                + message.getClass().getName() 
                + " to " + ContactUtils.toString(nodeId, dst) 
                + " which is equal to our local Node " 
                + context.getLocalNode();
            
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IOException(msg)));
            return false;
        }
        
        // Check if it's a valid destination address
        if (!NetworkUtils.isValidSocketAddress(dst)) {
            String msg = "Invalid IP:Port " + ContactUtils.toString(nodeId, dst);
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IllegalSocketAddressException(msg)));
            return false;
        }
        
        // And make sure we're not sending messages to private
        // IPs if it's not permitted. Two Nodes behind the same 
        // NAT using private IPs to communicate with each other
        // would screw up a few things.
        if (NetworkUtils.isPrivateAddress(dst)) {
            String msg = "Private IP:Port " + ContactUtils.toString(nodeId, dst);
            if (LOG.isErrorEnabled()) {
                LOG.error(msg);
            }
            
            process(new ErrorProcessor(tag, new IllegalSocketAddressException(msg)));
            return false;
        }
        
        // Serialize the Message
        ByteBuffer data = serialize(dst, message);
        int size = data.remaining();
        if (size >= MAX_MESSAGE_SIZE) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Message (" + message.getClass().getName()  + ") is too large: " 
                    + size + " >= " + MAX_MESSAGE_SIZE);
            }
        }
        
        tag.setData(data);
        return enqueueOutput(tag);
    }
    
    /**
     * Enqueues Tag to the Output queue
     */
    protected boolean enqueueOutput(Tag tag) {
        // The purpose of this locking is to set
        // interest write properly.
        synchronized(outputQueueLock) {
            outputQueue.add(tag);
            interestWrite(true);
            return true;
        }
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
    protected ByteBuffer serialize(SocketAddress dst, DHTMessage message) throws IOException {
        return context.getMessageFactory().writeMessage(dst, message);
    }
    
    /**
     * A helper method to deserialize DHTMessage(s)
     */
    protected DHTMessage deserialize(SocketAddress src, ByteBuffer data) 
            throws MessageFormatException, IOException {
        return context.getMessageFactory().createMessage(src, data);
    }
    
    /**
     * The raw read-method.
     */
    protected abstract SocketAddress receive(ByteBuffer dst) throws IOException;
    
    /**
     * Reads and returns a single DHTMessage from Network or null
     * if no Messages were in the input queue.
     */
    private DHTMessage readMessage() throws MessageFormatException, IOException {
        SocketAddress src = receive((ByteBuffer)inputBuffer.clear());
        if (src != null) {
            inputBuffer.flip();
            int length = inputBuffer.remaining();

            ByteBuffer data = ByteBuffer.allocate(length);
            data.put(inputBuffer);
            data.rewind();
            
            DHTMessage message = deserialize(src, data/*.asReadOnlyBuffer()*/);
            return message;
        }
        return null;
    }
    
    /**
     * Checks if we have ever sent a Request to the Node that
     * sent us the Response.
     */
    private boolean verifyQueryKey(ResponseMessage response) {
        MessageID messageId = response.getMessageID();
        Contact node = response.getContact();
        return messageId.verifyQueryKey(node.getContactAddress());
    }
    
    /**
     * Handles a DHTMessage as read from Network
     */
    protected void handleMessage(DHTMessage message) throws IOException {
        // Make sure we're not receiving messages from ourself.
        Contact node = message.getContact();
        KUID nodeId = node.getNodeID();
        SocketAddress src = node.getContactAddress();
        
        if (context.isLocalContactAddress(src)
                || (context.isLocalNodeID(nodeId) 
                        && !(message instanceof PingResponse))) {
            
            if (LOG.isErrorEnabled()) {
                String msg = "Received a message of type " 
                    + message.getClass().getName() 
                    + " from " + node 
                    + " which is equal to our local Node " 
                    + context.getLocalNode();
                
                LOG.error(msg);
            }
            
            return;
        }
        
        if (!NetworkUtils.isValidSocketAddress(src)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " has an invalid IP:Port");
            }
            return;
        }
        
        // Make sure we're not mixing IPv4 and IPv6 addresses.
        // See RouteTableImpl.add() for more info!
        if (!ContactUtils.isSameAddressSpace(context.getLocalNode(), node)) {
            
            // Log as ERROR so that we're not missing this
            if (LOG.isErrorEnabled()) {
                LOG.error(node + " is from a different IP address space than local Node");
            }
            return;
        }
        
        fireMessageReceived(message);
        
        if (!accept(message)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + node);
            }
            
            fireMessageFiltered(message);
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
            
            synchronized(receiptMapLock) {
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
            if (context.getLocalNode().isFirewalled()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Local Node is firewalled, dropping " + message);
                }
            } else {
                processRequest((RequestMessage)message);
            }
            
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
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Dropping secure response " 
                            + response + " because MasterKey is not set");
                }
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
            } else {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Dropping secure request " 
                            + request + " because MasterKey is not set");
                }
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
        
        synchronized (outputQueueLock) {
            Tag tag = null;
            while (!outputQueue.isEmpty()) {
                tag = outputQueue.get(0);

                if (tag.isCancelled()) {
                    outputQueue.remove(0);
                    continue;
                }

                try {
                    SocketAddress dst = tag.getSocketAddress();
                    ByteBuffer data = tag.getData();
                    
                    if (send(dst, data)) {
                        // Wohoo! Message was sent!
                        outputQueue.remove(0);
                        registerInput(tag);
                    } else {
                        // Dang! Re-Try next time!
                        break;
                    }
                } catch (IOException err) {
                    LOG.error("IOException", err);
                    outputQueue.remove(0);
                    process(new ErrorProcessor(tag, err));
                }
            }

            boolean isEmpty = outputQueue.isEmpty();
            interestWrite(!isEmpty);
            return !isEmpty;
        }
    }
    
    /**
     * The actual send method. Returns true if the data was
     * sent or false if there was insufficient space in the
     * output buffer (that means you'll have to re-try it later
     * again).
     */
    protected abstract boolean send(SocketAddress dst, ByteBuffer data) throws IOException;
    
    /**
     * Called right after a Message has been sent to register
     * its ResponseHandler (if it's a RequestMessage).
     */
    protected void registerInput(Tag tag) {
        Receipt receipt = tag.sent();
        if (receipt != null) {
            synchronized (receiptMapLock) {
                receiptMap.add(receipt);
                receiptMapLock.notifyAll();
            }
        }
        
        fireMessageSend(tag.getNodeID(), tag.getSocketAddress(), tag.getMessage());
    }
    
    /** Called to indicate an interest in reading */
    protected abstract void interestRead(boolean on);
    
    /** Called to indicate an interest in writing */
    protected abstract void interestWrite(boolean on);
    
    /** Called to process a Task */
    protected abstract void process(Runnable runnable);
    
    /** Called to verify a SecureMessage */
    protected abstract void verify(SecureMessage secureMessage, SecureMessageCallback smc);
    
    /**
     * This method is called from the MessageDispatcher Thread to
     * determinate whether or not the DHTMessage should be accepted.
     * As it's running on the MessageDispatcher Thread it shouldn't
     * block for too long. Use the allow() method for heavyweight
     * checks!
     */
    protected boolean accept(DHTMessage message) {
        return true;
    }
    
    /** 
     * This method is called to determinate whether or not the
     * DHTMessage should be processed. 
     */
    protected boolean allow(DHTMessage message) {
        Filter f = filter;
        if (f != null) {
            return f.allow(message);
        }
        return true;
    }
    
    /**
     * Clears the output queue and receipt map
     */
    protected void clear() {
        synchronized(outputQueueLock) {
            outputQueue.clear();
        }
        
        synchronized (receiptMapLock) {
            receiptMap.clear();
        }
    }
    
    /**
     * Cleans up the receipt mapping. Meant for
     * internal use only! DO NOT CALL!
     */
    protected void cleanup() {
        synchronized (receiptMapLock) {
            receiptMap.cleanup();
        }
    }
    
    protected void fireMessageSend(KUID nodeId, SocketAddress dst, DHTMessage message) {
        fireMessageDispatcherEvent(nodeId, dst, message, EventType.MESSAGE_SEND);
    }
    
    protected void fireMessageReceived(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.MESSAGE_RECEIVED);
    }
    
    protected void fireMessageFiltered(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.MESSAGE_FILTERED);
    }
    
    protected void fireLateResponse(DHTMessage message) {
        fireMessageDispatcherEvent(null, null, message, EventType.LATE_RESPONSE);
    }
    
    protected void fireReceiptTimeout(Receipt receipt) {
        fireMessageDispatcherEvent(receipt.getNodeID(), receipt.getSocketAddress(), 
                receipt.getRequestMessage(), EventType.RECEIPT_TIMEOUT);
    }
    
    protected void fireReceiptEvicted(Receipt receipt) {
        fireMessageDispatcherEvent(receipt.getNodeID(), receipt.getSocketAddress(), 
                receipt.getRequestMessage(), EventType.RECEIPT_EVICTED);
    }
    
    protected void fireMessageDispatcherEvent(KUID nodeId, SocketAddress dst, 
            DHTMessage message, EventType type) {
        
        List<MessageDispatcherListener> list = listeners;
        if (list != null) {
            Iterator<MessageDispatcherListener> it = list.iterator();
            if (it.hasNext()) {
                MessageDispatcherEvent evt 
                    = new MessageDispatcherEvent(this, nodeId, dst, message, type);
                while(it.hasNext()) {
                    it.next().handleMessageDispatcherEvent(evt);
                }
            }
        }
    }
    
    /**
     * A map of MessageID -> Receipts
     */
    @SuppressWarnings("serial")
    private class ReceiptMap extends FixedSizeHashMap<MessageID, Receipt> {
        
        public ReceiptMap(int maxSize) {
            super(maxSize);
        }

        public void add(Receipt receipt) {
            put(receipt.getMessageID(), receipt);
        }
        
        /**
         * Cleans up the Map and kicks off Ticks. Meant to be
         * called by a scheduled task.
         */
        public void cleanup() {
            for(Iterator<Receipt> it = values().iterator(); it.hasNext(); ) {
                Receipt receipt = it.next();
                
                if (receipt.isCancelled()) {
                    // The user cancelled the Future
                    it.remove();
                	
                } else if (receipt.timeout()) {
                    receipt.received();
                    it.remove();
                    
                    fireReceiptTimeout(receipt);
                    process(new TimeoutProcessor(receipt));
                } else {
                    process(new TickProcessor(receipt));
                }
            }
        }
        
        protected boolean removeEldestEntry(Map.Entry<MessageID, Receipt> eldest) {
            Receipt receipt = (Receipt)eldest.getValue();
            
            boolean timeout = receipt.timeout();
            if (super.removeEldestEntry(eldest) || timeout) {
                receipt.received();
                
                if(timeout) {
                    fireReceiptTimeout(receipt);
                } else {
                    fireReceiptEvicted(receipt);
                }
                
                process(new TimeoutProcessor(receipt));
                return true;
            }
            return false;
        }
    }
    
    /**
     * The CleanupTask calls is periodical intervals 
     * ReceiptMap's cleanup() methods
     */
    private class CleanupTask implements Runnable {
    	
    	private Thread thread = null;
    	private volatile boolean running = true;
    	
        public void run() {
            long sleep = NetworkSettings.CLEANUP_RECEIPTS_INTERVAL.getValue();
        	
            try {
                while(running) {
                    synchronized(receiptMapLock) {
                        if (receiptMap.isEmpty()) {
                            receiptMapLock.wait();
                        }
                        
                        receiptMap.cleanup();
                    }
                    
                    Thread.sleep(sleep);
                }
            } catch (InterruptedException err) {
                if (running) {
                    LOG.error("InterruptedException", err);
                }
            }
        }
        
        public void start() {
            running = true;
            
            if (thread == null) {
                thread = context.getThreadFactory().newThread(this);
                thread.setName(context.getName() + "-CleanupTask");
                thread.setDaemon(true);
                thread.start();
            }
        }
        
        public void stop() {
            running = false;
            
            if (thread != null) {
            	thread.interrupt();
            	thread = null;
            }
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
        
        /**
         * Processes a regular response
         */
        private void processResponse() {
            try {
                defaultHandler.handleResponse(response, receipt.time());
                if (receipt.getResponseHandler() != defaultHandler) {
                    receipt.getResponseHandler().handleResponse(response, receipt.time());
                }
            } catch (IOException e) {
                receipt.handleError(e);
                LOG.error("An error occured dusring processing the response", e);
            }
        }
        
        /**
         * A late response is a response that arrived after a timeout.
         * We rely on the fact that MessageIDs are tagged with a QueryKey
         * so that we can still figure out if we've ever send a request
         * to the remote Node.
         * 
         * The fact that the remote Node respond is a valuable information
         * and we'll use a higher timeout next time.
         */
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
            
            fireLateResponse(response);
            
            if (!node.isFirewalled()) {
                context.getRouteTable().add(node); // update
            }
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
                
                fireMessageFiltered(request);
                return;
            }
            
            RequestHandler requestHandler = null;
            
            if (request instanceof PingRequest) {
                requestHandler = pingHandler;
            } else if (request instanceof FindNodeRequest) {
                requestHandler = findNodeHandler;
            } else if (request instanceof FindValueRequest) {
                requestHandler = findValueHandler;
            } else if (request instanceof StoreRequest) {
                requestHandler = storeHandler;
            } else if (request instanceof StatsRequest) {
                requestHandler = statsHandler;
            }
            
            if (requestHandler != null) {
                try {
                    requestHandler.handleRequest(request);
                    defaultHandler.handleRequest(request); // AFTER!
                } catch (IOException e) {
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
                    receipt.getResponseHandler().handleTimeout(nodeId, dst, msg, time);
                }
            } catch (IOException e) {
                receipt.handleError(e);
                LOG.error("ReceiptMap removeEldestEntry error: ", e);
            }
        }
    }
    
    /**
     * An implementation of Runnable to handle Ticks.
     */
    private class TickProcessor implements Runnable {
        
        private Receipt receipt;
        
        private TickProcessor(Receipt receipt) {
            this.receipt = receipt;
        }
        
        public void run() {
            receipt.handleTick();
        }
    }
    
    /**
     * An implementation of Runnable to handle Errors.
     */
    private class ErrorProcessor implements Runnable {
        
        private Tag tag;
        
        private IOException exception;
        
        private ErrorProcessor(Tag tag, IOException exception) {
            this.tag = tag;
            this.exception = exception;
        }
        
        public void run() {
            tag.handleError(exception);
        }
    }
    
    /**
     * The MessageDispatcherListener is called for every send or 
     * received Message.
     */
    public static interface MessageDispatcherListener {
        
        /**
         * Invoked when an event occurs
         * 
         * @param evt The event that occured
         */
        public void handleMessageDispatcherEvent(MessageDispatcherEvent evt);
    }
    
    /**
     * MessageDispatcherEvent are created and fired for various MessageDispatcher events.
     */
    public static class MessageDispatcherEvent {
        
        public static enum EventType {
            
            /** Fired if a DHTMessage was send */
            MESSAGE_SEND,
            
            /** Fired if a DHTMessage was received */
            MESSAGE_RECEIVED,
            
            /** Fired if a DHTMessage filtered */
            MESSAGE_FILTERED,
            
            /** Fired if a request timed out */
            RECEIPT_TIMEOUT,
            
            /** Fired if a request was evicted */
            RECEIPT_EVICTED,
            
            /** Fired if a late response was received */
            LATE_RESPONSE;
        }
        
        private MessageDispatcher messageDispatcher;
        
        private KUID nodeId;
        
        private SocketAddress dst;
        
        private DHTMessage message;
        
        private EventType type;
        
        public MessageDispatcherEvent(MessageDispatcher messageDispatcher, 
                KUID nodeId, SocketAddress dst, DHTMessage message, EventType type) {
            this.nodeId = nodeId;
            this.dst = dst;
            this.message = message;
            this.type = type;
        }
        
        public MessageDispatcher getMessageDispatcher() {
            return messageDispatcher;
        }
        
        public KUID getNodeID() {
            return nodeId;
        }
        
        public SocketAddress getSocketAddress() {
            return dst;
        }
        
        public DHTMessage getMessage() {
            return message;
        }
        
        public EventType getEventType() {
            return type;
        }
    }
}