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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.IOUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.collection.FixedSizeHashSet;
import org.limewire.mojito.message.Message;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.MessageID;
import org.limewire.mojito.message.RequestMessage;
import org.limewire.mojito.message.ResponseMessage;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.EventUtils;
import org.limewire.mojito.util.SchedulingUtils;
import org.limewire.util.Objects;

/**
 * An abstract implementation of {@link MessageDispatcher}
 */
abstract class AbstractMessageDispatcher implements MessageDispatcher {

    private static final Log LOG 
        = LogFactory.getLog(AbstractMessageDispatcher.class);
    
    @InspectablePrimitive(value = "Number of messages sent")
    private static final AtomicLong MESSAGES_SENT = new AtomicLong();
    
    @InspectablePrimitive(value = "Number of messages received")
    private static final AtomicLong MESSAGES_RECEIVED = new AtomicLong();
    
    private final List<MessageDispatcherListener> listeners 
        = new CopyOnWriteArrayList<MessageDispatcherListener>();
    
    private final RequestManager requestManager = new RequestManager();
    
    private final ResponseHistory history = new ResponseHistory(512);
    
    private final MessageFactory messageFactory;
    
    private volatile Transport transport = null;
    
    private boolean open = true;
    
    public AbstractMessageDispatcher(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }
    
    @Override
    public synchronized void bind(Transport transport) throws IOException {
        Objects.nonNull(transport, "transport");
        
        if (!open) {
            throw new IOException();
        }
        
        if (isBound()) {
            throw new IOException();
        }
        
        this.transport = transport;
        transport.bind(this);
    }
    
    @Override
    public synchronized Transport unbind() {
        return unbind(false);
    }
    
    /**
     * Unbinds the {@link MessageDispatcher} from the underlying
     * {@link Transport} and calls {@code close()} if necessary.
     */
    private synchronized Transport unbind(boolean close) {
        Transport transport = this.transport;
        
        if (transport != null) {
            transport.unbind();
            
            if (close && transport instanceof Closeable) {
                IOUtils.close((Closeable)transport);
            }
            
            this.transport = null;
        }
        
        return transport;
    }
    
    @Override
    public synchronized boolean isBound() {
        return transport != null;
    }
    
    @Override
    public void close() {
        unbind(true);
        requestManager.close();
    }
    
    @Override
    public synchronized Transport getTransport() {
        return transport;
    }
    
    @Override
    public void addMessageDispatcherListener(MessageDispatcherListener l) {
        listeners.add(l);
    }
    
    @Override
    public void removeMessageDispatcherListener(MessageDispatcherListener l) {
        listeners.remove(l);
    }
    
    /**
     * Returns {@code true} if the {@link Message} is from localhost.
     * 
     * <p>NOTE: The default implementation returns {@code false}.
     */
    protected boolean isLocalhost(KUID contactId, 
            SocketAddress address, Message message) {
        return false;
    }
    
    @Override
    public void send(Contact dst, ResponseMessage response) throws IOException {
        
        KUID contactId = dst.getContactId();
        SocketAddress address = dst.getContactAddress();
        
        if (isLocalhost(contactId, address, response)) {
            throw new IOException();
        }
        
        Transport transport = this.transport;
        if (transport == null) {
            throw new IOException("Not Bound!");
        }
        
        byte[] data = messageFactory.serialize(address, response);
        transport.send(address, data, 0, data.length);
        
        fireMessageSent(contactId, address, response);
    }
    
    @Override
    public void send(ResponseHandler callback, Contact dst, 
            RequestMessage request, long timeout, TimeUnit unit) 
                throws IOException {
        KUID contactId = dst.getContactId();
        SocketAddress address = dst.getContactAddress();
        
        send(callback, contactId, address, request, timeout, unit);
    }
    
    @Override
    public void send(ResponseHandler callback, KUID contactId, 
            SocketAddress dst, RequestMessage request, 
            long timeout, TimeUnit unit) throws IOException {
        
        if (isLocalhost(contactId, dst, request)) {
            throw new IOException();
        }

        if (callback != null) {
            RequestHandle handle = new RequestHandle(
                    contactId, dst, request);
            
            requestManager.add(callback, handle, timeout, unit);
        }
        
        Transport transport = this.transport;
        if (transport == null) {
            throw new IOException("Not Bound!");
        }
        
        byte[] data = messageFactory.serialize(dst, request);
        transport.send(dst, data, 0, data.length);
        
        fireMessageSent(contactId, dst, request);
    }
    
