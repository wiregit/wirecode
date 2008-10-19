package org.limewire.listener;

/**
 * A default, simple implementation of Event.
 */
public class DefaultEvent<S, E> extends AbstractSourcedEvent<S> implements Event<S, E> {
    
    private final E event;
    
    public DefaultEvent(S source, E event) {
        super(source);
        this.event = event;
    }

    public E getType() {
        return event;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(": ");
        builder.append("source: ").append(getSource());
        builder.append(", type: ").append(event);
        return builder.toString();
    }
}
