package com.limegroup.gnutella.search;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.QueryResultHandler;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;

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
     * Returns the GUID with which this SearchResultStats was created.
     */
    public GUID getGUID();
    
    /**
     * Count of all results, good plus weighted bad.
     */
    public int getNumResults();
    
    /**
     * Returns the number of locations known for the URN. This includes 
     * combining partial results.
     * 
     * @param urn The URN for which you want the result count.
     * 
     * @return The number of results associated with the given URN.
     */
    public int getNumResultsForURN(URN urn);
    
    /**
     * Returns the percentage of the data for the given URN
     * that is available.
     */
    public float getPercentAvailable (URN urn);
    
    /**
     * Absorbs the query reply into the collective, adding
     * its distinctiveness to our own. Resistance is futile.
     */
    public int addQueryReply(SearchResultHandler srh, QueryReply qr, HostData data);
    
    /**
     * Returns a handler which can be used to easily access the
     * location count information for the given URN.
     */
    public QueryResultHandler getResultHandler (final URN urn);
    
}