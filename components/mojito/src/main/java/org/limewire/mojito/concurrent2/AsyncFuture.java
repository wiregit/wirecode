package org.limewire.mojito.concurrent2;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.Cancellable;

/**
 * 
 */
public interface AsyncFuture<V> extends Future<V>, Cancellable {

    /**
     * 
     */
    public void setValue(V value);
    
    /**
     * 
     */
    public void setException(Throwable exception);
    
    /**
     * 
     */
    public long getTimeout(TimeUnit unit);
    
    /**
     * 
     */
    public long getTimeoutInMillis();
    
    /**
     * 
     */
    public boolean isTimeout();
    
    /**
     * 
     */
    public boolean isCompletedAbnormally();
    
    /**
     * 
     */
    public void addAsyncFutureListener(AsyncFutureListener<V> l);
    
    /**
     * 
     */
    public void removeAsyncFutureListener(AsyncFutureListener<V> l);
    
    /**
     * 
     */
    public AsyncFutureListener<V>[] getAsyncFutureListeners();
}
