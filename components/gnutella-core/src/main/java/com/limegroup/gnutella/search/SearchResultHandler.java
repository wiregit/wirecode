package com.limegroup.gnutella.search;

import java.util.List;

import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
public interface SearchResultHandler {
    
    /**
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    public static final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * basically sent to say 'shut off query'.
     */
    public static final int MAX_RESULTS = 65535;

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /**
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible.
     * Also adds the query to the Spam Manager to adjust percentages.
     *
     * @param qr The query that has been started.  We really just access the guid.
     */ 
    public SearchResultStats addQuery(QueryRequest qr);

    /**
     * Removes the Query from the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid);

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, based on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List<QueryRequest> getQueriesToReSend();

    /**
     * Returns the searchServices member value.
     * 
     * @return
     */
    public SearchServices getSearchServices();

    /**
     * Returns the activityCallback member value.
     * 
     * @return
     */
    public Provider<ActivityCallback> getActivityCallback();
    
    /**
     * Returns the spamManager member value.
     * 
     * @return
     */
    public Provider<SpamManager> getSpamManager();
    
    /**
     * Returns the remoteFileDescFactory member value.
     * 
     * @return
     */
    public RemoteFileDescFactory getRemoteFileDescFactory();
    
    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    public int getNumResultsForQuery(GUID guid);
    
    /**
     * Determines whether or not the specified 
    
    /*---------------------------------------------------    
      END OF PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /*---------------------------------------------------    
      PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/


    /** 
	 * Handles the given query reply. Only one thread may call it at a time.
     *      
	 * @return <tt>true</tt> if the GUI will (probably) display the results,
	 *  otherwise <tt>false</tt> 
     */
    public void handleQueryReply(final QueryReply qr);

    void countClassC(QueryReply qr, Response r);

    void accountAndUpdateDynamicQueriers(final QueryReply qr, HostData data /*,
                                                 final int numGoodSentToFrontEnd */ );

    SearchResultStats removeQueryInternal(GUID guid);

    SearchResultStats retrieveGuidCount(GUID guid);
    
    boolean isWhatIsNew(QueryReply reply);
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    boolean isQueryStillValid(SearchResultStats srs, long now);

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
}
