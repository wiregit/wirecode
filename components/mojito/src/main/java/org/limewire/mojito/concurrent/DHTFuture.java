package org.limewire.mojito.concurrent;

import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ListeningFuture;

/**
 * 
 */
public interface DHTFuture<V> extends ListeningFuture<V> {

    /**
     * Sets the {@link DHTFuture}'s value and returns true on success
     */
    public boolean setValue(V value);
    
    /**
     * Sets the {@link DHTFuture}'s exception and returns true on success
     */
    public boolean setException(Throwable exception);
    
    /**
     * Returns true if the {@link DHTFuture} completed abnormally 
     * (i.e. {@link #get()} and {@link #get(long, TimeUnit)} will
     * throw an {@link Exception}).
     */
    public boolean isCompletedAbnormally();
}
