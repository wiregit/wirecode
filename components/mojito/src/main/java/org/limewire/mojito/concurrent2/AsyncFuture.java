package org.limewire.mojito.concurrent2;

import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.mojito.concurrent.Cancellable;

/**
 * 
 */
public interface AsyncFuture<V> extends ListeningFuture<V>, Cancellable {

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
}
