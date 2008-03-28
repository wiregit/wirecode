package com.limegroup.gnutella.dht.db;

public interface SearchListener<Result> {
    
    /**
     * Called when a result has been found, can be called several times,
     * depnding on the kind of search.
     */
    void handleResult(Result result);

    /**
     * Is called when a search has been performed, any result has been returned 
     * or an exception occurred during lookup. 
     * It is also called when the search was not successful.
     *
     * @param success whether or not the search was successful
     */
    void handleSearchDone(boolean success);
    
}
