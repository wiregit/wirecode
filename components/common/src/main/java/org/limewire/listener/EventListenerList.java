package org.limewire.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.util.Objects;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 */
public class EventListenerList<E> implements ListenerSupport<E> {
    
    private final List<EventListener<E>> listenerList = new CopyOnWriteArrayList<EventListener<E>>();

    /** Adds the listener. */
    public void addListener(EventListener<E> listener) {
        listenerList.add(Objects.nonNull(listener, "listener"));
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(EventListener<E> listener) {
        return listenerList.remove(Objects.nonNull(listener, "listener"));
    }
    
    /** Broadcasts an event to all listeners. */
    public void broadcast(E event) {
        Objects.nonNull(event, "event");
        for(EventListener<E> listener : listenerList) {
            listener.handleEvent(event);
        }
    }
}
