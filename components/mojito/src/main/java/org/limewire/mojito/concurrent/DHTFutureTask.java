package org.limewire.mojito.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.AsyncFutureTask;
import org.limewire.mojito.Context;

/**
 * 
 */
public class DHTFutureTask<V> extends AsyncFutureTask<V> implements DHTFuture<V> {

    private static final ScheduledThreadPoolExecutor WATCHDOG 
        = ExecutorUtils.newSingleThreadScheduledExecutor("WatchdogThread");
    
    private static Callable<Object> NOP = new Callable<Object>() {
        public Object call() {
            throw new IllegalStateException("Override doRun()");
        }
    };
    
    private final Context context;
    
    private final DHTTask<V> task;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public DHTFutureTask(Context context, DHTTask<V> task) {
        super((Callable<V>)NOP);
        
        this.context = context;
        this.task = task;
        
        // Thrown by some Unit-Tests. We will pass the timeout
        // through the constructor at some point and this is
        // just a temporary fix which doesn't break anything
        // in the production environment.
        long timeout = -1L;
        try {
            timeout = task.getWaitOnLockTimeout();
        } catch (UnsupportedOperationException ignore) {
        }
        
        this.timeout = timeout;
        this.unit = TimeUnit.MILLISECONDS;
    }
    
    public Context getContext() {
        return context;
    }
    
    @Override
    protected synchronized void doRun() {
        if (!isDone()) {
            try {
                start();
                watchdog();
            } catch (Exception err) {
                setException(err);
            }
        }
    }
    
    /**
     * 
     */
    protected synchronized void start() throws Exception {
        task.start(this);
    }
    
    /**
     * 
     */
    private synchronized void watchdog() {
        if (timeout == -1L) {
            return;
        }
        
        if (isDone()) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                synchronized (DHTFutureTask.this) {
                    if (!isDone()) {
                        wasTimeout = true;
                        setException(new TimeoutException());
                    }
                }
            }
        };
        
        watchdog = WATCHDOG.schedule(task, timeout, unit);
    }
    
    @Override
    public long getTimeout(TimeUnit unit) {
        return unit.convert(timeout, this.unit);
    }
    
    @Override
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }
    
    @Override
    public synchronized boolean isTimeout() {
        return wasTimeout;
    }
    
    /**
     * 
     */
    @Override
    protected final void done() {
        synchronized (this) {
            // Cancel the watchdog
            if (watchdog != null) {
                watchdog.cancel(true);
            }
        }
        
        done0();
    }
    
    /**
     * 
     */
    protected void done0() {
        // Override
    }
}
