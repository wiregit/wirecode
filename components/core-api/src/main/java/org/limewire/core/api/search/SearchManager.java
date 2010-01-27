package org.limewire.core.api.search;

import java.util.List;

/**
 * Defines the API for the search manager.
 */
public interface SearchManager {

    /**
     * Adds the specified search to the manager and returns its result list.
     * This method should be called before starting the search; otherwise, the
     * result list may not contain results generated before the list was
     * created.
     */
    SearchResultList addSearch(Search search, SearchDetails searchDetails);
    
    /**
     * Removes the specified search from the manager.  This method should be
     * called after stopping a search to dispose of resources.
     */
    void removeSearch(Search search);
    
    /**
     * Returns a list of active searches.  An active search has been assigned
     * a GUID, which includes network searches started but excludes unstarted
     * searches and browse searches.
     */
    List<Search> getActiveSearches();
    
    /**
     * Returns the result list for the specified search.  The method returns 
     * null if there is no list associated with the search.
     */
    SearchResultList getSearchResultList(Search search);
}
