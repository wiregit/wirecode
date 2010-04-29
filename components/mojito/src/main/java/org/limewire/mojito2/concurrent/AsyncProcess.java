package org.limewire.mojito2.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 
 */
public interface AsyncProcess<V> {

    /**
     * Starts the {@link AsyncProcess}
     */
    public void start(DHTFuture<V> future);
    
    /**
     * Stops the {@link AsyncProcess}
     */
    public void stop(DHTFuture<V> future);
    
    /**
     * A mix-in interface for {@link AsyncProcess}es.
     */
    public static interface Delay {
        
        /**
         * The delay for which the watchdog should be postponed.
         */
        public long getDelay(TimeUnit unit);
    }
}
