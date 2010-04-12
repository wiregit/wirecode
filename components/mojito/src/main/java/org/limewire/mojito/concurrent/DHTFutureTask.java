package org.limewire.mojito.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.AsyncValueFuture;
import org.limewire.concurrent.RunnableListeningFuture;
import org.limewire.concurrent.AsyncFutureTask.CurrentThread;
import org.limewire.concurrent.AsyncFutureTask.Interruptible;
import org.limewire.mojito.Context;

/**
 * 
 */
public class DHTFutureTask<V> extends AsyncValueFuture<V> 
        implements DHTFuture<V>, RunnableListeningFuture<V> {

    private static final ScheduledThreadPoolExecutor WATCHDOG 
        = ExecutorUtils.newSingleThreadScheduledExecutor("WatchdogThread");
    
    private final AtomicReference<Interruptible> thread 
        = new AtomicReference<Interruptible>(Interruptible.INIT);
    
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
    
    public Context getContext() {
        return context;
    }
    
    @Override
    public void run() {
        if (thread.compareAndSet(Interruptible.INIT, new CurrentThread())) {
            try {
                synchronized (this) {
                    if (!isDone()) {
                        try {
                            start();
                            watchdog();
                        } catch (Exception err) {
                            setException(err);
                        }
                    }
                }
            } finally {
                thread.set(Interruptible.DONE);
            }
        }
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = super.cancel(mayInterruptIfRunning);
        
        if (success && mayInterruptIfRunning) {
            thread.getAndSet(Interruptible.DONE).interrupt();
        }
        
        return success;
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
