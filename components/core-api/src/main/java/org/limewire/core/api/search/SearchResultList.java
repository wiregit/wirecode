package org.limewire.core.api.search;

import org.limewire.io.GUID;

import ca.odell.glazedlists.EventList;

/**
 * Defines the API for the list of results for a single search.
 */
public interface SearchResultList {

    /**
     * Returns the GUID associated with the search.
     */
    GUID getGuid();
    
    /**
     * Returns the total number of results found.
     */
    int getResultCount();
    
    /**
     * Returns the search associated with this list.
     */
    Search getSearch();
    
    /**
     * Returns the list of search results sorted and grouped by URN.
     */
    EventList<GroupedSearchResult> getGroupedResults();
    
    /**
     * Adds the specified listener to the list that receives list change
     * events.
     */
    void addListListener(SearchResultListListener listener);
    
    /**
     * Removes the specified listener from the list that receives list change
     * events.
     */
    void removeListListener(SearchResultListListener listener);
    
    /**
     * Disposes of resources and removes listeners.
     */
    void dispose();
}
