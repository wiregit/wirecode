package org.limewire.mojito.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.AsyncFutureTask;
import org.limewire.concurrent.ExecutorsHelper;

/**
 * {@link DHTFutureTask}s have a built-in watchdog {@link Thread} that 
 * interrupts all waiting {@link Thread}s after a predefined period of
 * time.
 * 
 * @see AsyncFutureTask
 */
public class DHTFutureTask<V> extends AsyncFutureTask<V> implements DHTFuture<V> {

    private static final ScheduledExecutorService WATCHDOG 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("WatchdogThread"));
    
    private final AsyncProcess<V> task;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * Creates an {@link DHTFutureTask}
     */
    public DHTFutureTask(AsyncProcess<V> task, long timeout, TimeUnit unit) {
        
        this.task = task;
        
        this.timeout = timeout;
        this.unit = unit;
    }
    
    @Override
    protected synchronized void doRun() {
        if (!isDone()) {
            start();
            watchdog();
        }
    }
    
    /**
     * Starts the {@link DHTTask}
     */
    protected synchronized void start() {
        task.start(this);
    }
    
    /**
     * 
     */
    protected synchronized void stop() {
        task.stop(this);
    }
    
    /**
     * Starts the watchdog task
     */
    private synchronized boolean watchdog() {
        if (timeout == -1L || isDone()) {
            return false;
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
        return true;
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
     * Overridden and declared final to do some critical bookkeeping. 
     * Please override {@link #done0()} in custom implementations.
     */
    @Override
    protected final void done() {
        synchronized (this) {
            // Cancel the watchdog
            if (watchdog != null) {
                watchdog.cancel(true);
            }
            
            stop();
        }
        
        done0();
    }
    
    /**
     * Protected method that is invoked when this {@link DHTFutureTask}
     * completes. You may override this method.
     * 
     * @see #done()
     */
    protected void done0() {
        // Override
    }
}
