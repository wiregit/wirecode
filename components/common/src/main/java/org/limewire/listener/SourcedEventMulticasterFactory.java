package org.limewire.listener;

/**
 * A factory that can create multicasters whose events will all have the same
 * source.
 */
public interface SourcedEventMulticasterFactory<E extends SourcedEvent<S>, S> {

    /**
     * Returns a new {@link DisposableEventMulticaster} whose events will all
     * have the same source.
     */
    DisposableEventMulticaster<E> createDisposableMulticaster(S source);
}
