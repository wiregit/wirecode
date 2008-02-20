package com.limegroup.gnutella.search;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

public interface SearchResultStats {

    public GUID getGUID();
    
    public int getNumResults();
    
    /**
     * Returns the number of locations known for the URN. This
     * includes combining partial results.
     * 
     * TODO
     * 
     * For the time being this simply adds one to the count if
     * any partial search results exist.
     * 
     * @param urn
     * @return
     */
    public int getNumResultsForURN(URN urn);
    
    public int getNextReportNum();
    public long getTime();
    public QueryRequest getQueryRequest();
    public boolean isFinished();
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
    
    public void increment(int good);
    
    public void markAsFinished();

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
    
}
