package org.limewire.nio.observer;

/**
 * Marks the class as being able to be shutdown.
 *
 * This should release any resources acquired as well as propogate
 * the shutting down to any components that also need to be shutdown.
 */
public interface Shutdownable {
    
    /**
     * Releases any resources used by this component.
     *
     * No exception should ever be thrown.
     */
    void shutdown();
    
}