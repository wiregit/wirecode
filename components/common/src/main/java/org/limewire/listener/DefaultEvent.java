package org.limewire.listener;

/**
 * A default, simple implementation of Event.
 */
public class DefaultEvent<T, E extends Enum> implements Event<T, E> {
    
    private final T source;
    private final E event;
    
    public DefaultEvent(T source, E event) {
        this.source = source;
        this.event = event;
    }

    public T getSource() {
        return source;
    }

    public E getType() {
        return event;
    }

}
