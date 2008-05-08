package com.limegroup.gnutella.search;

import com.limegroup.gnutella.QueryResultHandler;
import com.limegroup.gnutella.URN;

/**
 * One SearchResultStats instance exists for each active search (which is 
 * represented by a QueryRequest). As responses are received from the network
 * that information is incorporated into the SearchResultStats via 
 * addQueryReply().
 * 
 * Result counts are tabulated on a per-URN basis.
 */
public interface SearchResultStats {
    
    /**
     * Count of all results, good plus weighted bad.
     */
    public int getNumResults();
    
    /**
     * Returns the percentage of the data for the given URN
     * that is available.
     */
    public float getPercentAvailable (URN urn);
    
    /**
     * Returns a handler which can be used to easily access the
     * location count information for the given URN.
     */
    public QueryResultHandler getResultHandler (final URN urn);
    
}