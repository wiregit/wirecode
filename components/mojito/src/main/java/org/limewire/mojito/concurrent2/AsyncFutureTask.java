package org.limewire.mojito.concurrent2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 */
public class AsyncFutureTask<V> implements Runnable, AsyncFuture<V> {

    private static final ScheduledThreadPoolExecutor WATCHDOG 
        = ExecutorUtils.newSingleThreadScheduledExecutor("AsyncFutureWatchdogThread");
    
    private static final AsyncProcess<Object> NOP 
            = new AsyncProcess<Object>() {
        @Override
        public void start(AsyncFuture<Object> future) {
            throw new IllegalStateException("Please override start()");
        }
        
        @Override
        public void stop(AsyncFuture<Object> future) {
        }
    };
    
    private final AsyncExchanger<V, ExecutionException> exchanger 
        = new AsyncExchanger<V, ExecutionException>(this);
    
    private final AsyncProcess<V> process;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
    private List<AsyncFutureListener<V>> beforeListeners = null;
    
    private List<AsyncFutureListener<V>> afterListeners = null;
    
    private ScheduledFuture<?> watchdog;
    
    private boolean wasTimeout = false;
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public AsyncFutureTask(long timeout, TimeUnit unit) {
        this((AsyncProcess<V>)NOP, timeout, unit);
    }
    
    /**
     * 
     */
    public AsyncFutureTask(AsyncProcess<V> process, long timeout, TimeUnit unit) {
        if (process == null) {
            throw new NullArgumentException("process");
        }
        
        if (timeout < 0L && timeout != -1L) {
            throw new IllegalArgumentException("timeout=" + timeout);
        }
        
        if (unit == null) {
            throw new NullArgumentException("unit");
        }
        
        this.process = process;
        this.timeout = timeout;
        this.unit = unit;
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
    public synchronized void run() {
        try {
            start();
            watchdog();
        } catch (Exception err) {
            setException(err);
        }
    }
    
    /**
     * 
     */
    protected synchronized void start() throws Exception {
        process.start(this);
    }
    
    /**
     * 
     */
    protected synchronized void stop() throws Exception {
        process.stop(this);
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
                synchronized (AsyncFutureTask.this) {
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
    public synchronized void setValue(V value) {
        boolean success = exchanger.setValue(value);
        
        if (success) {
            complete();
        }
    }
    
    @Override
    public synchronized void setException(Throwable exception) {
        boolean success = exchanger.setException(new ExecutionException(exception));
        
        if (success) {
            complete();
        }
    }
    
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = exchanger.cancel();
        
        if (success) {
            complete();
        }
        
        return success;
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
        checkIfEventThread();
        return exchanger.get();
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) 
        throws InterruptedException, ExecutionException,
            TimeoutException {
        checkIfEventThread();
        return exchanger.get(timeout, unit);
    }

    @Override
    public synchronized boolean isCancelled() {
        return exchanger.isCancelled();
    }

    @Override
    public synchronized boolean isDone() {
        return exchanger.isDone();
    }
    
    @Override
    public synchronized boolean isCompletedAbnormally() {
        return exchanger.isCompletedAbnormally();
    }

    @Override
    public synchronized boolean isTimeout() {
        return wasTimeout;
    }
    
    /**
     * 
     */
    private synchronized void complete() {
        
        if (watchdog != null) {
            watchdog.cancel(true);
        }
        
        try {
            stop();
        } catch (Exception err) {
            ExceptionUtils.exceptionCaught(err);
        }
        
        done();
        fireOperationComplete();
    }
    
    /**
     * 
     */
    protected synchronized void done() {
        
    }

    @Override
    public synchronized void addAsyncFutureListener(final AsyncFutureListener<V> l) {
        if (l == null) {
            throw new NullArgumentException("l");
        }
        
        if (!isDone()) {
            if (beforeListeners == null) {
                beforeListeners = new ArrayList<AsyncFutureListener<V>>();
            }
            beforeListeners.add(l);
            return;
            
        }
        
        if (afterListeners == null) {
            afterListeners = new ArrayList<AsyncFutureListener<V>>();
        }
        
        afterListeners.add(l);
        
        Runnable event = new Runnable() {
            @Override
            public void run() {
                l.operationComplete(AsyncFutureTask.this);
            }
        };
        
        EventUtils.fireEvent(event);
    }

    @Override
    public synchronized void removeAsyncFutureListener(AsyncFutureListener<V> l) {
        if (l == null) {
            throw new NullPointerException("l");
        }
        
        boolean success = false;
        if (beforeListeners != null) {
            success = beforeListeners.remove(l);
        }
        
        if (!success && afterListeners != null) {
            afterListeners.remove(l);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public synchronized AsyncFutureListener<V>[] getAsyncFutureListeners() {
        List<AsyncFutureListener<V>> copy 
            = new ArrayList<AsyncFutureListener<V>>();
        if (beforeListeners != null) {
            copy.addAll(beforeListeners);
        }
        
        if (afterListeners != null) {
            copy.addAll(afterListeners);
        }
        
        return copy.toArray(new AsyncFutureListener[0]);
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    private AsyncFutureListener<V>[] getBeforeAsyncFutureListeners() {
        if (beforeListeners != null) {
            return beforeListeners.toArray(new AsyncFutureListener[0]);
        }
        
        return null;
    }
    
    /**
     * 
     */
    protected synchronized void fireOperationComplete() {
        final AsyncFutureListener<V>[] listeners 
            = getBeforeAsyncFutureListeners();
        
        if (listeners != null && listeners.length > 0) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (AsyncFutureListener<V> l : listeners) {
                        l.operationComplete(AsyncFutureTask.this);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
    }
    
    private void checkIfEventThread() throws IllegalStateException {
        if (!isDone() && EventUtils.isEventThread()) {
            throw new IllegalStateException();
        }
    }
}
