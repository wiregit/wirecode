package org.limewire.mojito.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * An asynchronous process that is started by a {@link DHTFuture}.
 */
public interface DHTFutureProcess<V> {

    /**
     * Starts the {@link DHTFutureProcess}
     */
    public void start(DHTFuture<V> future);
    
    /**
     * A mix-in interface for {@link DHTFutureProcess}es.
     */
    public static interface Delay {
        
        /**
         * The delay for which the watchdog should be postponed.
         */
        public long getDelay(TimeUnit unit);
    }
}
