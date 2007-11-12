package org.limewire.listener;

/**
 * A listener for a given kind of event.
 */
public interface EventListener<E extends Event> {
    
    public void handleEvent(E event);

}
