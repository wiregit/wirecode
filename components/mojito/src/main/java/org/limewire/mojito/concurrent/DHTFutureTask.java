package org.limewire.mojito.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.concurrent.AsyncFutureTask;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFutureProcess.Delay;
import org.limewire.mojito.util.EventUtils;
import org.limewire.mojito.util.SchedulingUtils;

/**
 * {@link DHTFutureTask}s have a built-in watchdog {@link Thread} that 
 * interrupts all waiting {@link Thread}s after a predefined period of
 * time.
 * 
 * @see AsyncFutureTask
 */
public class DHTFutureTask<V> extends AsyncFutureTask<V> implements DHTFuture<V> {
    
    private final DHTFutureProcess<V> task;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * Creates an {@link DHTFutureTask}
     */
    public DHTFutureTask(DHTFutureProcess<V> task, long timeout, TimeUnit unit) {
        
        this.task = task;
        
        this.timeout = timeout;
        this.unit = unit;
    }
    
    @Override
    protected synchronized void doRun() {
        if (!isDone()) {
            watchdog(timeout, unit);
            start();
        }
    }
    
    /**
     * Starts the {@link DHTTask}
     */
    protected synchronized void start() {
        task.start(this);
    }
    
    /**
     * Starts the watchdog task
     */
    private synchronized boolean watchdog(long timeout, TimeUnit unit) {
        if (timeout < 0L || isDone()) {
            return false;
        }
        
        Runnable task = new Runnable() {
            
            private final long creationTime = System.currentTimeMillis();
            
            @Override
            public void run() {
                synchronized (DHTFutureTask.this) {
                    if (!isDone() && !isDelay()) {
                        wasTimeout = true;
                        
                        long time = System.currentTimeMillis() - creationTime;
                        handleTimeout(time, TimeUnit.MILLISECONDS);
                    }
                }
            }
        };
        
        watchdog = SchedulingUtils.schedule(task, timeout, unit);
        return true;
    }
    
    /**
     * Returns true if the watchdog was delayed.
     */
    private boolean isDelay() {
        long delay = getDelay(unit);
        return watchdog(delay, unit);
    }
    
    /**
     * Returns the watchdog delay.
     */
    protected long getDelay(TimeUnit unit) {
        if (task instanceof Delay) {
            return ((Delay)task).getDelay(unit);
        }
        
        return -1L;
    }
    
    /**
     * 
     */
    protected synchronized void handleTimeout(long time, TimeUnit unit) {
        setException(new TimeoutException("Watchdog: " + time + " " + unit));
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

    @Override
    protected boolean isEventThread() {
        return EventUtils.isEventThread();
    }
    
    @Override
    protected void fireOperationComplete(
            final EventListener<FutureEvent<V>>[] listeners,
            final FutureEvent<V> event) {
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                DHTFutureTask.super.fireOperationComplete(listeners, event);
            }
        };
        
        EventUtils.fireEvent(task);
    }
}
