package org.limewire.nio.observer;

/**
 * Interface that combines ReadObserver & WriteObserver, to allow
 * one object to be passed around and marked as supporting both
 * read handling events & write handling events.
 */
public interface ReadWriteObserver extends ReadObserver, WriteObserver {}
    
    