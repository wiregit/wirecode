/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
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
    
    private final Object OUTPUT_LOCK = new Object();
    
    private LinkedList output = new LinkedList();
    private ReceiptMap input = new ReceiptMap(1024);
    
    private Selector selector;
    private DatagramChannel channel;
    
    private Context context;
    
    private DefaultMessageHandler defaultHandler;
    private PingRequestHandler pingHandler;
    private FindNodeRequestHandler findNodeHandler;
    private FindValueRequestHandler findValueHandler;
    private StoreRequestHandler storeHandler;
    
    private Filter filter;
    
    public MessageDispatcher(Context context) {
        this.context = context;
        
        defaultHandler = new DefaultMessageHandler(context);
        pingHandler = new PingRequestHandler(context);
        findNodeHandler = new FindNodeRequestHandler(context);
        findValueHandler = new FindValueRequestHandler(context);
        storeHandler = new StoreRequestHandler(context);
        
        filter = new Filter();
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
        
        input.clear();
        output.clear();
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
        
        // MAKE SURE WE'RE NOT SENDING MESSAGES TO OURSELF
        if (nodeId != null 
                && nodeId.equals(context.getLocalNodeID())) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send messages to ourself: " + ContactNode.toString(nodeId, dst));
            }
            return;
        }
        
        if(handler == null) {
            handler = defaultHandler;
        }
        
        Receipt receipt = new Receipt(context, nodeId, dst, message, handler);
        
        synchronized(OUTPUT_LOCK) {
            output.add(receipt);
            interestWrite(true);
        }          	

    }
    
    private void handleRequest(KUID nodeId, SocketAddress src, Message msg) throws IOException {
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
                LOG.error(t);
            }
        }
    }
    
    private void handleLateResponse(KUID nodeId, SocketAddress src, Message msg) throws IOException {
        
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
        ContactNode node = new ContactNode(nodeId,src);
        context.getRouteTable().add(node,true);
    }
    
    /**
     * Returns the number of remaining Messages in
     * the output Queue
     */
    private int writeNext() throws IOException {
        if (!output.isEmpty()) {
            Receipt receipt = (Receipt)output.removeFirst();

            if (receipt.send(channel)) {
                receipt.sent();
                
                if (receipt.isRequest()) {
                    input.put(receipt.getMessageID(), receipt);
                }
                receipt.freeData();
            } else {
                output.addFirst(receipt);
            }
        }
        return output.size();
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
            Receipt receipt = null;
            
            if (message instanceof ResponseMessage) {
                receipt = (Receipt)input.remove(message.getMessageID());
            
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
        
        // MAKE SURE WE'RE NOT RECEIVING MESSAGES FROM OURSELF
        if (nodeId != null 
                && nodeId.equals(context.getLocalNodeID())
                && src.equals(context.getLocalSocketAddress())) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Received a message from ourself: " + ContactNode.toString(nodeId, src));
            }
            return;
        }
        
        if (receipt != null) {
            long time = receipt.time();
            defaultHandler.handleResponse(nodeId, src, message, time); // BEFORE!
            
            if (receipt.getHandler() != defaultHandler) {
                receipt.handleSuccess(nodeId, src, message);
            }
        } else {
            
            // Make sure a singe Node cannot monopolize our resources
            if (!filter.allow(src)) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(ContactNode.toString(nodeId, src) + " refused");
                }
                return;
            }
            
            if (message instanceof RequestMessage) {
                handleRequest(nodeId, src, message);
                defaultHandler.handleRequest(nodeId, src, message); // AFTER!
            } else if (message instanceof ResponseMessage) {
                handleLateResponse(nodeId, src, message);
            } else if (LOG.isErrorEnabled()) {
                LOG.error(message + " is neither Request nor Response");
            }
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
        
        while(isRunning()) {
            try {
                
                selector.select(SLEEP);
                
                while(readNext() && isRunning());
                interestRead(true); // We're always interested in reading
                
                synchronized(OUTPUT_LOCK) {
                    
                    // TODO propper throtteling.
                    int remaining = 0;
                    for(int i = 0; i < 10 && isRunning(); i++) {
                        remaining = writeNext();
                        if (remaining == 0) {
                            break;
                        }
                    }
                    interestWrite(remaining > 0);
                }
                
                if ((System.currentTimeMillis()-lastCleanup) >= CLEANUP_INTERVAL) {
                    input.cleanup();
                    lastCleanup = System.currentTimeMillis();
                }
            } catch (ClosedChannelException err) {
                // thrown as close() is called asynchron
                //LOG.error(err);
            } catch (IOException err) {
                err.printStackTrace();
                //LOG.error(err);
            }
        }
    }
    
    private static class ReceiptMap extends FixedSizeHashMap {
        
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
                    
                    try {
                        receipt.handleTimeout();
                    } catch (Throwable t) {
                        LOG.error(t);
                    }
                }
            }
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            Receipt receipt = (Receipt)eldest.getValue();
            if (super.removeEldestEntry(eldest) 
                    || receipt.timeout()) {
                receipt.received();
                try {
                    receipt.handleTimeout();
                } catch (Throwable t) {
                    LOG.error(t);
                }
                return true;
            }
            return false;
        }
    }  
}
