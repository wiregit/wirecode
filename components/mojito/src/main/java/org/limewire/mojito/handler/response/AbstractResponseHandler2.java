package org.limewire.mojito.handler.response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.Entity;
import org.limewire.mojito.handler.ResponseHandler2;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.util.Objects;

public abstract class AbstractResponseHandler2<V extends Entity> 
        implements ResponseHandler2, AsyncProcess<V> {
    
    private static final Log LOG 
        = LogFactory.getLog(AbstractResponseHandler2.class);

    protected final Context context;
    
    protected final MessageDispatcher2 messageDispatcher;
    
    protected final long timeout;
    
    protected final TimeUnit unit;
    
    protected volatile DHTFuture<V> future = null;
    
    private long startTime = -1L;
    
    private long timeStamp = 0L;
    
    public AbstractResponseHandler2(Context context, 
            MessageDispatcher2 messageDispatcher, 
            long timeout, TimeUnit unit) {
        
        this.context = context;
        this.messageDispatcher = messageDispatcher;
        this.timeout = timeout;
        this.unit = unit;
    }
    
    /**
     * 
     */
    public long getTimeout(TimeUnit unit) {
        return unit.convert(timeout, this.unit);
    }
    
    /**
     * 
     */
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }
    
    @Override
    public final void start(DHTFuture<V> future) {
        Objects.nonNull(future, "future");
        
        synchronized (future) {
            synchronized (this) {
                this.future = future;
                this.startTime = System.currentTimeMillis();
                
                try {
                    start();
                } catch (IOException err) {
                    setException(err);
                }
            }
        }
    }
    
    //@Override
    public final void stop(DHTFuture<V> future) {
        Objects.nonNull(future, "future");
        
        synchronized (future) {
            synchronized (this) {
                stop();
            }
        }
    }
    
    /**
     * 
     */
    protected abstract void start() throws IOException;
    
    /**
     * 
     */
    protected synchronized void stop() {
        
    }
    
    /**
     * 
     */
    protected long getTime(TimeUnit unit) {
        long duration = System.currentTimeMillis() - startTime;
        return unit.convert(duration, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    protected boolean setValue(V value) {
        return future.setValue(value);
    }
    
    /**
     * 
     */
    protected boolean setException(Throwable exception) {
        return future.setException(exception);
    }
    
    /**
     * 
     */
    protected synchronized long getLastResponseTime(TimeUnit unit) {
        long duration = System.currentTimeMillis() - timeStamp;
        return unit.convert(duration, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    protected long getLastResponseTimeInMillis() {
        return getLastResponseTime(TimeUnit.MILLISECONDS);
    }

    @Override
    public final void handleResponse(RequestMessage request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            synchronized (this) {
                timeStamp = System.currentTimeMillis();
                processResponse(request, response, time, unit);
            }
        }
    }
    
    /**
     * 
     */
    protected abstract void processResponse(RequestMessage request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException;
    
    @Override
    public final void handleTimeout(KUID contactId, SocketAddress dst,
            RequestMessage request, long time, TimeUnit unit) throws IOException {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            synchronized (this) {
                processTimeout(contactId, dst, request, time, unit);
            }
        }
    }
    
    /**
     * 
     */
    protected synchronized void processTimeout(KUID contactId, SocketAddress dst,
            RequestMessage request, long time, TimeUnit unit) throws IOException {
        setException(new RequestTimeoutException(
                contactId, dst, request, time, unit));
    }
    
    @Override
    public final void handleException(RequestMessage request, Throwable exception) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            synchronized (this) {
                processException(request, exception);
            }
        }
    }
    
    /**
     * 
     */
    protected synchronized void processException(
            RequestMessage request, Throwable exception) {
        setException(exception);
    }
    
    public static class RequestTimeoutException extends IOException {
        
        private final KUID contactId;
        
        private final SocketAddress dst;
        
        private final RequestMessage request;
        
        private final long time;
        
        private final TimeUnit unit;
        
        public RequestTimeoutException(KUID contactId, 
                SocketAddress dst, RequestMessage request, 
                long time, TimeUnit unit) {
            
            this.contactId = contactId;
            this.dst = dst;
            this.request = request;
            this.time = time;
            this.unit = unit;
        }
    }
}
