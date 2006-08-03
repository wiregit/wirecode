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
 
package com.limegroup.mojito.handler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.event.DHTException;
import com.limegroup.mojito.messages.RequestMessage;
import com.limegroup.mojito.messages.ResponseMessage;
import com.limegroup.mojito.settings.NetworkSettings;
import com.limegroup.mojito.util.ContactUtils;

/**
 * An abstract base class for ResponseHandlers
 */
public abstract class AbstractResponseHandler<V> implements ResponseHandler, Callable<V> {
    
    private static final Log LOG = LogFactory.getLog(AbstractResponseHandler.class);
    
    /** The number of errors that have occured */
    private int errors = 0;
    
    /** The total time that has elapsed since the request was sent */
    private long time;
    
    /** The timeout of this handler */
    private long timeout;
    
    /** The maximum number of errors that may occur */
    private int maxErrors;
    
    /** LOCK */
    private final Object lock = new Object();
    
    /** Whether or not this handler has been started */
    private volatile boolean started = false;
    
    /** Whether or not this handler has finished */
    private volatile boolean finished = false;
    
    /** Whether or not this handler was cancelled */
    private volatile boolean cancelled = false;
    
    /** Whether or not this handler is done */
    private volatile boolean done = false;
    
    /** The return value */
    private volatile V value = null;
    
    /** The Exception we may throw */
    private volatile Exception ex = null;
    
    /** A handle to Context */
    protected final Context context;
    
    public AbstractResponseHandler(Context context) {
        this(context, -1L, -1);
    }
    
    public AbstractResponseHandler(Context context, long timeout) {
        this(context, timeout, -1);
    }
    
    public AbstractResponseHandler(Context context, int maxErrors) {
        this(context, -1L, maxErrors);
    }
    
    public AbstractResponseHandler(Context context, long timeout, int maxErrors) {
        this.context = context;
        
        if (timeout < 0L) {
            this.timeout = NetworkSettings.TIMEOUT.getValue();
        } else {
            this.timeout = timeout;
        }
        
        if (maxErrors < 0) {
            this.maxErrors = NetworkSettings.MAX_ERRORS.getValue();
        } else {
            this.maxErrors = maxErrors;
        }
    }
    
    /**
     * Is called by call()
     */
    protected void start() throws Exception {
    }
    
    /**
     * Is called by call()
     */
    protected void finish() {
        
    }
    
    /**
     * Sets the timeout of this handler
     */
    protected void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long timeout() {
        return timeout;
    }
    
    /**
     * Increments the gobal time by time
     */
    protected void addTime(long time) {
        this.time += time;
    }
    
    public long time() {
        return time;
    }
    
    /**
     * Resets the error counter
     */
    protected void resetErrors() {
        errors = 0;
    }
    
    /**
     * Returns the number of errors
     */
    protected int getErrors() {
        return errors;
    }
    
    /**
     * Sets the maximum number of errors that
     * may occur before we're giving up
     */
    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }
    
    /**
     * Returns the maximum number of errors that
     * may occur
     */
    public int getMaxErrors() {
        return maxErrors;
    }
    
    /**
     * See handleResponse()
     */
    protected abstract void response(ResponseMessage message, long time) throws IOException;
    
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        
        if (isCancelled() || isDone()) {
            return;
        }
        
        addTime(time);
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received " + response + " from " + response.getContact() 
                    + " after " + getErrors() + " errors and a total time of " + time() + "ms");
        }
        
        response(response, time);
    }

    /**
     * See handleTimeout()
     */
    protected abstract void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException;
    
    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage request, long time) throws IOException {
        
        if (isCancelled() || isDone()) {
            return;
        }
        
        addTime(time);
        
        if (LOG.isTraceEnabled()) {
            LOG.trace(request + " to " + ContactUtils.toString(nodeId, dst) 
                    + " failed after " + time + "ms");
        }
        
        // maxErrors is actually the total number of
        // retries. Since the first try failed there
        // are maxErrors-1 retries left.
        if (errors++ >= (maxErrors-1)) {
            timeout(nodeId, dst, request, time());
        } else {
            resend(nodeId, dst, request);
        }
    }
    
    /**
     * Resends the given Message to nodeId/dst
     */
    protected void resend(KUID nodeId, SocketAddress dst, RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending " + message + " to " + ContactUtils.toString(nodeId, dst));
        }
        
        context.getMessageDispatcher().send(nodeId, dst, message, this);
    }
    
    /**
     * See handleError()
     */
    protected abstract void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e);
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        
        if (isCancelled() || isDone()) {
            return;
        }
        
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a " + message + " to " + ContactUtils.toString(nodeId, dst) + " failed", e);
        }
        
        error(nodeId, dst, message, e);
    }
    
    public boolean isCancelled() {
        return cancelled;            
    }
    
    /**
     * Returns whether or not this handler is done which
     * means if it has returnded a result or threw an
     * Exception
     */
    protected boolean isDone() {
        return done;
    }
    
    /**
     * Sets the return value which will be returned by the 
     * call() method
     */
    protected void setReturnValue(V value) {
        synchronized (lock) {
            if (cancelled) {
                return;
            }
            
            if (done) {
                throw new IllegalStateException();
            }
            
            done = true;
            this.value = value;
            lock.notifyAll();
        }
    }
    
    /**
     * Sets the Exception which will be thrown by the
     * call() method
     */
    protected void setException(Exception ex) {
        synchronized (lock) {
            if (cancelled) {
                return;
            }
            
            if (done) {
                throw new IllegalStateException();
            }
            
            done = true;
            this.ex = ex;
            lock.notifyAll();
        }
    }
    
    public V call() throws Exception {
        try {
            synchronized (lock) {
                if (!started) {
                    started = true;
                    start();
                }
                
                if (!done && !cancelled) {
                    lock.wait();
                }
    
                if (!done) {
                    cancelled = true;
                }
                
                if (cancelled && ex == null) {
                    ex = new CancellationException();
                }
                
                if (ex != null) {
                    throw ex;
                }
                
                return value;
            }
        } catch (InterruptedException err) {
            cancelled = true;
            ex = err;
            cancelled();
            throw err;
        } finally {
            if (!finished) {
                finished = true;
                finish();
            }
        }
    }

    /**
     * Called if this handler was cancelled externally (interrupted)
     */
    protected void cancelled() throws Exception {
    }
    
    /**
     * A helper method to throw Timeout Exceptions
     */
    protected void fireTimeoutException(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
        TimeoutException timeout = new TimeoutException(
                ContactUtils.toString(nodeId, address) + " timed out after " + time + "ms");
        
        setException(new DHTException(nodeId, address, request, time, timeout));
    }
}
