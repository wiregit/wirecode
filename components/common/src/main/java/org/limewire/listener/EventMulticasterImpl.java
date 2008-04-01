package org.limewire.listener;

/**
 * An implementation of an event multicaster.
 * This forwards all received events all any listener.
 */
public class EventMulticasterImpl<E> implements EventMulticaster<E> {

    private final EventListenerList<E> listeners = new EventListenerList<E>();
    
    public void handleEvent(E event) {
        listeners.broadcast(event);
    }

    public void addListener(EventListener<E> eventListener) {
        listeners.addListener(eventListener);
    }

    public boolean removeListener(EventListener<E> eventListener) {
        return listeners.removeListener(eventListener);        
    }
    
    
}
