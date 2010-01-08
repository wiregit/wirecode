package org.limewire.core.api.search;

import ca.odell.glazedlists.EventList;

/**
 * Defines the API for the list of results for a single search.
 */
public interface SearchResultList {

    /**
     * Returns the search associated with this list.
     */
    Search getSearch();
    
    /**
     * Returns the list of search results.
     */
    EventList<SearchResult> getSearchResults();
    
    /**
     * Disposes of resources and removes listeners.
     */
    void dispose();
}
