package org.limewire.listener;

/**
 * An implementation of an event multicaster.
 * This forwards all received events all any listener.
 */
public class EventMulticasterImpl<E extends Event> implements EventMulticaster<E> {

    private final WeakEventListenerList<E> listeners = new WeakEventListenerList<E>();
    
    public void handleEvent(E event) {
        listeners.broadcast(event);
    }

    public void addListener(Object strongRef, EventListener<E> eventListener) {
        listeners.addListener(strongRef, eventListener);
    }

    public boolean removeListener(Object strongRef, EventListener<E> eventListener) {
        return listeners.removeListener(strongRef, eventListener);        
    }
    
    
}
