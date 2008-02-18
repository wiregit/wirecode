package org.limewire.listener;

/**
 * An implementation of an event multicaster.
 * This forwards all received events all any listener.
 */
public class EventMulticasterImpl<L extends EventListener<E>, E> implements EventMulticaster<L, E> {

    private final WeakEventListenerList<L, E> listeners = new WeakEventListenerList<L, E>();
    
    public void handleEvent(E event) {
        listeners.broadcast(event);
    }

    public void addListener(Object strongRef, L eventListener) {
        listeners.addListener(strongRef, eventListener);
    }

    public boolean removeListener(Object strongRef, L eventListener) {
        return listeners.removeListener(strongRef, eventListener);        
    }
    
    
}
