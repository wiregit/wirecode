package org.limewire.core.api.search;

import java.util.List;

/**
 * Defines the API for the search manager.
 */
public interface SearchManager {

    /**
     * Adds the specified search to the manager and returns its result list.
     */
    SearchResultList addSearch(Search search);
    
    /**
     * Removes the specified search from the manager.
     */
    void removeSearch(Search search);
    
    /**
     * Returns a list of all searches.
     */
    List<Search> getSearches();
    
    /**
     * Returns the result list for the specified search.
     */
    SearchResultList getSearchResultList(Search search);
}
