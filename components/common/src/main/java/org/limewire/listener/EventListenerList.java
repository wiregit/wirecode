package org.limewire.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 */
public class EventListenerList<E> implements ListenerSupport<E> {
    
    private final Log log;
    
    private final List<EventListener<E>> listenerList = new CopyOnWriteArrayList<EventListener<E>>();

    public EventListenerList() {
        log = null;        
    }
    
    public EventListenerList(Class loggerKey) {
        log = LogFactory.getLog(loggerKey); 
    }
    
    /** Adds the listener. */
    public void addListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("adding listener {0}", listener);
        }
        listenerList.add(Objects.nonNull(listener, "listener"));
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("removing listener {0}", listener);
        }
        return listenerList.remove(Objects.nonNull(listener, "listener"));
    }
    
    /** Broadcasts an event to all listeners. */
    public void broadcast(E event) {
        Objects.nonNull(event, "event");
        if(log != null) {
            log.debugf("broadcasting event {0}", event);
        }
        for(EventListener<E> listener : listenerList) {
            listener.handleEvent(event);
        }
    }
}
