package org.limewire.mojito.handler.response2;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent2.AsyncFuture;
import org.limewire.mojito.concurrent2.AsyncProcess;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.ResponseMessage;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;

public abstract class AbstractResponseHandler<V extends Result> 
        implements AsyncProcess<V>, ResponseHandler {

    private static final Log LOG 
        = LogFactory.getLog(AbstractResponseHandler.class);
    
    private final AtomicInteger errors = new AtomicInteger();
    
    protected final Context context;
    
    private volatile AsyncFuture<V> future = null;
    
    private long elapsedTime = 0L;
    
    private long lastResponseTime = 0L;
    
    private volatile int maxErrors = 0;
    
    public AbstractResponseHandler(Context context) {
        this.context = context;
    }
    
    /**
     * 
     */
    protected AsyncFuture<V> getAsyncFuture() {
        AsyncFuture<V> future = this.future;
        if (future != null) {
            return future;
        }
        
        throw new IllegalStateException();
    }
    
    /**
     * 
     */
    protected boolean isDone() {
        return getAsyncFuture().isDone();
    }
    
    /**
     * 
     */
    protected boolean cancel() {
        return getAsyncFuture().cancel(true);
    }
    
    /**
     * 
     */
    protected boolean setValue(V value) {
        return getAsyncFuture().setValue(value);
    }
    
    /**
     * 
     */
    protected boolean setException(Throwable exception) {
        return getAsyncFuture().setException(exception);
    }
    
    /**
     * 
     */
    protected long getTimeout(TimeUnit unit) {
        return getAsyncFuture().getTimeout(unit);
    }
    
    /**
     * 
     */
    protected long getTimeoutInMillis() {
        return getAsyncFuture().getTimeoutInMillis();
    }
    
    /**
     * 
     */
    public int getErrors() {
        return errors.get();
    }
    
    /**
     * 
     */
    public int getMaxErrors() {
        return maxErrors;
    }
    
    /**
     * Sets the maximum number of errors that may occur before
     * we're giving up to re-send a request.
     */
    public void setMaxErrors(int maxErrors) {
        if (maxErrors < 0) {
            this.maxErrors = NetworkSettings.MAX_ERRORS.getValue();
        } else {
            this.maxErrors = maxErrors;
        }
    }
    
    /**
     * 
     */
    protected long getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * 
     */
    protected long getLastResponseTime() {
        return lastResponseTime;
    }
    
    @Override
    public final void start(AsyncFuture<V> future) throws Exception {
        this.future = future;
        
        doStart(future);
    }

    @Override
    public final void stop(AsyncFuture<V> future) throws Exception {
        doStop(future);
    }
    
    /**
     * 
     */
    protected abstract void doStart(AsyncFuture<V> future) throws Exception;
    
    /**
     * 
     */
    protected void doStop(AsyncFuture<V> future) throws Exception {
        
    }

    @Override
    public final void handleResponse(ResponseMessage response, long time) 
            throws IOException {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            elapsedTime += time;
            lastResponseTime = System.currentTimeMillis();
            
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received " + response + " from "
                        + response.getContact() + " after " + errors
                        + " errors and a total time of " + elapsedTime
                        + "ms");
            }
            
            response(response, time);
        }
    }
    
    /**
     * 
     */
    protected abstract void response(ResponseMessage message, long time) 
            throws IOException;

    @Override
    public final void handleTimeout(KUID nodeId, SocketAddress dst, 
            RequestMessage request, long time) throws IOException {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            if (LOG.isTraceEnabled()) {
                LOG.trace(request + " to " + ContactUtils.toString(nodeId, dst)
                        + " failed after " + time + "ms");
            }
            
            elapsedTime += time;
            
            if (errors.getAndIncrement() < maxErrors) {
                resend(nodeId, dst, request);
            } else {
                timeout(nodeId, dst, request, elapsedTime);
            }
        }
    }
    
    /**
     * 
     */
    protected abstract void timeout(KUID nodeId, SocketAddress dst, 
            RequestMessage message, long time) throws IOException;
    
    /**
     * 
     */
    protected void resend(KUID nodeId, SocketAddress dst, 
            RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending " + message + " to " 
                    + ContactUtils.toString(nodeId, dst));
        }
        
        MessageDispatcher messageDispatcher = context.getMessageDispatcher();
        messageDispatcher.send(nodeId, dst, message, this);
    }
    
    /**
     * 
     */
    @Override
    public final void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, IOException e) {
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }

            if (LOG.isErrorEnabled()) {
                LOG.error("Sending a " + message + " to "
                        + ContactUtils.toString(nodeId, dst) + " failed", e);
            }

            error(nodeId, dst, message, e);
        }
    }
    
    /**
     * 
     */
    protected abstract void error(KUID nodeId, SocketAddress dst, 
            RequestMessage message, IOException e);
    
    /**
     * 
     */
    protected void done() {
        
    }
    
    /**
     * A helper method to throw Timeout Exceptions.
     */
    protected void fireTimeoutException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
        setException(createTimeoutException(nodeId, address, request, time));
    }
    
    /**
     * A helper method to create Timeout Exceptions.
     */
    protected DHTTimeoutException createTimeoutException(KUID nodeId, SocketAddress address, 
            RequestMessage request, long time) {
        return new DHTTimeoutException(nodeId, address, request, time);
    }
}
