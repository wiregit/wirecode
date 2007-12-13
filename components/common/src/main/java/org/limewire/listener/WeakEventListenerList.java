package org.limewire.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.limegroup.gnutella.gui.privategroups.PrivateGroupsMessageWindow;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 * Listeners are kept by weak references to a key value, allowing anonymous
 * listeners to be used while still allowing the list to time out.
 */
// TODO: This isn't synchronized right now, which is bad -- listener list should really be CoW,
//       or a ConcurrentMap.  (See Google Collection's ReferenceMap.)
public class  WeakEventListenerList<E extends Event> implements WeakEventListenerSupport<E> {
    
    private  final Map<Object, List<EventListener<E>>> listenerMap;

    public WeakEventListenerList() {
        this.listenerMap = new WeakHashMap<Object, List<EventListener<E>>>();
    }
    
    /** Adds the listener. */
    public void addListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if(listeners == null){
            listeners = new ArrayList<EventListener<E>>();
            listenerMap.put(strongRef, listeners);
        }
        listeners.add(listener);
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if(listeners != null) {
            listeners.remove(listener);
            if(listeners.isEmpty())
                listenerMap.remove(strongRef);
            return true;
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
