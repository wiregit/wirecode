package org.limewire.core.api.search;

/**
 * A listener that is notified when events occur on a list of search results.
 */
public interface SearchResultListListener {

    /**
     * Invoked when a new result is added. 
     */
    void resultCreated(GroupedSearchResult gsr);
    
    /**
     * Invoked when an existing result is changed.  The propertyName, oldValue
     * and newValue represent the specific change.
     */
    void resultChanged(GroupedSearchResult gsr, String propertyName, Object oldValue, Object newValue);
    
    /** 
     * Invoked when all results are cleared from the list. 
     */
    void resultsCleared();
}
