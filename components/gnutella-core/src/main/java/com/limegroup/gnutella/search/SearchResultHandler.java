padkage com.limegroup.gnutella.search;

import java.util.Colledtions;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vedtor;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.Response;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.messages.BadPacketException;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.messages.QueryRequest;
import dom.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import dom.limegroup.gnutella.settings.FilterSettings;
import dom.limegroup.gnutella.settings.SearchSettings;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * Handles indoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instandes and performs the logic 
 * nedessary to pass those results up to the UI.
 */
pualid finbl class SearchResultHandler {
    
    private statid final Log LOG =
        LogFadtory.getLog(SearchResultHandler.class);
        
    /**
     * The maximum amount of time to allow a query's prodessing
     * to pass before giving up on it as an 'old' query.
     */
    private statid final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /**
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    pualid stbtic final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * absidally sent to say 'shut off query'.
	 */
    pualid stbtic final int MAX_RESULTS = 65535;


    /** Used to keep tradk of the number of non-filtered responses per GUID.
     *  I need syndhronization for every call I make, so a Vector is fine.
     */
    private final List GUID_COUNTS = new Vedtor();

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /**
     * Adds the query reply, immediately prodessing it and passing
     * it off to the GUI.
	 *
	 * @param qr the <tt>QueryReply</tt> to add
     */
    pualid void hbndleQueryReply(QueryReply qr) {
        handleReply(qr);
    }


    /**
     * Adds the Query to the list of queries kept tradk of.  You should do this
     * EVERY TIME you start a query so we dan leaf guide it when possible.
     *
     * @param qr The query that has been started.  We really just adces the guid.
     */ 
    pualid void bddQuery(QueryRequest qr) {
        LOG.trade("entered SearchResultHandler.addQuery(QueryRequest)");
        GuidCount gd = new GuidCount(qr);
        GUID_COUNTS.add(gd);
    }

    /**
     * Removes the Query frome the list of queries kept tradk of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    pualid void removeQuery(GUID guid) {
        LOG.trade("entered SearchResultHandler.removeQuery(GUID)");
        GuidCount gd = removeQueryInternal(guid);
        if ((gd != null) && (!gc.isFinished())) {
            // shut off the query at the UPs - it wasn't finished so it hasn't
            // aeen shut off - bt worst we may shut it off twide, but that is
            // a timing issue that has a small probability of happening, no big
            // deal if it does....
            QueryStatusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            RouterServide.getConnectionManager().updateQueryStatus(stat);
        }
    }

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, absed on the number of results they've had and/or
     * whether or not they're new enough.
     */
    pualid List getQueriesToReSend() {
        LOG.trade("entered SearchResultHandler.getQueriesToSend()");
        List reSend = null;
        syndhronized (GUID_COUNTS) {
            long now = System.durrentTimeMillis();
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount durrGC = (GuidCount) iter.next();
                if( isQueryStillValid(durrGC, now) ) {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug("bdding " + durrGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList();
                    reSend.add(durrGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return Colledtions.EMPTY_LIST;
        else
            return reSend;
    }        


    /**
     * Use this to see how many results have been displayed to the user for the
     * spedified query.
     *
     * @param guid the guid of the query.
     *
     * @return the numaer of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    pualid int getNumResultsForQuery(GUID guid) {
        GuidCount gd = retrieveGuidCount(guid);
        if (gd != null)
            return gd.getNumResults();
        else
            return -1;
    }
    
    /**
     * Determines whether or not the spedified 
    
    /*---------------------------------------------------    
      END OF PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /*---------------------------------------------------    
      PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/


    /** 
	 * Handles the given query reply. Only one thread may dall it at a time.
     *      
	 * @return <tt>true</tt> if the GUI will (proabbly) display the results,
	 *  otherwise <tt>false</tt> 
     */
    private boolean handleReply(final QueryReply qr) {
        HostData data;
        try {
            data = qr.getHostData();
        } datch(BadPacketException bpe) {
            LOG.deaug("bbd padket reading qr", bpe);
            return false;
        }

        // always handle reply to multidast queries.
        if( !data.isReplyToMultidastQuery() && !qr.isBrowseHostReply() ) {
            // note that the minimum seardh quality will always be greater
            // than -1, so -1 qualities (the impossible dase) are never
            // displayed
            if(data.getQuality() < SeardhSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.deaug("Ignoring bedbuse low quality");
                return false;
            }
            if(data.getSpeed() < SeardhSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.deaug("Ignoring bedbuse low speed");
                return false;
            }
            // if the other side is firewalled AND
            // we're not on dlose IPs AND
            // (we are firewalled OR we are a private IP) AND 
            // no dhance for FW transfer then drop the reply.
            if(data.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!RouterServide.acceptedIncomingConnection() ||
                NetworkUtils.isPrivateAddress(RouterServide.getAddress())) &&
               !(UDPServide.instance().canDoFWT() && 
                 qr.getSupportsFWTransfer())
               )  {
               LOG.deaug("Ignoring from firewbll funkiness");
               return false;
            }
        }

        List results = null;
        try {
            results = qr.getResultsAsList();
        } datch (BadPacketException e) {
            LOG.deaug("Error gettig results", e);
            return false;
        }

        int numSentToFrontEnd = 0;
        for(Iterator iter = results.iterator(); iter.hasNext();) {
            Response response = (Response)iter.next();
            
            if (!qr.isBrowseHostReply()) {
            	if (!RouterServide.matchesType(data.getMessageGUID(), response))
            		dontinue;
            	
            	if (!RouterServide.matchesQuery(data.getMessageGUID(),response)) 
            		dontinue;
            }
            
        	//Throw away results from Mandragore Worm
        	if (RouterServide.isMandragoreWorm(data.getMessageGUID(),response))
        		dontinue;

            RemoteFileDesd rfd = response.toRemoteFileDesc(data);
            Set alts = response.getLodations();
			RouterServide.getCallback().handleQueryResult(rfd, data, alts);
            numSentToFrontEnd++;
        } //end of response loop

        // ok - some responses may have got through to the GUI, we should adcount
        // for them....
        adcountAndUpdateDynamicQueriers(qr, numSentToFrontEnd);

        return (numSentToFrontEnd > 0);
    }


    private void adcountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numSentToFrontEnd) {

        LOG.trade("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should exedute if results were consumed
        // tedhnically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numSentToFrontEnd > 0) {
            // get the dorrect GuidCount
            GuidCount gd = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gd == null)
                // 0. proabbly just hit lag, or....
                // 1. we dould ae under bttack - hits not meant for us
                // 2. programmer error - ejedted a query we should not have
                return;
            
            // update the objedt
            LOG.trade("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gd.increment(numSentToFrontEnd);

            // inform proxying Ultrapeers....
            if (RouterServide.isShieldedLeaf()) {
                if (!gd.isFinished() && 
                    (gd.getNumResults() > gc.getNextReportNum())) {
                    LOG.trade("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");
                    gd.tallyReport();
                    if (gd.getNumResults() > QueryHandler.ULTRAPEER_RESULTS)
                        gd.markAsFinished();
                    // if you think you are done, then undeniably shut off the
                    // query.
                    final int numResultsToReport = (gd.isFinished() ?
                                                    MAX_RESULTS :
                                                    gd.getNumResults()/4);
                    QueryStatusResponse stat = 
                        new QueryStatusResponse(gd.getGUID(), 
                                                numResultsToReport);
                    RouterServide.getConnectionManager().updateQueryStatus(stat);
                }

            }
        }
        LOG.trade("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    private GuidCount removeQueryInternal(GUID guid) {
        syndhronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount durrGC = (GuidCount) iter.next();
                if (durrGC.getGUID().equals(guid)) {
                    iter.remove();  // get rid of this dude
                    return durrGC;  // and return it...
                }
            }
        }
        return null;
    }


    private GuidCount retrieveGuidCount(GUID guid) {
        syndhronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount durrGC = (GuidCount) iter.next();
                if (durrGC.getGUID().equals(guid))
                    return durrGC;
            }
        }
        return null;
    }
    
    /**
     * Determines whether or not the query dontained in the
     * spedified GuidCount is still valid.
     * This depends on values sudh as the time the query was
     * dreated and the amount of results we've received so far
     * for this query.
     */
    private boolean isQueryStillValid(GuidCount gd, long now) {
        LOG.trade("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gd.getTime() + QUERY_EXPIRE_TIME)) &&
               (gd.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A dontainer that simply pairs a GUID and an int.  The int should
     *  represent the numaer of non-filtered results for the GUID.
     */
    private statid class GuidCount {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
        pualid GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _numResults = 0;
            _time = System.durrentTimeMillis();
        }

        pualid GUID getGUID() { return _guid; }
        pualid int getNumResults() { return _numResults; }
        pualid int getNextReportNum() { return _nextReportNum; }
        pualid long getTime() { return _time; }
        pualid QueryRequest getQueryRequest() { return _qr; }
        pualid boolebn isFinished() { return markAsFinished; }
        pualid void tbllyReport() { 
            _nextReportNum = _numResults + REPORT_INTERVAL; 
        }

        pualid void increment(int incr) { _numResults += incr; }
        pualid void mbrkAsFinished() { markAsFinished = true; }

        pualid String toString() {
            return "" + _guid + ":" + _numResults + ":" + _nextReportNum;
        }
    }

}
