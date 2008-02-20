package org.limewire.listener;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 * Listeners are kept by weak references to a key value, allowing anonymous
 * listeners to be used while still allowing the list to time out.
 * @param <E>
 */
public class WeakEventListenerList<E> implements WeakEventListenerSupport<E> {
    
    private final ConcurrentMap<Object, List<EventListener<E>>> listenerMap = new ReferenceMap<Object, List<EventListener<E>>>(ReferenceType.WEAK, ReferenceType.STRONG);

    /** Adds the listener. */
    public void addListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> newListenerList = new CopyOnWriteArrayList<EventListener<E>>();
        List<EventListener<E>> oldlistenerList = listenerMap.putIfAbsent(strongRef, newListenerList);
        if (oldlistenerList != null) {
            oldlistenerList.add(listener);
        } else {
            newListenerList.add(listener);
        }
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if(listeners != null) {
            return listeners.remove(listener);
            // we can't remove the list from the map without synchronizing on it, since we would need
            // to find out if it's empty and then remove it while ensuring no other thread adds
            // a listener to it in the meantime.
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
