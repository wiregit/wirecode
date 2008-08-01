package org.limewire.core.api.search;

/**
 * A single search.
 */
public interface Search {
    
    /** Returns the category this search is for. */
    SearchCategory getCategory();
    
    /** Starts the search. */
    void start(SearchListener searchListener);
    
    /** Repeats the search. */
    void repeat();
    
    /** Stops the search. */
    void stop();

}
