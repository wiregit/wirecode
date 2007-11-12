package org.limewire.listener;

/**
 * An event for a given source and enum.
 */
public interface Event<T, E extends Enum> {
    
    public T getSource();
    
    public E getType();

}
