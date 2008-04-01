package org.limewire.listener;

/**
 * Describes an interface to allow objects to add and remove listeners for
 * certain events.
 */
public interface ListenerSupport<E> {

    public void addListener(EventListener<E> listener);

    public boolean removeListener(EventListener<E> listener);

}
