package org.limewire.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

/**
 * Delegates from one ListeningFuture to another, converting from the Source
 * type to the Result type.
 */
public abstract class ListeningFutureDelegator<S, R> implements ListeningFuture<R> {
    
    private final ListeningFuture<S> delegate;
    
    public ListeningFutureDelegator(ListeningFuture<S> delegate) {
        this.delegate = delegate;
    }

    public void addFutureListener(final EventListener<FutureEvent<R>> listener) {
        delegate.addFutureListener(new EventListener<FutureEvent<S>>() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleEvent(FutureEvent<S> event) {
                FutureEvent<R> newEvent;
                switch(event.getType()) {
                case SUCCESS:
                    try {
                        newEvent = FutureEvent.createSuccess(convertSource(event.getResult()));
                    } catch(ExecutionException ee) {
                        newEvent = FutureEvent.createException(ee);
                    }
                    break;
                default:
                    // It's ok to erase because the type is unused.
                    newEvent = (FutureEvent<R>)event;
                }
                EventListenerList.dispatch(listener, newEvent);
            }
        });
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return delegate.cancel(mayInterruptIfRunning);
    }

    public R get() throws InterruptedException, ExecutionException {
        return convertSource(delegate.get());
    }

    public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        return convertSource(delegate.get(timeout, unit));
    }

    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    public boolean isDone() {
        return delegate.isDone();
    }
    
    /** Converts from S to R. If it cannot be converted, throws an ExecutionException. */
    protected abstract R convertSource(S source) throws ExecutionException;

}
