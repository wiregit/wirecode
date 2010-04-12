package org.limewire.mojito.concurrent;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;

/**
 * 
 */
public abstract class DHTFutureAdapter<V> implements EventListener<FutureEvent<V>> {

    @BlockingEvent(queueName="AsyncFuture")
    @Override
    public final void handleEvent(FutureEvent<V> event) {
        operationComplete(event);
    }
    
    /**
     * 
     */
    protected abstract void operationComplete(FutureEvent<V> event);
}
