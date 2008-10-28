package org.limewire.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** A future that is designed to return what is passed into its constructor. */
public class SimpleFuture<T> implements Future<T> {
    
    private final T t;
    
    public SimpleFuture(T t) {
        this.t = t;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() {
        return t;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        return t;
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
