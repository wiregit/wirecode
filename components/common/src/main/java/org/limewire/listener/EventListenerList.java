package org.limewire.listener;

import java.awt.EventQueue;
import java.lang.reflect.Method;
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
    
    private final List<ListenerProxy<E>> listenerList = new CopyOnWriteArrayList<ListenerProxy<E>>();

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
        listenerList.add(new ListenerProxy<E>(Objects.nonNull(listener, "listener")));
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("removing listener {0}", listener);
        }
        Objects.nonNull(listener, "listener");
        
        // Find the proxy, then remove it.
        for(ListenerProxy<E> proxyListener : listenerList) {
            if(proxyListener.delegate.equals(listener)) {
                return listenerList.remove(proxyListener);
            }
        }
        return false;
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
    
    private static final class ListenerProxy<E> implements EventListener<E> {
        private final EventListener<E> delegate;
        private volatile Boolean swingProxy = null;
        
        public ListenerProxy(EventListener<E> delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public void handleEvent(final E event) {
            if(swingProxy == null) {
                swingProxy = analyze(delegate, event);                
            }
            
            if(swingProxy.booleanValue() && !EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        delegate.handleEvent(event);
                    }
                });
            } else {
                delegate.handleEvent(event);
            }
        }
        
        /**
         * Loops through all 'handleEvent' methods whose parameter type can match event's
         * classes & superclasses.  When one is found, see if it is annotated with @SwingProxy.
         */
        private Boolean analyze(EventListener<E> delegate, E event) {
            Class<?> eventClass = event.getClass();
            Method method = null;
            while(eventClass != null) {
                try {
                    method = delegate.getClass().getMethod("handleEvent", eventClass);
                    break;
                } catch (NoSuchMethodException ignored) {
                }
                eventClass = eventClass.getSuperclass();
            }
            
            if(method == null) {
                throw new IllegalStateException("Unable to find method!");
            }

            return method.getAnnotation(SwingEDTEvent.class) != null;
        }
        
    }
}
