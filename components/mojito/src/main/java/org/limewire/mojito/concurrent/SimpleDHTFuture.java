package org.limewire.mojito.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.OnewayExchanger;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventListenerList.EventListenerListContext;

/**
 * 
 */
public class SimpleDHTFuture<V> implements DHTFuture2<V> {

    private final AtomicReference<EventListenerList<FutureEvent<V>>> listenersRef 
        = new AtomicReference<EventListenerList<FutureEvent<V>>>(
            new EventListenerList<FutureEvent<V>>());
    
    // The listenerContext is required to make sure that listeners are 
    // notified in the correct threads.  We eagerly clear the listenerRef 
    // to release old listeners, but need to keep the context around to 
    // make sure future listeners reuse the context.
    private final EventListenerListContext listenerContext 
        = listenersRef.get().getContext();
    
    private final OnewayExchanger<V, ExecutionException> exchanger 
        = new OnewayExchanger<V, ExecutionException>(this, true);
    
    public SimpleDHTFuture() {
    }
    
    public SimpleDHTFuture(V value) {
        setValue(value);
    }
    
    public SimpleDHTFuture(Throwable exception) {
        setException(exception);
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
        boolean success = exchanger.setException(wrap(exception));
        
        if (success) {
            complete();
        }
        
        return success;
    }
    
    private static ExecutionException wrap(Throwable t) {
        if (t instanceof ExecutionException) {
            return (ExecutionException)t;
        }
        
        return new ExecutionException(t);
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
    public boolean isCompletedAbnormally() {
        return exchanger.throwsException();
    }

    /**
     * 
     */
    private synchronized void complete() {
        // Fire the event
        EventListenerList<FutureEvent<V>> listeners 
            = listenersRef.getAndSet(null);
        assert listeners != null;

        if (listeners.size() > 0) {
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
