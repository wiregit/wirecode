package org.limewire.mojito.concurrent;

/**
 * 
 */
public interface AsyncProcess<V> {

    /**
     * Starts the {@link AsyncProcess}
     */
    public void start(DHTFuture<V> future);
}
