package org.limewire.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 * Listeners are kept by weak references to a key value, allowing anonymous
 * listeners to be used while still allowing the list to time out.
 */
public class WeakEventListenerList<E extends Event> implements WeakEventListenerSupport<E> {
    
    private final Map<Object, List<EventListener<E>>> listenerMap = new ReferenceMap<Object, List<EventListener<E>>>(ReferenceType.WEAK, ReferenceType.STRONG);

    /** Adds the listener. */
    public void addListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<EventListener<E>>();
            listenerMap.put(strongRef, listeners);
        }
        listeners.add(listener);
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if(listeners != null) {
            boolean removed = listeners.remove(listener);
            if(listeners.isEmpty())
                listenerMap.remove(strongRef);
            return removed;
        }
        return false;
    }
    
    /** Broadcasts an event to all listeners. */
    public void broadcast(E event) {
        for(List<EventListener<E>> listenerList : listenerMap.values()) {
            if(listenerList != null) {
                for(EventListener<E> listener : listenerList) {
                    listener.handleEvent(event);
                }
            }
        }
    }
    

}
