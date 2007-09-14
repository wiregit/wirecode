package org.limewire.nio.observer;

/**
 * Defines the interface that combines <code>ReadObserver</code> and 
 * <code>WriteObserver</code>, to allow one object to be passed around and 
 * marked as supporting both read handling events and write handling events.
 */
public interface ReadWriteObserver extends ReadObserver, WriteObserver {}
    
    