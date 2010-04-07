package org.limewire.mojito.concurrent2;

import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;

/**
 * 
 */
public abstract class AsyncFutureListener<V> implements EventListener<V> {

    @BlockingEvent(queueName="AsyncFuture")
    @Override
    public final void handleEvent(V event) {
        operationComplete(event);
    }
    
    /**
     * 
     */
    protected abstract void operationComplete(V event);
}
