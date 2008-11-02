package org.limewire.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** A future that is designed to return what is passed into its constructor. */
public class SimpleFuture<T> implements Future<T> {
    
    private final T t;
    private volatile Throwable exception;
    
    public SimpleFuture(T t) {
        this.t = t;
    }
    
    /** Sets an exception that will be thrown when get is called. */
    public void setException(Throwable t) {
        this.exception = t;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() throws ExecutionException {
        if(exception != null) {
            throw new ExecutionException(exception);
        } else {
            return t;
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        if(exception != null) {
            throw new ExecutionException(exception);
        } else {
            return t;
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

}
