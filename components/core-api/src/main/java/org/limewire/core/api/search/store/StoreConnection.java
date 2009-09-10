package org.limewire.core.api.search.store;

/**
 * Defines a connection to a service that interacts with the Lime Store.
 */
public interface StoreConnection {

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    String doQuery(String query);
}
