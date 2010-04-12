package org.limewire.mojito.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.RunnableListeningFuture;
import org.limewire.mojito.Context;

/**
 * 
 */
public class DHTFutureTask<V> extends DHTValueFuture<V> 
        implements RunnableListeningFuture<V> {

    private static final ScheduledThreadPoolExecutor WATCHDOG 
        = ExecutorUtils.newSingleThreadScheduledExecutor("WatchdogThread");
    
    private final Context context;
    
    private final DHTTask<V> task;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * 
     */
    public DHTFutureTask(final Context context, DHTTask<V> task) {
        this.context = context;
        this.task = task;
        
        this.timeout = task.getWaitOnLockTimeout();
        this.unit = TimeUnit.MILLISECONDS;
    }
    
    public Context getContect() {
        return context;
    }
    
    @Override
    public synchronized void run() {
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
    
    /**
     * Returns the timeout of the watchdog in the given {@link TimeUnit}
     */
    public long getTimeout(TimeUnit unit) {
        return unit.convert(timeout, this.unit);
    }
    
    /**
     * Returns the timeout of the watchdog in milliseconds
     */
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns true if the {@link DHTFuture} completed due to a timeout
     */
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
