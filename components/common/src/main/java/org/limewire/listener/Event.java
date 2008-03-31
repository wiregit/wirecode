package org.limewire.listener;

/**
 * An event for a given source and type.
 */
public interface Event<T, E> {
    
    public T getSource();
    
    public E getType();

}
