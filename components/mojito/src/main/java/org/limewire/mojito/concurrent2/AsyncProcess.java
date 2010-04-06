package org.limewire.mojito.concurrent2;

/**
 * 
 */
public interface AsyncProcess<V> {

    /**
     * 
     */
    public void start(AsyncFuture<V> future) throws Exception;
    
    /**
     * 
     */
    public void stop(AsyncFuture<V> future) throws Exception;
}
