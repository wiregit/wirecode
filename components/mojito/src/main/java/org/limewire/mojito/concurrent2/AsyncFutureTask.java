package org.limewire.mojito.concurrent2;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.RunnableListeningFuture;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;

/**
 * 
 */
public class AsyncFutureTask<V> implements RunnableListeningFuture<V>, AsyncFuture<V> {

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
    
    private final AtomicReference<EventListenerList<FutureEvent<V>>> listenersRef 
        = new AtomicReference<EventListenerList<FutureEvent<V>>>(
            new EventListenerList<FutureEvent<V>>());
    
    // The listenerContext is required to make sure that listeners are 
    // notified in the correct threads.  We eagerly clear the listenerRef 
    // to release old listeners, but need to keep the context around to 
    // make sure future listeners reuse the context.
    private final EventListenerListContext listenerContext 
        = listenersRef.get().getContext();
    
    private final AsyncExchanger<V, ExecutionException> exchanger 
        = new AsyncExchanger<V, ExecutionException>(this);
    
    private final AsyncProcess<V> process;
    
    private final long timeout;
    
    private final TimeUnit unit;
    
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
    public synchronized boolean setValue(V value) {
        boolean success = exchanger.setValue(value);
        
        if (success) {
            complete();
        }
        
        return success;
    }
    
    @Override
    public synchronized boolean setException(Throwable exception) {
        boolean success = exchanger.setException(new ExecutionException(exception));
        
        if (success) {
            complete();
        }
        
        return success;
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
        return exchanger.get();
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit) 
            throws InterruptedException, ExecutionException, TimeoutException {
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
        
        // Cancel the watchdog
        if (watchdog != null) {
            watchdog.cancel(true);
        }
        
        // Stop the AsyncProcess if it isn't already
        try {
            stop();
        } catch (Exception err) {
            ExceptionUtils.exceptionCaught(err);
        }
        
        // Fire the event
        EventListenerList<FutureEvent<V>> listeners 
            = listenersRef.getAndSet(null);
        assert listeners != null;

        if (!listeners.isEmpty()) {
            listeners.broadcast(FutureEvent.createEvent(this));
        }
        
        done();
    }
    
    /**
     * 
     */
    protected synchronized void done() {
        
    }

    @Override
    public void addFutureListener(EventListener<FutureEvent<V>> listener) {
        boolean added = false;
        EventListenerList<FutureEvent<V>> listeners = listenersRef.get();
        // Add the listener & set it back -- we add a proxy listener
        // because there's a chance that we add it to the list
        // before another thread sets it to null, leaving us
        // to potentially call methods on the listener twice.
        // (Once from the done() thread, and once from this thread.)
        if (!isDone() && listeners != null) {
            listeners.addListener(new ProxyListener<V>(listener, listenerContext));
            added = listenersRef.compareAndSet(listeners, listeners);
        }

        if (!added) {
            EventListenerList.dispatch(listener, 
                    FutureEvent.createEvent(this), listenerContext);
        }
    }
    
    private static class ProxyListener<V> implements EventListener<FutureEvent<V>> {
        
        private final AtomicBoolean called = new AtomicBoolean(false);
        private final EventListenerListContext listenerContext;

        private final EventListener<FutureEvent<V>> delegate;

        public ProxyListener(EventListener<FutureEvent<V>> delegate, 
                EventListenerListContext listenerContext) {
            this.delegate = delegate;
            this.listenerContext = listenerContext;
        }

        @Override
        public void handleEvent(FutureEvent<V> event) {
            if (!called.getAndSet(true)) {
                // Dispatch via EventListenerList to support annotations.
                EventListenerList.dispatch(delegate, event, listenerContext);
            }
        }
    }
}
