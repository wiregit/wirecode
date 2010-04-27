package org.limewire.mojito.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.mojito.KUID;
import org.limewire.mojito.handler.ResponseHandler2;
import org.limewire.mojito.message2.Message;
import org.limewire.mojito.message2.MessageID;
import org.limewire.mojito.message2.NodeRequest;
import org.limewire.mojito.message2.NodeResponse;
import org.limewire.mojito.message2.PingRequest;
import org.limewire.mojito.message2.PingResponse;
import org.limewire.mojito.message2.RequestMessage;
import org.limewire.mojito.message2.ResponseMessage;
import org.limewire.mojito.message2.StoreRequest;
import org.limewire.mojito.message2.StoreResponse;
import org.limewire.mojito.message2.ValueRequest;
import org.limewire.mojito.message2.ValueResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.EventUtils;
import org.limewire.mojito.util.FixedSizeHashSet;

/**
 * 
 */
public abstract class MessageDispatcher2 implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(MessageDispatcher2.class);
    
    private static final ScheduledExecutorService EXECUTOR
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "MessageDispatcherThread"));
    
    @InspectablePrimitive(value = "")
    private static final AtomicInteger MESSAGES_SENT = new AtomicInteger();
    
    @InspectablePrimitive(value = "")
    private static final AtomicInteger MESSAGES_RECEIVED = new AtomicInteger();
    
    /**
     * 
     */
    private final List<MessageDispatcherListener2> listeners 
        = new CopyOnWriteArrayList<MessageDispatcherListener2>();
    
    /**
     * 
     */
    private final RequestManager requestManager = new RequestManager();
    
    /**
     * 
     */
    private final ResponseHistory history = new ResponseHistory(512);
    
    /**
     * 
     */
    private final Transport transport;
    
    /**
     * 
     */
    public MessageDispatcher2(Transport transport) {
        this.transport = transport;
    }
    
    /**
     * 
     */
    public void bind() {
        transport.bind(this);
    }
    
    /**
     * 
     */
    public void unbind() {
        transport.bind(null);
    }
    
    /**
     * Returns the {@link Transport}
     */
    public Transport getTransport() {
        return transport;
    }
    
    @Override
    public void close() {
        unbind();
        requestManager.close();
    }
    
    /**
     * 
     */
    public void addMessageDispatcherListener(MessageDispatcherListener2 l) {
        listeners.add(l);
    }
    
    /**
     * 
     */
    public void removeMessageDispatcherListener(MessageDispatcherListener2 l) {
        listeners.remove(l);
    }
    
    /**
     * 
     */
    public void send(Contact dst, ResponseMessage response) throws IOException {
        SocketAddress address = dst.getContactAddress();
        transport.send(address, response);
        
        fireMessageSent(response);
    }
    
    /**
     * 
     */
    public void send(ResponseHandler2 callback, KUID contactId, 
            SocketAddress dst, RequestMessage request, 
            long timeout, TimeUnit unit) throws IOException {
        
        requestManager.add(callback, contactId, dst, 
                request, timeout, unit);
        transport.send(dst, request);
        
        fireMessageSent(request);
    }
    
    /**
     * 
     */
    public void handleMessage(Message message) throws IOException {
        if (message instanceof RequestMessage) {
            handleRequest((RequestMessage)message);
        } else {
            handleResponse((ResponseMessage)message);
        }
        
        fireMessageReceived(message);
    }
    
    /**
     * 
     */
    private void handleResponse(ResponseMessage response) throws IOException {
        
        if (!history.check(response)) {
            return;
        }
        
        RequestEntity entity = requestManager.remove(response);
        
        boolean success = false;
        if (entity != null) {
            success = entity.handleResponse(response);
        }
        
        if (!success) {
            handleLateResponse(response);
        }
    }
    
    /**
     * 
     */
    protected abstract void handleRequest(
            RequestMessage request) throws IOException;
    
    /**
     * 
     */
    protected abstract void handleLateResponse(
            ResponseMessage response) throws IOException;
    
    /**
     * 
     */
    protected void handleResponse(ResponseHandler2 callback, 
            RequestMessage request, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        callback.handleResponse(request, response, time, unit);
    }
    
    /**
     * 
     */
    protected void handleTimeout(ResponseHandler2 callback, KUID contactId, 
            SocketAddress dst, RequestMessage request, 
            long time, TimeUnit unit) throws IOException {
        callback.handleTimeout(contactId, dst, request, time, unit);
    }
    
    /**
     * 
     */
    protected void handleIllegalResponse(ResponseHandler2 callback, 
            RequestMessage request, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        
        if (LOG.isErrorEnabled()) {
            LOG.error("Illegal Response: " + request + " -> " + response);
        }
    }
    
    protected void fireMessageSent(final Message message) {
        
        MESSAGES_SENT.incrementAndGet();
        
        if (!listeners.isEmpty()) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (MessageDispatcherListener2 l : listeners) {
                        l.messageSent(message);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
    }
    
    protected void fireMessageReceived(final Message message) {
        
        MESSAGES_RECEIVED.incrementAndGet();
        
        if (!listeners.isEmpty()) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (MessageDispatcherListener2 l : listeners) {
                        l.messageReceived(message);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
    }
    
    /**
     * 
     */
    public static interface Transport {
        
        /**
         * 
         */
        public void bind(MessageDispatcher2 messageDispatcher);
        
        /**
         * 
         */
        public void send(SocketAddress dst, Message message) throws IOException;
    }
    
    /**
     * 
     */
    public static abstract class AbstractTransport implements Transport {

        /**
         * 
         */
        private volatile MessageDispatcher2 messageDispatcher;
        
        @Override
        public void bind(MessageDispatcher2 messageDispatcher) {
            this.messageDispatcher = messageDispatcher;
        }
        
        /**
         * 
         */
        public boolean isBound() {
            return messageDispatcher != null;
        }
        
        /**
         * 
         */
        public boolean handleMessage(Message message) throws IOException {
            MessageDispatcher2 messageDispatcher = this.messageDispatcher;
            
            if (messageDispatcher != null) {
                messageDispatcher.handleMessage(message);
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * 
     */
    private class RequestManager implements Closeable {
        
        private final Map<MessageID, RequestEntity> callbacks
            = Collections.synchronizedMap(
                    new HashMap<MessageID, RequestEntity>());
        
        private boolean open = true;
        
        @Override
        public void close() {
            synchronized (callbacks) {
                if (!open) {
                    return;
                }
                
                open = false;
                
                for (RequestEntity entity : callbacks.values()) {
                    entity.cancel();
                }
                
                callbacks.clear();
            }
        }
        
        /**
         * 
         */
        public void add(ResponseHandler2 callback, KUID contactId, 
                SocketAddress dst, RequestMessage request, 
                long timeout, TimeUnit unit) {
            
            final MessageID messageId = request.getMessageId();
            
            synchronized (callbacks) {
                if (!open) {
                    throw new IllegalStateException();
                }
                
                if (callbacks.containsKey(messageId)) {
                    throw new IllegalArgumentException("messageId=" + messageId);
                }
                
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        RequestEntity entity 
                            = callbacks.remove(messageId);
                        
                        if (entity != null) {
                            try {
                                entity.handleTimeout();
                            } catch (IOException err) {
                                LOG.error("IOException", err);
                            }
                        }
                    }
                };
                
                ScheduledFuture<?> future
                    = EXECUTOR.schedule(task, timeout, unit);
                
                RequestEntity entity = new RequestEntity(
                        future, callback, contactId, dst, request);
                callbacks.put(messageId, entity);
            }
        }
        
        /**
         * 
         */
        public RequestEntity remove(ResponseMessage message) {
            return callbacks.remove(message.getMessageId());
        }
    }
    
    /**
     * 
     */
    private class RequestEntity {
        
        private final long creationTime = System.currentTimeMillis();
        
        private final ScheduledFuture<?> future;
        
        private final ResponseHandler2 callback;
        
        private final KUID contactId;
        
        private final SocketAddress dst;
        
        private final RequestMessage request;
        
        private final AtomicBoolean open = new AtomicBoolean(true);
        
        public RequestEntity(ScheduledFuture<?> future, 
                ResponseHandler2 callback, KUID contactId,
                SocketAddress dst, RequestMessage request) {
            
            this.future = future;
            this.callback = callback;
            
            this.contactId = contactId;
            this.dst = dst;
            this.request = request;
        }
        
        /**
         * 
         */
        public boolean cancel() {
            future.cancel(true);
            return open.getAndSet(false);
        }
        
        /**
         * 
         */
        public boolean check(ResponseMessage response) {
            if (request instanceof PingRequest) {
                return response instanceof PingResponse;
            } else if (request instanceof NodeRequest) {
                return response instanceof NodeResponse;
            } else if (request instanceof ValueRequest) {
                return response instanceof ValueResponse 
                    || response instanceof NodeResponse;
            } else if (request instanceof StoreRequest) {
                return response instanceof StoreResponse;
            }
            
            return false;
        }
        
        /**
         * 
         */
        public boolean handleResponse(ResponseMessage response) throws IOException {
            if (cancel()) {
                long time = System.currentTimeMillis() - creationTime;
                
                if (check(response)) {
                    MessageDispatcher2.this.handleResponse(callback, request, 
                            response, time, TimeUnit.MILLISECONDS);
                } else {
                    MessageDispatcher2.this.handleIllegalResponse(callback, request, 
                            response, time, TimeUnit.MILLISECONDS);
                }
                
                return true;
            }
            
            return false;
        }

        /**
         * 
         */
        public void handleTimeout() throws IOException {
            if (cancel()) {
                long time = System.currentTimeMillis() - creationTime;
                MessageDispatcher2.this.handleTimeout(callback, 
                        contactId, dst, request, 
                        time, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    private static class ResponseHistory {
        
        private static final Log LOG 
            = LogFactory.getLog(ResponseHistory.class);
        
        private final Set<MessageID> history;
        
        public ResponseHistory(int historySize) {
            this.history = Collections.synchronizedSet(
                    new FixedSizeHashSet<MessageID>(historySize));
        }
        
        public boolean check(ResponseMessage response) {
            MessageID messageId = response.getMessageId();
            if (!history.add(messageId)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Multiple respones: " + response);
                }
                return false;
            }
            
            return true;
        }
    }
}
