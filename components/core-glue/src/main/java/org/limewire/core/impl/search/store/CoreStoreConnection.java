package org.limewire.core.impl.search.store;

import org.apache.commons.lang.StringUtils;
import org.limewire.core.api.search.store.StoreConnection;

/**
 * Implementation of StoreConnection for the live core.
 */
public class CoreStoreConnection implements StoreConnection {

    /**
     * Performs a search using the specified query text, and returns the 
     * result as a JSON text string.
     */
    @Override
    public String doQuery(String query) {
        if (StringUtils.isEmpty(query)) {
            return "";
        }
        
        // TODO implement
        
        return "";
    }
}
