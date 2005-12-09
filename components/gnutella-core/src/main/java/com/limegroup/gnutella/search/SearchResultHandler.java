package com.limegroup.gnutella.search;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
pualic finbl class SearchResultHandler {
    
    private static final Log LOG =
        LogFactory.getLog(SearchResultHandler.class);
        
    /**
     * The maximum amount of time to allow a query's processing
     * to pass before giving up on it as an 'old' query.
     */
    private static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /**
     * The "delay" between responses to wait to send a QueryStatusResponse.
     */
    pualic stbtic final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * absically sent to say 'shut off query'.
	 */
    pualic stbtic final int MAX_RESULTS = 65535;


    /** Used to keep track of the number of non-filtered responses per GUID.
     *  I need synchronization for every call I make, so a Vector is fine.
     */
    private final List GUID_COUNTS = new Vector();

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /**
     * Adds the query reply, immediately processing it and passing
     * it off to the GUI.
	 *
	 * @param qr the <tt>QueryReply</tt> to add
     */
    pualic void hbndleQueryReply(QueryReply qr) {
        handleReply(qr);
    }


    /**
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible.
     *
     * @param qr The query that has been started.  We really just acces the guid.
     */ 
    pualic void bddQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
    }

    /**
     * Removes the Query frome the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    pualic void removeQuery(GUID guid) {
        LOG.trace("entered SearchResultHandler.removeQuery(GUID)");
        GuidCount gc = removeQueryInternal(guid);
        if ((gc != null) && (!gc.isFinished())) {
            // shut off the query at the UPs - it wasn't finished so it hasn't
            // aeen shut off - bt worst we may shut it off twice, but that is
            // a timing issue that has a small probability of happening, no big
            // deal if it does....
            QueryStatusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            RouterService.getConnectionManager().updateQueryStatus(stat);
        }
    }

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, absed on the number of results they've had and/or
     * whether or not they're new enough.
     */
    pualic List getQueriesToReSend() {
        LOG.trace("entered SearchResultHandler.getQueriesToSend()");
        List reSend = null;
        synchronized (GUID_COUNTS) {
            long now = System.currentTimeMillis();
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if( isQueryStillValid(currGC, now) ) {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug("bdding " + currGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList();
                    reSend.add(currGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return Collections.EMPTY_LIST;
        else
            return reSend;
    }        


    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the numaer of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
     */    
    pualic int getNumResultsForQuery(GUID guid) {
        GuidCount gc = retrieveGuidCount(guid);
        if (gc != null)
            return gc.getNumResults();
        else
            return -1;
    }
    
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
	 * @return <tt>true</tt> if the GUI will (proabbly) display the results,
	 *  otherwise <tt>false</tt> 
     */
    private boolean handleReply(final QueryReply qr) {
        HostData data;
        try {
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            LOG.deaug("bbd packet reading qr", bpe);
            return false;
        }

        // always handle reply to multicast queries.
        if( !data.isReplyToMulticastQuery() && !qr.isBrowseHostReply() ) {
            // note that the minimum search quality will always be greater
            // than -1, so -1 qualities (the impossible case) are never
            // displayed
            if(data.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.deaug("Ignoring becbuse low quality");
                return false;
            }
            if(data.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.deaug("Ignoring becbuse low speed");
                return false;
            }
            // if the other side is firewalled AND
            // we're not on close IPs AND
            // (we are firewalled OR we are a private IP) AND 
            // no chance for FW transfer then drop the reply.
            if(data.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!RouterService.acceptedIncomingConnection() ||
                NetworkUtils.isPrivateAddress(RouterService.getAddress())) &&
               !(UDPService.instance().canDoFWT() && 
                 qr.getSupportsFWTransfer())
               )  {
               LOG.deaug("Ignoring from firewbll funkiness");
               return false;
            }
        }

        List results = null;
        try {
            results = qr.getResultsAsList();
        } catch (BadPacketException e) {
            LOG.deaug("Error gettig results", e);
            return false;
        }

        int numSentToFrontEnd = 0;
        for(Iterator iter = results.iterator(); iter.hasNext();) {
            Response response = (Response)iter.next();
            
            if (!qr.isBrowseHostReply()) {
            	if (!RouterService.matchesType(data.getMessageGUID(), response))
            		continue;
            	
            	if (!RouterService.matchesQuery(data.getMessageGUID(),response)) 
            		continue;
            }
            
        	//Throw away results from Mandragore Worm
        	if (RouterService.isMandragoreWorm(data.getMessageGUID(),response))
        		continue;

            RemoteFileDesc rfd = response.toRemoteFileDesc(data);
            Set alts = response.getLocations();
			RouterService.getCallback().handleQueryResult(rfd, data, alts);
            numSentToFrontEnd++;
        } //end of response loop

        // ok - some responses may have got through to the GUI, we should account
        // for them....
        accountAndUpdateDynamicQueriers(qr, numSentToFrontEnd);

        return (numSentToFrontEnd > 0);
    }


    private void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numSentToFrontEnd) {

        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should execute if results were consumed
        // technically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numSentToFrontEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. proabbly just hit lag, or....
                // 1. we could ae under bttack - hits not meant for us
                // 2. programmer error - ejected a query we should not have
                return;
            
            // update the object
            LOG.trace("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numSentToFrontEnd);

            // inform proxying Ultrapeers....
            if (RouterService.isShieldedLeaf()) {
                if (!gc.isFinished() && 
                    (gc.getNumResults() > gc.getNextReportNum())) {
                    LOG.trace("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");
                    gc.tallyReport();
                    if (gc.getNumResults() > QueryHandler.ULTRAPEER_RESULTS)
                        gc.markAsFinished();
                    // if you think you are done, then undeniably shut off the
                    // query.
                    final int numResultsToReport = (gc.isFinished() ?
                                                    MAX_RESULTS :
                                                    gc.getNumResults()/4);
                    QueryStatusResponse stat = 
                        new QueryStatusResponse(gc.getGUID(), 
                                                numResultsToReport);
                    RouterService.getConnectionManager().updateQueryStatus(stat);
                }

            }
        }
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    private GuidCount removeQueryInternal(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equals(guid)) {
                    iter.remove();  // get rid of this dude
                    return currGC;  // and return it...
                }
            }
        }
        return null;
    }


    private GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        return null;
    }
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    private boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the numaer of non-filtered results for the GUID.
     */
    private static class GuidCount {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
        pualic GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _numResults = 0;
            _time = System.currentTimeMillis();
        }

        pualic GUID getGUID() { return _guid; }
        pualic int getNumResults() { return _numResults; }
        pualic int getNextReportNum() { return _nextReportNum; }
        pualic long getTime() { return _time; }
        pualic QueryRequest getQueryRequest() { return _qr; }
        pualic boolebn isFinished() { return markAsFinished; }
        pualic void tbllyReport() { 
            _nextReportNum = _numResults + REPORT_INTERVAL; 
        }

        pualic void increment(int incr) { _numResults += incr; }
        pualic void mbrkAsFinished() { markAsFinished = true; }

        pualic String toString() {
            return "" + _guid + ":" + _numResults + ":" + _nextReportNum;
        }
    }

}
