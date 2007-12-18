package org.limewire.listener;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;
import com.limegroup.gnutella.gui.privategroups.PrivateGroupsMessageWindow;


/**
 * Maintains event listeners and broadcasts events to all listeners.
 * Listeners are kept by weak references to a key value, allowing anonymous
 * listeners to be used while still allowing the list to time out.
 */
// TODO: This isn't synchronized right now, which is bad -- listener list should really be CoW,
//       or a ConcurrentMap.  (See Google Collection's ReferenceMap.)
public class  WeakEventListenerList<E extends Event> implements WeakEventListenerSupport<E> {
    
//    private  final Map<Object, List<EventListener<E>>> listenerMap;
    private  final ReferenceMap<Object, List<EventListener<E>>> listenerMap;
    private static final Log LOG = LogFactory.getLog(PrivateGroupsMessageWindow.class);

    public WeakEventListenerList() {
        
        this.listenerMap = new ReferenceMap(ReferenceType.WEAK, ReferenceType.STRONG, new ConcurrentHashMap<Object, List<EventListener<E>>>());
//        this.listenerMap = new WeakHashMap<Object, List<EventListener<E>>>();
    }
    
    /** Adds the listener. */
    public void addListener(Object strongRef, EventListener<E> listener) {
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        LOG.debug("in WeakHashMap addListener");
        if(listeners == null){
            LOG.debug("no existing listeners list.  need to create a new list");
            listeners = new CopyOnWriteArrayList<EventListener<E>>();
            listenerMap.put(strongRef, listeners);
            
        }
        listeners.add(listener);
        System.out.println("STREF: " + strongRef);
        System.out.println("HELLO: " + listenerMap.isEmpty());
        System.out.println("values: " + listenerMap.values());
        LOG.debug("after put value in the map");
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(Object strongRef, EventListener<E> listener) {
        
        System.out.println("REMOVE LISTENER: " + strongRef);
        System.out.println("HELLO: " + listenerMap.isEmpty());
        System.out.println("values: " + listenerMap.values());
        
        List<EventListener<E>> listeners = listenerMap.get(strongRef);
        if(listeners != null) {
            System.out.println("HELLO: " + listeners.isEmpty());
            System.out.println("values: " + listeners);
            
            listeners.remove(listener);
            System.out.println("values after remove: " + listeners);
            
            if(listeners.isEmpty())
                listenerMap.remove(strongRef);
            return true;
        }
        return false;
    }
    
    /** Broadcasts an event to all listeners. */
    public void broadcast(E event) {
        LOG.debug("in WeakHashMap broadcast");
        System.out.println("HELLO AGAIN: " + listenerMap.isEmpty());
        System.out.println("HELLO AGAIN2: " + listenerMap.isEmpty());
        System.out.println("LISTENER VALUES: " + listenerMap.values());
        for(List<EventListener<E>> listenerList : listenerMap.values()) {
           
            LOG.debug("for each list in listenerMap");
            if(listenerList != null) {
                for(EventListener<E> listener : listenerList) {
                    listener.handleEvent(event);
                }
            }
        }
    }
}
