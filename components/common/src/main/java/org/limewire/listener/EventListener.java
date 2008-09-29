package org.limewire.listener;

/**
 * A listener for a given kind of event.
 */
public interface EventListener<E> {
    
    // DO NOT CHANGE THIS METHOD NAME WITHOUT CHANGING EventListenerList's annotation inspection
    public void handleEvent(E event); 

}
