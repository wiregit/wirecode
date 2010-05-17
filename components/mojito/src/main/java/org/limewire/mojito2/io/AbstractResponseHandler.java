package org.limewire.mojito2.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.Entity;
import org.limewire.mojito2.entity.RequestTimeoutException;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.message.ResponseMessage;
import org.limewire.util.Objects;

public abstract class AbstractResponseHandler<V extends Entity> 
        implements ResponseHandler, AsyncProcess<V> {
    
    protected final Context context;
    
    protected final long timeout;
    
    protected final TimeUnit unit;
    
    protected volatile DHTFuture<V> future = null;
    
    private long startTime = -1L;
    
    private long timeStamp = 0L;
    
    public AbstractResponseHandler(Context context, 
            long timeout, TimeUnit unit) {
        
        this.context = context;
        this.timeout = timeout;
        this.unit = unit;
    }
    
    /**
     * 
     */
    public Context getContext() {
        return context;
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
                
                future.addFutureListener(new EventListener<FutureEvent<V>>() {
                    @Override
                    public void handleEvent(FutureEvent<V> event) {
                        doStop();
                    }
                });
                
                this.startTime = System.currentTimeMillis();
                try {
                    start();
                } catch (IOException err) {
                    setException(err);
                }
            }
        }
    }
    
    /**
     * 
     */
    private void doStop() {
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
    public final void handleResponse(RequestHandle request, 
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
    protected abstract void processResponse(RequestHandle request, 
            ResponseMessage response, long time,
            TimeUnit unit) throws IOException;
    
    @Override
    public final void handleTimeout(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            synchronized (this) {
                processTimeout(request, time, unit);
            }
        }
    }
    
    /**
     * 
     */
    protected synchronized void processTimeout(RequestHandle request, 
            long time, TimeUnit unit) throws IOException {
        setException(new RequestTimeoutException(request, time, unit));
    }
    
    @Override
    public final void handleException(RequestHandle request, Throwable exception) {
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
            RequestHandle request, Throwable exception) {
        setException(exception);
    }
    
    /**
     * 
     */
    protected void send(KUID contactId, SocketAddress dst, 
            RequestMessage request, long timeout, TimeUnit unit) 
                throws IOException {
        
        MessageDispatcher messageDispatcher 
            = context.getMessageDispatcher();
        messageDispatcher.send(this, contactId, dst, 
                request, timeout, unit);
    }
}