    @Override
    public void handleMessage(SocketAddress src, byte[] data, 
            int offset, int length) throws IOException {
        
        Message message = messageFactory.deserialize(
                src, data, offset, length);
        
        handleMessage(message);
    }
    
    @Override
    public void handleMessage(Message message) throws IOException {
        
        if (message instanceof RequestMessage) {
            handleRequest((RequestMessage)message);
        } else {
            handleResponse((ResponseMessage)message);
        }
        
        fireMessageReceived(message);
    }
    
    /**
     * Called for each {@link ResponseMessage}.
     */
    protected void handleResponse(ResponseMessage response) throws IOException {
        
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
     * Called for each {@link RequestMessage}.
     */
    protected abstract void handleRequest(
            RequestMessage request) throws IOException;
    
    /**
     * Called for each {@link ResponseMessage} that arrived too late.
     */
    protected abstract void handleLateResponse(
            ResponseMessage response) throws IOException;
    
    /**
     * A callback method that is called for each {@link ResponseMessage} 
     * that passed all checks.
     * 
     * @see #handleResponse(ResponseMessage)
     */
    protected void handleResponse(ResponseHandler callback, 
            RequestHandle handle, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        callback.handleResponse(handle, response, time, unit);
    }
    
    /**
     * A callback method that is called for each timeout.
     */
    protected void handleTimeout(ResponseHandler callback, 
            RequestHandle handle, long time, TimeUnit unit) throws IOException {
        callback.handleTimeout(handle, time, unit);
    }
    
    /**
     * A callback method that is called for each {@link ResponseMessage}
     * that didn't pass all checks.
     */
    protected void handleIllegalResponse(ResponseHandler callback, 
            RequestHandle handle, ResponseMessage response, 
            long time, TimeUnit unit) throws IOException {
        
        if (LOG.isErrorEnabled()) {
            LOG.error("Illegal Response: " + handle + " -> " + response);
        }
    }
    
    /**
     * Fires a message sent event.
     */
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
    
    /**
     * Fires a message received event.
     */
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
     * The {@link RequestManager} is responsible for keeping track of
     * all {@link RequestMessage}s we have sent.
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
         * Adds the given {@link RequestHandle}.
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
                    = SchedulingUtils.schedule(task, timeout, unit);
                
                RequestEntity entity = new RequestEntity(
                        future, callback, handle);
                callbacks.put(messageId, entity);
            }
        }
        
        /**
         * Returns the {@link RequestEntity} for the 
         * given {@link ResponseMessage}.
         */
        public RequestEntity remove(ResponseMessage message) {
            return callbacks.remove(message.getMessageId());
        }
    }
    
    /**
     * An internal class that keeps track of the creation time,
     * {@link RequestHandle} etc.
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
         * Cancels the {@link RequestEntity}.
         */
        public boolean cancel() {
            future.cancel(true);
            return open.getAndSet(false);
        }
        
        /**
         * Called for a {@link ResponseMessage}.
         */
        public boolean handleResponse(ResponseMessage response) throws IOException {
            if (cancel()) {
                long time = System.currentTimeMillis() - creationTime;
                
                if (handle.check(response)) {
                    AbstractMessageDispatcher.this.handleResponse(callback, handle, 
                            response, time, TimeUnit.MILLISECONDS);
                } else {
                    AbstractMessageDispatcher.this.handleIllegalResponse(callback, handle, 
                            response, time, TimeUnit.MILLISECONDS);
                }
                
                return true;
            }
            
            return false;
        }

        /**
         * Called if a timeout occurred.
         */
        public void handleTimeout() throws IOException {
            if (cancel()) {
                long time = System.currentTimeMillis() - creationTime;
                AbstractMessageDispatcher.this.handleTimeout(callback, 
                        handle, time, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    /**
     * The {@link ResponseHistory} keeps track of {@link ResponseMessage}
     * {@link MessageID}s and makes essentially sure that responses are
     * never processed more than once.
     */
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
