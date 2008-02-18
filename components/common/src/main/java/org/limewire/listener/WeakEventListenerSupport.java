package org.limewire.listener;

/**
 * Describes an interface to allow objects to add and remove listeners for
 * certain events. All listeners are added with a reference, allowing listeners
 * who no longer have any live references to be removed from the list. The
 * reference is *not* the listener itself, which allows anonymous classes and
 * one-off instances to be used.
 */
public interface WeakEventListenerSupport<L extends EventListener<?>> {

    public void addListener(Object strongRef, L listener);

    public boolean removeListener(Object strongRef, L listener);

}
