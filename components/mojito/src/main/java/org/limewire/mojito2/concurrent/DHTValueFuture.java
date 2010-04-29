package org.limewire.mojito2.concurrent;

import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.AsyncValueFuture;

/**
 * 
 */
public class DHTValueFuture<V> extends AsyncValueFuture<V> implements DHTFuture<V> {

    /**
     * Creates a {@link DHTValueFuture}
     */
    public DHTValueFuture() {
    }
    
    /**
     * Creates a {@link DHTValueFuture} with the given value
     */
    public DHTValueFuture(V value) {
        setValue(value);
    }
    
    /**
     * Creates a {@link DHTValueFuture} with the given {@link Throwable}
     */
    public DHTValueFuture(Throwable exception) {
        setException(exception);
    }

    @Override
    public long getTimeout(TimeUnit unit) {
        return 0L;
    }

    @Override
    public long getTimeoutInMillis() {
        return getTimeout(TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isTimeout() {
        return false;
    }
}
