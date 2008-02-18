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
public class WeakEventListenerList<L extends EventListener<E>, E> implements WeakEventListenerSupport<L> {
    
    private final ConcurrentMap<Object, List<L>> listenerMap = new ReferenceMap<Object, List<L>>(ReferenceType.WEAK, ReferenceType.STRONG);

    /** Adds the listener. */
    public void addListener(Object strongRef, L listener) {
        List<L> newListenerList = new CopyOnWriteArrayList<L>();
        List<L> oldlistenerList = listenerMap.putIfAbsent(strongRef, newListenerList);
        if (oldlistenerList != null) {
            oldlistenerList.add(listener);
        } else {
            newListenerList.add(listener);
        }
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, L listener) {
        List<L> listeners = listenerMap.get(strongRef);
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
        for(List<L> listenerList : listenerMap.values()) {
            if(listenerList != null) {
                for(L listener : listenerList) {
                    listener.handleEvent(event);
                }
            }
        }
    }
}
