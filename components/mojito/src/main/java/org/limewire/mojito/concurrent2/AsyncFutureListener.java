package org.limewire.mojito.concurrent2;

/**
 * 
 */
public interface AsyncFutureListener<V> {

    /**
     * 
     */
    public void operationComplete(AsyncFuture<V> future);
}
