package org.limewire.mojito2.io;

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
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.collection.FixedSizeHashSet;
import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.message.MessageID;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.util.EventUtils;
import org.limewire.mojito2.util.IoUtils;
import org.limewire.util.Objects;

/**
 * 
 */
public abstract class MessageDispatcher implements Closeable {

    private static final Log LOG 
        = LogFactory.getLog(MessageDispatcher.class);
    
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
    private final List<MessageDispatcherListener> listeners 
        = new CopyOnWriteArrayList<MessageDispatcherListener>();
    
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
    private volatile Transport transport = null;
    
    /**
     * 
     */
    private boolean open = true;
    
    /**
     * 
     */
    public synchronized void bind(Transport transport) throws IOException {
        Objects.nonNull(transport, "transport");
        
        if (!open) {
            throw new IOException();
        }
        
        if (this.transport != null) {
            throw new IOException();
        }
        
        this.transport = transport;
        transport.bind(this);
    }
    
    /**
     * 
     */
    public synchronized Transport unbind() {
        return unbind(false);
    }
    
    /**
     * 
     */
    private synchronized Transport unbind(boolean close) {
        Transport transport = this.transport;
        
        if (transport != null) {
            transport.bind(null);
            
            if (close && transport instanceof Closeable) {
                IoUtils.close((Closeable)transport);
            }
            
            this.transport = null;
        }
        
        return transport;
    }
    
    /**
     * 
     */
    public synchronized boolean isBound() {
        return transport != null;
    }
    
    @Override
    public void close() {
        unbind(true);
        requestManager.close();
    }
    
    /**
     * Returns the {@link Transport}
     */
    public synchronized Transport getTransport() {
        return transport;
    }
    
    /**
     * 
     */
    public void addMessageDispatcherListener(MessageDispatcherListener l) {
        listeners.add(l);
    }
    
    /**
     * 
     */
    public void removeMessageDispatcherListener(MessageDispatcherListener l) {
        listeners.remove(l);
    }
    
    /**
     * 
     */
    public void send(Contact dst, ResponseMessage response) throws IOException {
        SocketAddress address = dst.getContactAddress();
        
        Transport transport = this.transport;
        if (transport == null) {
            throw new IOException("Not Bound!");
        }
        
        transport.send(address, response);
        fireMessageSent(dst.getNodeID(), address, response);
    }
    
    /**
     * 
     */
    public void send(ResponseHandler callback, KUID contactId, 
            SocketAddress dst, RequestMessage request, 
            long timeout, TimeUnit unit) throws IOException {
        
        RequestHandle handle = new RequestHandle(
                contactId, dst, request);
        
        requestManager.add(callback, handle, timeout, unit);
        
        Transport transport = this.transport;
        if (transport == null) {
            throw new IOException("Not Bound!");
        }
        
        transport.send(dst, request);
        fireMessageSent(contactId, dst, request);
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
    protected void handleResponse(ResponseHandler callback, 
            RequestHandle handle, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        callback.handleResponse(handle, response, time, unit);
    }
    
    /**
     * 
     */
    protected void handleTimeout(ResponseHandler callback, 
            RequestHandle handle, long time, TimeUnit unit) throws IOException {
        callback.handleTimeout(handle, time, unit);
    }
    
    /**
     * 
     */
    protected void handleIllegalResponse(ResponseHandler callback, 
            RequestHandle handle, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        
        if (LOG.isErrorEnabled()) {
            LOG.error("Illegal Response: " + handle + " -> " + response);
        }
    }
    
    protected void fireMessageSent(final KUID contactId, 
            final SocketAddress dst, final Message message) {
        
        MESSAGES_SENT.incrementAndGet();
        
        if (!listeners.isEmpty()) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (MessageDispatcherListener l : listeners) {
                        l.messageSent(contactId, dst, message);
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
                    for (MessageDispatcherListener l : listeners) {
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
        public void add(ResponseHandler callback, RequestHandle handle, 
                long timeout, TimeUnit unit) {
            
            final MessageID messageId = handle.getMessageId();
            
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
                        future, callback, handle);
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
        
        private final ResponseHandler callback;
        
        private final RequestHandle handle;
        
        private final AtomicBoolean open = new AtomicBoolean(true);
        
        public RequestEntity(ScheduledFuture<?> future, 
                ResponseHandler callback, RequestHandle handle) {
            
            this.future = future;
            this.callback = callback;
            
            this.handle = handle;
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
        public boolean handleResponse(ResponseMessage response) throws IOException {
            if (cancel()) {
                long time = System.currentTimeMillis() - creationTime;
                
                if (handle.check(response)) {
                    MessageDispatcher.this.handleResponse(callback, handle, 
                            response, time, TimeUnit.MILLISECONDS);
                } else {
                    MessageDispatcher.this.handleIllegalResponse(callback, handle, 
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
                MessageDispatcher.this.handleTimeout(callback, 
                        handle, time, TimeUnit.MILLISECONDS);
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
