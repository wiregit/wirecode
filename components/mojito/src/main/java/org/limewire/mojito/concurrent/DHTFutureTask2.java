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
public class DHTFutureTask2<V> extends SimpleDHTFuture<V> 
        implements RunnableListeningFuture<V> {

    private static final ScheduledThreadPoolExecutor WATCHDOG 
        = ExecutorUtils.newSingleThreadScheduledExecutor("AsyncFutureWatchdogThread");
    
    private final Context context;
    
    private final DHTTask<V> task;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * 
     */
    public DHTFutureTask2(final Context context, DHTTask<V> task) {
        this.context = context;
        this.task = task;
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
        if (isDone()) {
            return;
        }
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (DHTFutureTask2.this) {
                    if (!isDone()) {
                        wasTimeout = true;
                        setException(new TimeoutException());
                    }
                }
            }
        };
        
        long timeout = task.getWaitOnLockTimeout();
        watchdog = WATCHDOG.schedule(r, timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    @Override
    protected synchronized final void done() {
        // Cancel the watchdog
        if (watchdog != null) {
            watchdog.cancel(true);
        }
        
        bla();
    }
    
    protected synchronized void bla() {
        // Override
    }
}
