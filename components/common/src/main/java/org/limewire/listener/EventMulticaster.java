package org.limewire.listener;

/**
 * Describes the interface for which all incoming events can be forwarded to any registered listeners.
 */
public interface EventMulticaster<L extends EventListener<E>, E extends Event> extends WeakEventListenerSupport<L>, EventListener<E> {

}
