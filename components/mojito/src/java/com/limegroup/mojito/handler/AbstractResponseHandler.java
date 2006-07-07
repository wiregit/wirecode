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
    
    private int errors = 0;
    
    private long time;
    private long timeout;
    
    private int maxErrors;
    
    private final Object lock = new Object();
    
    private volatile boolean cancelled = false;
    
    private volatile boolean done = false;
    
    private volatile V value = null;
    
    private volatile Exception ex = null;
    
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
            this.timeout = NetworkSettings.MAX_TIMEOUT.getValue();
        } else {
            this.timeout = timeout;
        }
        
        if (maxErrors < 0) {
            if (NetworkSettings.USE_RANDOM_MAX_ERRORS.getValue()) {
                // MIN_RETRIES <= x <= MAX_ERRORS
                this.maxErrors = Math.max(NetworkSettings.MIN_RETRIES.getValue(), 
                        (int)(Math.random() * NetworkSettings.MAX_ERRORS.getValue()));
            } else {
                this.maxErrors = NetworkSettings.MAX_ERRORS.getValue();
            }
        } else {
            this.maxErrors = maxErrors;
        }
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    public long timeout() {
        return timeout;
    }
    
    public void addTime(long time) {
        this.time += time;
    }
    
    public long time() {
        return time;
    }
    
    protected void resetErrors() {
        errors = 0;
    }
    
    public int getErrors() {
        return errors;
    }
    
    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }
    
    public int getMaxErrors() {
        return maxErrors;
    }
    
    /**
     * 
     */
    protected abstract void response(ResponseMessage message, long time) throws IOException;
    
    public void handleResponse(ResponseMessage response, long time) throws IOException {
        
        if (isCancelled()) {
            return;
        }
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received " + response + " from " + response.getContact() 
                    + " after " + getErrors() + " errors and a total time of " + time() + "ms");
        }
        
        response(response, time);
    }

    /**
     * 
     */
    protected abstract void timeout(KUID nodeId, SocketAddress dst, RequestMessage message, long time) throws IOException;
    
    public void handleTimeout(KUID nodeId, 
            SocketAddress dst, RequestMessage request, long time) throws IOException {
        
        if (isCancelled()) {
            return;
        }
        
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
     * 
     */
    protected void resend(KUID nodeId, SocketAddress dst, RequestMessage message) throws IOException {
        
        if (LOG.isTraceEnabled()) {
            LOG.trace("Re-sending " + message + " to " + ContactUtils.toString(nodeId, dst));
        }
        
        context.getMessageDispatcher().send(nodeId, dst, message, this);
    }
    
    /**
     * 
     */
    protected abstract void error(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e);
    
    public void handleError(KUID nodeId, SocketAddress dst, RequestMessage message, Exception e) {
        
        if (isCancelled()) {
            return;
        }
        
        if (LOG.isErrorEnabled()) {
            LOG.error("Sending a " + message + " to " + ContactUtils.toString(nodeId, dst) + " failed", e);
        }
        
        error(nodeId, dst, message, e);
    }
    
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized(lock) {
            if (done) {
                return false;
            }
            
            if (cancelled) {
                return true;
            }
            
            cancelled = true;
            lock.notifyAll();
            return true;
        }
    }
    
    public boolean isCancelled() {
        synchronized (lock) {
            return cancelled;            
        }
    }
    
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
        synchronized (lock) {
            if (!done && !cancelled) {
                try {
                    lock.wait();
                } catch (InterruptedException err) {
                    cancelled = true;
                    throw err;
                }
            }

            if (!done) {
                cancelled = true;
            }
            
            if (cancelled) {
                throw new CancellationException();
            }
            
            if (ex != null) {
                throw ex;
            }
            
            return value;
        }
    }

    protected void fireTimeoutException(KUID nodeId, SocketAddress address, RequestMessage request, long time) {
        TimeoutException timeout = new TimeoutException(
                ContactUtils.toString(nodeId, address) + " timed out after " + time + "ms");
        
        setException(new DHTException(nodeId, address, request, time, timeout));
    }
}
