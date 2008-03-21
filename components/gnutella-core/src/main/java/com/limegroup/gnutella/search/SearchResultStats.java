package com.limegroup.gnutella.search;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.QueryResultHandler;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

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
     * 
     * @return
     */
    public int getNextReportNum();
    
    /**
     * 
     * @return
     */
    public long getTime();
    
    /**
     * 
     * @return
     */
    public QueryRequest getQueryRequest();
    
    /**
     * 
     * @return
     */
    public boolean isFinished();
    
    /**
     * 
     */
    public void tallyReport();

    /**
     * Returns the percentage of the data for the given URN
     * that is available.
     * 
     * TODO
     * 
     * Make this work.
     * 
     * @param urn
     * @return
     */
    public int getPercentAvailable (URN urn);
    
    /**
     * Returns the number of locations that have the data for
     * the given URN. This incorporates partial search results,
     * where the partial results will be combined together to
     * form complete results.
     * 
     * @param urn
     * @return
     */
    public int getNumberOfLocations (URN urn);
    
    /**
     * 
     * @param good
     */
    public void increment(int good);
    
    /**
     * 
     */
    public void markAsFinished();

    /**
     * 
     * @return
     */
    public String toString();
    
    /**
     * Absorbs the query reply into the collective, adding
     * its distinctiveness to our own. Resistance is futile.
     * 
     * TODO
     * 
     * Determine the number of locations from which the relevant URN is
     * available, incorporating the partial search results.
     * 
     * Moved into GuidCount from SearchResultHandler::handleQueryReply().
     * 
     * @param qr
     */
    public int addQueryReply(SearchResultHandler srh, QueryReply qr, HostData data);
    
    /**
     * Returns a handler which can be used to easily access the
     * location count information for the given URN.
     */
    public QueryResultHandler getResultHandler (final URN urn);
    
}