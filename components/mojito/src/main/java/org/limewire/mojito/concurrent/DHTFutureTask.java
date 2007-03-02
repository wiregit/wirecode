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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.mojito.Context;
import org.limewire.mojito.exceptions.LockTimeoutException;
import org.limewire.mojito.util.OnewayExchanger;

/**
 * 
 */
public class DHTFutureTask<T> implements Runnable, DHTTask.Callback<T>, DHTFuture<T>, Cancellable {
    
    private final OnewayExchanger<T, ExecutionException> exchanger
        = new OnewayExchanger<T, ExecutionException>(true);
    
    private final Set<DHTFutureListener<T>> listeners 
        = new LinkedHashSet<DHTFutureListener<T>>();
    
    private final Context context;
    
    private final DHTTask<T> task;
    
    private boolean running = false;
    
    private ScheduledFuture<?> watchdog = null;
    
    public DHTFutureTask(Context context, DHTTask<T> task) {
        this.task = task;
        this.context = context;
    }
    
    public void run() {
        try {
            synchronized (exchanger) {
                if (!exchanger.isDone()) {
                    task.start(this);
                    running = true;
                    
                    if (!exchanger.isDone()) {
                        initWatchdog();
                    }
                }
            }
        } catch (Throwable t) {
            setException(t);
        }
    }

    private void initWatchdog() {
        Runnable r = new Runnable() {
            public void run() {
                synchronized (exchanger) {
                    if (!exchanger.isDone()) {
                        setException(new LockTimeoutException(task.toString()));
                    }
                }
            }
        };
    
        watchdog = context.getDHTExecutorService().schedule(
                r, task.getLockTimeout(), TimeUnit.MILLISECONDS);
    }
    
    protected void done() {
        
    }
    
    private void internalDone() {
        done();
        
        Runnable r = new Runnable() {
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
        
        context.getDHTExecutorService().execute(r);
    }
    
    public void setReturnValue(T value) {
        boolean done = false;
        synchronized (exchanger) {
            if (!exchanger.isDone()) {
                exchanger.setValue(value);
                if (watchdog != null) {
                    watchdog.cancel(true);
                    watchdog = null;
                }
                done = true;
            }
        }
        
        if (done) {
            internalDone();
        }
    }
    
    public void setException(Throwable t) {
        boolean done = false;
        synchronized (exchanger) {
            if (!exchanger.isDone()) {
                exchanger.setException(new ExecutionException(t));
                if (watchdog != null) {
                    watchdog.cancel(true);
                    watchdog = null;
                }
                done = true;
            }
        }
        
        if (done) {
            internalDone();
        }
    }
    
    public void addDHTFutureListener(final DHTFutureListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("DHTFutureListener is null");
        }
        
        boolean done = false;
        synchronized (listeners) {
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

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean done = false;
        synchronized (exchanger) {
            if (!exchanger.isDone()) {
                if (!running || mayInterruptIfRunning) {
                    exchanger.cancel();
                    task.cancel();
                    if (watchdog != null) {
                        watchdog.cancel(true);
                        watchdog = null;
                    }
                    done = true;
                }
            }
        }
        
        if (done) {
            internalDone();
        }
        
        return done;
    }

    public boolean isCancelled() {
        return exchanger.isCancelled();
    }

    public boolean isDone() {
        return exchanger.isDone();
    }
    
    public T get() throws InterruptedException, ExecutionException {
        return exchanger.get();
    }

    public T get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return exchanger.get(timeout, unit);
    }
    
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
        for (DHTFutureListener<T> l : listeners()) {
            l.handleInterruptedException(e);
        }
    }
}
