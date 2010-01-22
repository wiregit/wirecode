package org.limewire.core.api.search;

import org.limewire.io.GUID;

import ca.odell.glazedlists.EventList;

/**
 * Defines the API for the list of results for a single search.
 */
public interface SearchResultList {

    /**
     * Returns the GUID associated with the search.  May be null if the search
     * has not started, or if the search is a browse.
     */
    GUID getGuid();
    
    /**
     * Returns the total number of results found.
     */
    int getResultCount();
    
    /**
     * Returns the search associated with this list.  Never null.
     */
    Search getSearch();
    
    /**
     * Returns the list of search results sorted and grouped by URN.
     */
    EventList<GroupedSearchResult> getGroupedResults();
    
    /**
     * Adds the specified listener to the list that is notified about
     * list events.
     */
    void addListListener(SearchResultListListener listener);
    
    /**
     * Removes the specified listener from the list that is notified about
     * list events.
     */
    void removeListListener(SearchResultListListener listener);
    
    /**
     * Clears all results.
     */
    void clear();
    
    /**
     * Disposes of resources and removes listeners.
     */
    void dispose();
}
