pbckage com.limegroup.gnutella.search;

import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Set;
import jbva.util.Vector;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.Response;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutellb.settings.FilterSettings;
import com.limegroup.gnutellb.settings.SearchSettings;
import com.limegroup.gnutellb.spam.SpamManager;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * Hbndles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instbnces and performs the logic 
 * necessbry to pass those results up to the UI.
 */
public finbl class SearchResultHandler {
    
    privbte static final Log LOG =
        LogFbctory.getLog(SearchResultHandler.class);
        
    /**
     * The mbximum amount of time to allow a query's processing
     * to pbss before giving up on it as an 'old' query.
     */
    privbte static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

    /**
     * The "delby" between responses to wait to send a QueryStatusResponse.
     */
    public stbtic final int REPORT_INTERVAL = 15;

    /** 
     * The mbximum number of results to send in a QueryStatusResponse -
     * bbsically sent to say 'shut off query'.
	 */
    public stbtic final int MAX_RESULTS = 65535;


    /** Used to keep trbck of the number of non-filtered responses per GUID.
     *  I need synchronizbtion for every call I make, so a Vector is fine.
     */
    privbte final List GUID_COUNTS = new Vector();

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /**
     * Adds the query reply, immedibtely processing it and passing
     * it off to the GUI.
	 *
	 * @pbram qr the <tt>QueryReply</tt> to add
     */
    public void hbndleQueryReply(QueryReply qr) {
        hbndleReply(qr);
    }


    /**
     * Adds the Query to the list of queries kept trbck of.  You should do this
     * EVERY TIME you stbrt a query so we can leaf guide it when possible.
     *
     * @pbram qr The query that has been started.  We really just acces the guid.
     */ 
    public void bddQuery(QueryRequest qr) {
        LOG.trbce("entered SearchResultHandler.addQuery(QueryRequest)");
		SpbmManager.instance().startedQuery(qr);
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.bdd(gc);
    }

    /**
     * Removes the Query frome the list of queries kept trbck of.  You should do
     * this EVERY TIME you stop b query.
     *
     * @pbram guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid) {
        LOG.trbce("entered SearchResultHandler.removeQuery(GUID)");
        GuidCount gc = removeQueryInternbl(guid);
        if ((gc != null) && (!gc.isFinished())) {
            // shut off the query bt the UPs - it wasn't finished so it hasn't
            // been shut off - bt worst we may shut it off twice, but that is
            // b timing issue that has a small probability of happening, no big
            // debl if it does....
            QueryStbtusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            RouterService.getConnectionMbnager().updateQueryStatus(stat);
        }
    }

    /**
     * Returns b <tt>List</tt> of queries that require replanting into
     * the network, bbsed on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List getQueriesToReSend() {
        LOG.trbce("entered SearchResultHandler.getQueriesToSend()");
        List reSend = null;
        synchronized (GUID_COUNTS) {
            long now = System.currentTimeMillis();
            Iterbtor iter = GUID_COUNTS.iterator();
            while (iter.hbsNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if( isQueryStillVblid(currGC, now) ) {
                    if(LOG.isDebugEnbbled())
                        LOG.debug("bdding " + currGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList();
                    reSend.bdd(currGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return Collections.EMPTY_LIST;
        else
            return reSend;
    }        


    /**
     * Use this to see how mbny results have been displayed to the user for the
     * specified query.
     *
     * @pbram guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid wbs not found....
     */    
    public int getNumResultsForQuery(GUID guid) {
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
	 * Hbndles the given query reply. Only one thread may call it at a time.
     *      
	 * @return <tt>true</tt> if the GUI will (probbbly) display the results,
	 *  otherwise <tt>fblse</tt> 
     */
    privbte boolean handleReply(final QueryReply qr) {
        HostDbta data;
        try {
            dbta = qr.getHostData();
        } cbtch(BadPacketException bpe) {
            LOG.debug("bbd packet reading qr", bpe);
            return fblse;
        }

        // blways handle reply to multicast queries.
        if( !dbta.isReplyToMulticastQuery() && !qr.isBrowseHostReply() ) {
            // note thbt the minimum search quality will always be greater
            // thbn -1, so -1 qualities (the impossible case) are never
            // displbyed
            if(dbta.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.debug("Ignoring becbuse low quality");
                return fblse;
            }
            if(dbta.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.debug("Ignoring becbuse low speed");
                return fblse;
            }
            // if the other side is firewblled AND
            // we're not on close IPs AND
            // (we bre firewalled OR we are a private IP) AND 
            // no chbnce for FW transfer then drop the reply.
            if(dbta.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!RouterService.bcceptedIncomingConnection() ||
                NetworkUtils.isPrivbteAddress(RouterService.getAddress())) &&
               !(UDPService.instbnce().canDoFWT() && 
                 qr.getSupportsFWTrbnsfer())
               )  {
               LOG.debug("Ignoring from firewbll funkiness");
               return fblse;
            }
        }

        List results = null;
        try {
            results = qr.getResultsAsList();
        } cbtch (BadPacketException e) {
            LOG.debug("Error gettig results", e);
            return fblse;
        }

        int numGoodSentToFrontEnd = 0;
        for(Iterbtor iter = results.iterator(); iter.hasNext();) {
            Response response = (Response)iter.next();
            
            if (!qr.isBrowseHostReply()) {
            	if (!RouterService.mbtchesType(data.getMessageGUID(), response))
            		continue;
            	
            	if (!RouterService.mbtchesQuery(data.getMessageGUID(),response)) 
            		continue;
            }
            
        	//Throw bway results from Mandragore Worm
        	if (RouterService.isMbndragoreWorm(data.getMessageGUID(),response))
        		continue;

            RemoteFileDesc rfd = response.toRemoteFileDesc(dbta);
            Set blts = response.getLocations();
			RouterService.getCbllback().handleQueryResult(rfd, data, alts);
			
			if (! SpbmManager.instance().isSpam(rfd))
				numGoodSentToFrontEnd++;
        } //end of response loop

        // ok - some responses mby have got through to the GUI, we should account
        // for them....
        bccountAndUpdateDynamicQueriers(qr, numGoodSentToFrontEnd);

        return numGoodSentToFrontEnd > 0;
    }


    privbte void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 finbl int numGoodSentToFrontEnd) {

        LOG.trbce("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should execute if results were consumed
        // technicblly Ultrapeers don't use this info, but we are keeping it
        // bround for further use
        if (numGoodSentToFrontEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. probbbly just hit lag, or....
                // 1. we could be under bttack - hits not meant for us
                // 2. progrbmmer error - ejected a query we should not have
                return;
            
            // updbte the object
            LOG.trbce("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numGoodSentToFrontEnd);

            // inform proxying Ultrbpeers....
            if (RouterService.isShieldedLebf()) {
                if (!gc.isFinished() && 
                    (gc.getNumResults() > gc.getNextReportNum())) {
                    LOG.trbce("SRH.accountAndUpdateDynamicQueriers(): telling UPs.");
                    gc.tbllyReport();
                    if (gc.getNumResults() > QueryHbndler.ULTRAPEER_RESULTS)
                        gc.mbrkAsFinished();
                    // if you think you bre done, then undeniably shut off the
                    // query.
                    finbl int numResultsToReport = (gc.isFinished() ?
                                                    MAX_RESULTS :
                                                    gc.getNumResults()/4);
                    QueryStbtusResponse stat = 
                        new QueryStbtusResponse(gc.getGUID(), 
                                                numResultsToReport);
                    RouterService.getConnectionMbnager().updateQueryStatus(stat);
                }

            }
        }
        LOG.trbce("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    privbte GuidCount removeQueryInternal(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterbtor iter = GUID_COUNTS.iterator();
            while (iter.hbsNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equbls(guid)) {
                    iter.remove();  // get rid of this dude
                    return currGC;  // bnd return it...
                }
            }
        }
        return null;
    }


    privbte GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterbtor iter = GUID_COUNTS.iterator();
            while (iter.hbsNext()) {
                GuidCount currGC = (GuidCount) iter.next();
                if (currGC.getGUID().equbls(guid))
                    return currGC;
            }
        }
        return null;
    }
    
    /**
     * Determines whether or not the query contbined in the
     * specified GuidCount is still vblid.
     * This depends on vblues such as the time the query was
     * crebted and the amount of results we've received so far
     * for this query.
     */
    privbte boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trbce("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < QueryHbndler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A contbiner that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    privbte static class GuidCount {

        privbte final long _time;
        privbte final GUID _guid;
        privbte final QueryRequest _qr;
        privbte int _numGoodResults;
        privbte int _nextReportNum = REPORT_INTERVAL;
        privbte boolean markAsFinished = false;
        
        public GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _time = System.currentTimeMillis();
        }

        public GUID getGUID() { return _guid; }
        public int getNumResults() {
			return _numGoodResults ;
		}
		public int getNextReportNum() { return _nextReportNum; }
        public long getTime() { return _time; }
        public QueryRequest getQueryRequest() { return _qr; }
        public boolebn isFinished() { return markAsFinished; }
        public void tbllyReport() { 
            _nextReportNum = _numGoodResults + REPORT_INTERVAL; 
        }

        public void increment(int good) {
			_numGoodResults += good;
		}
        
        public void mbrkAsFinished() { markAsFinished = true; }

        public String toString() {
            return "" + _guid + ":" + _numGoodResults + ":" + _nextReportNum;
        }
    }

}
