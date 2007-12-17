package org.limewire.listener;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;


/**
 * Maintains event listeners and broadcasts events to all listeners.
 * Listeners are kept by weak references to a key value, allowing anonymous
 * listeners to be used while still allowing the list to time out.
 */
// TODO: This isn't synchronized right now, which is bad -- listener list should really be CoW,
//       or a ConcurrentMap.  (See Google Collection's ReferenceMap.)
public class  WeakEventListenerList<E extends Event> implements WeakEventListenerSupport<E> {
    
//    private  final Map<Object, List<EventListener<E>>> listenerMap;
    private  final ReferenceMap listenerMap;

    public WeakEventListenerList() {
        
        this.listenerMap = new ReferenceMap(ReferenceType.WEAK, ReferenceType.WEAK, new ConcurrentHashMap<Object, List<EventListener<E>>>());
//        this.listenerMap = new WeakHashMap<Object, List<EventListener<E>>>();
    }
    
    /** Adds the listener. */
    public void addListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = (List) listenerMap.get(strongRef);
        if(listeners == null){
            listeners = new CopyOnWriteArrayList<EventListener<E>>();
            listenerMap.put(strongRef, listeners);
        }
        listeners.add(listener);
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = (List)listenerMap.get(strongRef);
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
        Collection col = listenerMap.values();
        Iterator i = col.iterator();
        while(i.hasNext()){
            List<EventListener<E>> listenerList = (List) i.next();
            
            
            if(listenerList != null) {
                for(EventListener<E> listener : listenerList) {
                    listener.handleEvent(event);
                }
            }
        }
        
//        for(List<EventListener<E>> listenerList : i) {
//            if(listenerList != null) {
//                for(EventListener<E> listener : listenerList) {
//                    listener.handleEvent(event);
//                }
//            }
//        }
    }
}
