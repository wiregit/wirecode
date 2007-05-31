/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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

package org.limewire.mojito.concurrent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.exceptions.LockTimeoutException;

/**
 * 
 */
public class DHTFutureTask<T> implements Runnable, DHTFuture<T>, Cancellable {
    
    private static final Log LOG = LogFactory.getLog(DHTFutureTask.class);
    
    private final OnewayExchanger<T, ExecutionException> exchanger;
	
    private final Set<DHTFutureListener<T>> listeners 
        = Collections.synchronizedSet(new LinkedHashSet<DHTFutureListener<T>>());
    
    private final Context context;
    
    private final DHTTask<T> task;
    
    private boolean running = false;
    
    private ScheduledFuture<?> watchdog = null;
    
    public DHTFutureTask(final Context context, DHTTask<T> task) {
        this.task = task;
        this.context = context;
        
        exchanger = new OnewayExchanger<T, ExecutionException>(true) {
            @Override
            public synchronized void setValue(T value) {
                if (!isDone()) {
                    /*if (LOG.isDebugEnabled()) {
                        LOG.debug(value);
                    }*/
                    
                    super.setValue(value);
                    internalDone();
                }
            }

            @Override
            public synchronized void setException(ExecutionException exception) {
                if (!isDone()) {
                    /*if (LOG.isDebugEnabled()) {
                        LOG.debug("ExecutionException", exception);
                    }*/
                    
                    super.setException(exception);
                    internalDone();
                }
            }

            @Override
            public synchronized boolean cancel() {
                if (super.cancel()) {
                    internalDone();
                    return true;
                }
                return false;
            }
        };
    }
    
    public void run() {
        try {
            synchronized (exchanger) {
                if (!exchanger.isDone()) {
                    task.start(exchanger);
                    running = true;
                    
                    if (!exchanger.isDone()) {
                        initWatchdog();
                    }
                }
            }
        } catch (Throwable t) {
            exchanger.setException(new ExecutionException(t));
        }
    }

    private void initWatchdog() {
        Runnable r = new Runnable() {
            public void run() {
                synchronized (exchanger) {
                    if (!exchanger.isDone()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Watchdog is canceling " + task);
                        }
                        
                    	task.cancel();
                    	
                        exchanger.setException(
                                new ExecutionException(
                                        new LockTimeoutException(task.toString())));
                    }
                }
            }
        };
    
        watchdog = context.getDHTExecutorService().schedule(
                r, task.getLockTimeout(), TimeUnit.MILLISECONDS);
    }
    
	/**
     * Protected method invoked when this task transitions to state isDone 
     * (whether normally or via cancellation). The default implementation 
     * does nothing. Subclasses may override this method to invoke completion 
     * callbacks or perform bookkeeping. Note that you can query status 
     * inside the implementation of this method to determine whether this 
     * task has been cancelled.
     */
    protected void done() {
        
    }
    
    /**
     * Calls done() and notifies all DHTFutureListeners
     */
    private void internalDone() {
    	
        // NOTE: YOU ARE HOLDING A LOCK ON THE EXCHANGER RIGHT NOW!!!
    	
        if (watchdog != null) {
            watchdog.cancel(true);
            watchdog = null;
        }
    	
        done();
    	
        // Notify the listeners on a different Thread
    	Runnable task = new Runnable() {
            public void run() {
                try {
                    T value = get();
                    fireFutureResult(value);
                } catch (ExecutionException e) {
                    fireExecutionException(e);
                } catch (CancellationException e) {
                    fireCancellationException(e);
                } catch (InterruptedException e) {
                    fireInterruptedException(e);
                }
            }
        };
    	
    	context.getDHTExecutorService().execute(task);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.concurrent.DHTFuture#addDHTFutureListener(org.limewire.mojito.concurrent.DHTFutureListener)
     */
    public void addDHTFutureListener(final DHTFutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("DHTFutureListener is null");
        }
        
        boolean done = false;
        synchronized (exchanger) {
            done = exchanger.isDone();
            if (!done) {
                listeners.add(listener);
            }
        }
        
        if (done) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        T value = get();
                        listener.handleFutureSuccess(value);
                    } catch (ExecutionException e) {
                        listener.handleExecutionException(e);
                    } catch (CancellationException e) {
                        listener.handleCancellationException(e);
                    } catch (InterruptedException e) {
                        listener.handleInterruptedException(e);
                    }
                }
            };
            
            context.getDHTExecutorService().execute(r);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean done = false;
        synchronized (exchanger) {
            if (!exchanger.isDone()) {
                if (!running || mayInterruptIfRunning) {
                    exchanger.cancel();
                    done = true;
                }
            }
        }
        
        if (done) {
            task.cancel();
        }
        
        return done;
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#isCancelled()
     */
    public boolean isCancelled() {
        return exchanger.isCancelled();
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#isDone()
     */
    public boolean isDone() {
        return exchanger.isDone();
    }
    
    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#get()
     */
    public T get() throws InterruptedException, ExecutionException {
        return exchanger.get();
    }

    /*
     * (non-Javadoc)
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    public T get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return exchanger.get(timeout, unit);
    }
    
    /**
     * Returns a copy of all listeners as an Array
     */
    @SuppressWarnings("unchecked")
    private DHTFutureListener<T>[] listeners() {
        synchronized (listeners) {
            return listeners.toArray(new DHTFutureListener[0]);
        }
    }
    
    /**
     * Fires a success event
     */
    protected void fireFutureResult(T value) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleFutureSuccess(value);
        }
    }
    
    /**
     * Fires an ExecutionException event
     */
    protected void fireExecutionException(ExecutionException e) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleExecutionException(e);
        }
    }
    
    /**
     * Fires an CancellationException event
     */
    protected void fireCancellationException(CancellationException e) {
        for (DHTFutureListener<T> l : listeners()) {
            l.handleCancellationException(e);
        }
    }
    
    /**
     * Fires an InterruptedException event
     */
    protected void fireInterruptedException(InterruptedException e) {
        for (DHTFutureListener<T> l : listeners) {
            l.handleInterruptedException(e);
        }
    }
}
