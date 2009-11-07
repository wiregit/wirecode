package org.limewire.core.api.search.store;

/**
 * Defines a factory for creating StoreConnection instances.
 */
public interface StoreConnectionFactory {

    /**
     * Creates a StoreConnection.
     */
    StoreConnection create();
}
