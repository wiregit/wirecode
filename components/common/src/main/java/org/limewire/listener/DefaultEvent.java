package org.limewire.listener;

/**
 * A default, simple implementation of Event.
 */
public class DefaultEvent<S, E> implements Event<S, E> {
    
    private final S source;
    private final E event;
    
    public DefaultEvent(S source, E event) {
        this.source = source;
        this.event = event;
    }

    public S getSource() {
        return source;
    }

    public E getType() {
        return event;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(": ");
        builder.append("source: ").append(source);
        builder.append(", type: ").append(event);
        return builder.toString();
    }
}
