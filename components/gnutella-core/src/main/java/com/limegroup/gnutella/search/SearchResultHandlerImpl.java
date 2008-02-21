package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.FixedsizeForgetfulHashMap;
import org.limewire.collection.IntervalSet;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.QueryResultHandler;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.util.ClassCNetworks;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class SearchResultHandlerImpl implements SearchResultHandler {

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
    public static final int REPORT_INTERVAL = 15;

    /** 
     * The maximum number of results to send in a QueryStatusResponse -
     * basically sent to say 'shut off query'.
     */
    public static final int MAX_RESULTS = 65535;


    /** Used to keep track of the number of non-filtered responses per GUID.
     *  I need synchronization for every call I make, so a Vector is fine.
     */
    private final List<GuidCount> GUID_COUNTS = new Vector<GuidCount>();
    
    /**
     * counter for class C networks per query per urn
     * remember the last 10 queries.
     */
    private final Map<GUID, Map<URN,ClassCNetworks[]>> cncCounter = 
        Collections.synchronizedMap(new FixedsizeForgetfulHashMap<GUID, Map<URN,ClassCNetworks[]>>(10));
    
    private final NetworkManager networkManager;
    private final SearchServices searchServices;
    private final Provider<ActivityCallback> activityCallback;
    private final Provider<ConnectionManager> connectionManager;
    private final ConnectionServices connectionServices;
    private final Provider<SpamManager> spamManager;
    private final RemoteFileDescFactory remoteFileDescFactory;

    @Inject
    public SearchResultHandlerImpl(NetworkManager networkManager,
            SearchServices searchServices,
            Provider<ActivityCallback> activityCallback,
            Provider<ConnectionManager> connectionManager,
            ConnectionServices connectionServices,
            Provider<SpamManager> spamManager,
            RemoteFileDescFactory remoteFileDescFactory) {
        this.networkManager = networkManager;
        this.searchServices = searchServices;
        this.activityCallback = activityCallback;
        this.connectionManager = connectionManager;
        this.connectionServices = connectionServices;
        this.spamManager = spamManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

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
    public SearchResultStats addQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        
        if (!qr.isBrowseHostQuery() && !qr.isWhatIsNewRequest())
            spamManager.get().startedQuery(qr);
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
        
        return gc;
    }

    /**
     * Removes the Query from the list of queries kept track of.  You should do
     * this EVERY TIME you stop a query.
     *
     * @param guid the guid of the query that has been removed.
     */ 
    public void removeQuery(GUID guid) {
        LOG.trace("entered SearchResultHandler.removeQuery(GUID)");
        cncCounter.remove(guid);
        GuidCount gc = removeQueryInternal(guid);
        if ((gc != null) && (!gc.isFinished())) {
            // shut off the query at the UPs - it wasn't finished so it hasn't
            // been shut off - at worst we may shut it off twice, but that is
            // a timing issue that has a small probability of happening, no big
            // deal if it does....
            QueryStatusResponse stat = new QueryStatusResponse(guid, 
                                                               MAX_RESULTS);
            connectionManager.get().updateQueryStatus(stat);
        }
    }

    /**
     * Returns a <tt>List</tt> of queries that require replanting into
     * the network, based on the number of results they've had and/or
     * whether or not they're new enough.
     */
    public List<QueryRequest> getQueriesToReSend() {
        LOG.trace("entered SearchResultHandler.getQueriesToSend()");
        List<QueryRequest> reSend = null;
        synchronized (GUID_COUNTS) {
            long now = System.currentTimeMillis();
            for(GuidCount currGC : GUID_COUNTS) {
                if( isQueryStillValid(currGC, now) ) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("adding " + currGC + 
                                  " to list of queries to resend");
                    if( reSend == null )
                        reSend = new LinkedList<QueryRequest>();
                    reSend.add(currGC.getQueryRequest());
                }
            }
        }
        if( reSend == null )
            return Collections.emptyList();
        else
            return reSend;
    }        

    /**
     * Returns the searchServices member value.
     * 
     * @return
     */
    public SearchServices getSearchServices() {
        return searchServices;
    }

    /**
     * Returns the activityCallback member value.
     * 
     * @return
     */
    public Provider<ActivityCallback> getActivityCallback() {
        return activityCallback;
    }
    
    /**
     * Returns the spamManager member value.
     * 
     * @return
     */
    public Provider<SpamManager> getSpamManager() {
        return spamManager;
    }
    
    /**
     * Returns the remoteFileDescFactory member value.
     * 
     * @return
     */
    public RemoteFileDescFactory getRemoteFileDescFactory() {
        return remoteFileDescFactory;
    }
    
    /**
     * Use this to see how many results have been displayed to the user for the
     * specified query.
     *
     * @param guid the guid of the query.
     *
     * @return the number of non-filtered results for query with guid guid. -1
     * is returned if the guid was not found....
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
     * Handles the given query reply. Only one thread may call it at a time.
     *      
     * @return <tt>true</tt> if the GUI will (probably) display the results,
     *  otherwise <tt>false</tt> 
     */
    public void handleQueryReply(final QueryReply qr) {
        HostData data;
        try {
            data = qr.getHostData();
        } catch(BadPacketException bpe) {
            LOG.debug("bad packet reading qr", bpe);
            return;
        }

        // always handle reply to multicast queries.
        if( !data.isReplyToMulticastQuery() && !qr.isBrowseHostReply() ) {
            // note that the minimum search quality will always be greater
            // than -1, so -1 qualities (the impossible case) are never
            // displayed
            if(data.getQuality() < SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
                LOG.debug("Ignoring because low quality");
                return;
            }
            if(data.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.debug("Ignoring because low speed");
                return;
            }
            // if the other side is firewalled AND
            // we're not on close IPs AND
            // (we are firewalled OR we are a private IP) AND 
            // no chance for FW transfer then drop the reply.
            if(data.isFirewalled() && 
               !NetworkUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!networkManager.acceptedIncomingConnection() ||
                NetworkUtils.isPrivateAddress(networkManager.getAddress())) &&
               !(networkManager.canDoFWT() && 
                 qr.getSupportsFWTransfer())
               )  {
               LOG.debug("Ignoring from firewall funkiness");
               return;
            }
        }

        // throw away results that aren't secure.
        int secureStatus = qr.getSecureStatus();
        if(secureStatus == SecureMessage.FAILED)
            return;
        
        // XXX
        //
        // add in a count of partial search results
        //
        accountAndUpdateDynamicQueriers(qr, data /*, numGoodSentToFrontEnd + (int)numBadSentToFrontEnd */ );
    }

    public void countClassC(QueryReply qr, Response r) {
        synchronized(cncCounter) {
            GUID searchGuid = new GUID(qr.getGUID());
            Map<URN, ClassCNetworks[]> m = cncCounter.get(searchGuid);
            if (m == null) {
                m = new HashMap<URN,ClassCNetworks[]>();
                cncCounter.put(searchGuid,m);
            }
            for (URN u : r.getUrns()) {
                ClassCNetworks [] cnc = m.get(u);
                if (cnc == null) {
                    cnc = new ClassCNetworks[]{new ClassCNetworks(), new ClassCNetworks()};
                    m.put(u, cnc);
                }
                cnc[0].add(ByteOrder.beb2int(qr.getIPBytes(), 0), 1);
                cnc[1].addAll(r.getLocations());
            }
        }
    }

    public void accountAndUpdateDynamicQueriers(final QueryReply qr, HostData data /*,
                                                 final int numGoodSentToFrontEnd */ ) {

        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");
        
        // get the correct GuidCount
        GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
        
        if (gc == null)
            // 0. probably just hit lag, or....
            // 1. we could be under attack - hits not meant for us
            // 2. programmer error - ejected a query we should not have
            return;
        
        int numGoodSentToFrontEnd = gc.addQueryReply(this, qr, data);
            
        // we should execute if results were consumed
        // technically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numGoodSentToFrontEnd > 0) {
            // update the object
            LOG.trace("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            
            // inform proxying Ultrapeers....
            if (connectionServices.isShieldedLeaf()) {
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
                    connectionManager.get().updateQueryStatus(stat);
                }

            }
        }
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    public GuidCount removeQueryInternal(GUID guid) {
        synchronized (GUID_COUNTS) {
            Iterator<GuidCount> iter = GUID_COUNTS.iterator();
            while (iter.hasNext()) {
                GuidCount currGC = iter.next();
                if (currGC.getGUID().equals(guid)) {
                    iter.remove();  // get rid of this dude
                    return currGC;  // and return it...
                }
            }
        }
        return null;
    }


    public GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            for(GuidCount currGC : GUID_COUNTS) {
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        
        return null;
    }
    
    public boolean isWhatIsNew(QueryReply reply) {
        GuidCount gc = retrieveGuidCount(new GUID(reply.getGUID()));
        return gc != null && gc.getQueryRequest().isWhatIsNewRequest();
    }
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    public boolean isQueryStillValid(SearchResultStats srs, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (srs.getTime() + QUERY_EXPIRE_TIME)) &&
               (srs.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    static class GuidCount implements SearchResultStats {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numGoodResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
        /**
         * An IntervalSet represents a partial search result, and in
         * this Map we organize all of the partial search results by
         * the associated URN.
         */
        private final Map<URN, ResourceLocationCounter> _isets = new HashMap<URN, ResourceLocationCounter>();
        
        public GuidCount(QueryRequest qr) {
            _qr = qr;
            _guid = new GUID(qr.getGUID());
            _time = System.currentTimeMillis();
        }

        public GUID getGUID() { return _guid; }
        public int getNumResults() {
            return _numGoodResults ;
        }
        
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
        public int getNumResultsForURN(URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return getNumResults();
            else
                return getNumResults() + rlc.getLocationCount();
        }
        
        public int getNextReportNum() { return _nextReportNum; }
        public long getTime() { return _time; }
        public QueryRequest getQueryRequest() { return _qr; }
        public boolean isFinished() { return markAsFinished; }
        public void tallyReport() { 
            _nextReportNum = _numGoodResults + REPORT_INTERVAL; 
        }

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
        public int getPercentAvailable (URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return 0;
            
            return rlc.getPercentAvailable();
        }
        
        /**
         * Returns the number of locations that have the data for
         * the given URN. This incorporates partial search results,
         * where the partial results will be combined together to
         * form complete results.
         * 
         * @param urn
         * @return
         */
        public int getNumberOfLocations (URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return 0;
            
            return rlc.getLocationCount();
        }
        
        public void increment(int good) {
            _numGoodResults += good;
        }
        
        public void markAsFinished() { markAsFinished = true; }

        public String toString() {
            return "" + _guid + ":" + _numGoodResults + ":" + _nextReportNum;
        }
        
        
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
        public int addQueryReply(SearchResultHandler srh, QueryReply qr, HostData data) {
            List<Response> results = null;
            double numBad = 0;
            int numGood = 0;
            
            try {
                results = qr.getResultsAsList();
            } catch (BadPacketException e) {
                LOG.debug("Error gettig results", e);
                return 0;
            }

            // throw away results that aren't secure.
            int secureStatus = qr.getSecureStatus();
            if(secureStatus == SecureMessage.FAILED)
                return 0;
            
            boolean skipSpam = srh.isWhatIsNew(qr) || qr.isBrowseHostReply();

            for (Response response : results) {
                if (!qr.isBrowseHostReply() && secureStatus != SecureMessage.SECURE) {
                    if (!srh.getSearchServices().matchesType(data.getMessageGUID(), response))
                        continue;

                    if (!srh.getSearchServices().matchesQuery(data.getMessageGUID(), response))
                        continue;
                }

                // Throw away results from Mandragore Worm
                if (srh.getSearchServices().isMandragoreWorm(data.getMessageGUID(), response))
                    continue;
                
                // If there was an action, only allow it if it's a secure message.
                LimeXMLDocument doc = response.getDocument();
                if (ApplicationSettings.USE_SECURE_RESULTS.getValue() &&
                   doc != null && !"".equals(doc.getAction()) && secureStatus != SecureMessage.SECURE) {
                   continue;
                }
                
                // the interval set, if there is one, represents the
                // subset of the file data that this responding client
                // has available.
                //
                IntervalSet is = response.getRanges();
                
                // we'll be showing the result to the user, count it
                srh.countClassC(qr,response);
                RemoteFileDesc rfd = response.toRemoteFileDesc(data, srh.getRemoteFileDescFactory());
                rfd.setSecureStatus(secureStatus);
                Set<? extends IpPort> alts = response.getLocations();
                srh.getActivityCallback().get().handleQueryResult(rfd, data, alts);
                
                if (skipSpam || !srh.getSpamManager().get().isSpam(rfd)) {
                    if (is != null) {
                        numGood += addIntervalSet(response.getUrns(), is, response.getSize());
                        System.out.println(" *** GuidCount::addQueryReply().. [POINT-A] partial result");
                    }
                    else {
                        numGood += addLocation(response.getUrns(), response.getSize());
                        System.out.println(" *** GuidCount::addQueryReply().. [POINT-B] whole result");
                    }
                }
                else
                    numBad++;
            } //end of response loop
            
            numGood += Math.ceil(numBad * SearchSettings.SPAM_RESULT_RATIO.getValue());
            _numGoodResults += numGood;
            
            return numGood;
        }
        
        /**
         * Adds the given IntervalSet to our internal map,
         * keyed on the appropriate URN, where the appropriate
         * URN is presently the first sha1 URN that we find. 
         * 
         * @param urns A Set of URNs, typically from a Response
         * @param is An IntervalSet, typically from a Response
         * @return Returns the number of new locations that exist for the
         *         given Set<URN> based on the IntervalSet.
         * 
         */
        private int addIntervalSet(Set<URN> urns, IntervalSet is, long size) {
            ResourceLocationCounter rlc = null;
            
            int count_before = 0;
            int count_after = 0;
            
            for (URN urn : urns) {
                System.out.println(" *** GuidCount::addIntervalSet().. [POINT-A] urn.isSHA1 = " + urn.isSHA1() + ";");
                
                if (!urn.isSHA1())
                    continue;
                
                if (null == (rlc = _isets.get(urn)))
                    _isets.put(urn, (rlc = new ResourceLocationCounter(urn, size)));
                
                count_before = rlc.getLocationCount();
                rlc.addIntervalSet( is );
                count_after = rlc.getLocationCount();
                
                break;
            }
            
            System.out.println(" *** GuidCount::addIntervalSet().. [POINT-B] b=" + count_before + "; a=" + count_after + ";");
            
            return count_after - count_before;
        }
        
        private int addLocation(Set<URN> urns, long size) {
            ResourceLocationCounter rlc = null;
            
            int count_before = 0;
            int count_after = 1;
            
            for (URN urn : urns) {
                if (!urn.isSHA1())
                    continue;
                
                if (null == (rlc = _isets.get(urn)))
                    _isets.put(urn, (rlc = new ResourceLocationCounter(urn, size)));
                
                rlc.incrementCount();
                
                break;
            }
            
            return count_after - count_before;
        }
        
        /**
         * Returns a handler which can be used to easily access the
         * location count information for the given URN.
         * 
         */
        public QueryResultHandler getResultHandler (final URN urn) {
            final GuidCount gc = this;
            
            return new QueryResultHandler () {
                public int getPercentAvailable() {
                    return gc.getPercentAvailable(urn);
                }
                public int getNumberOfLocations() {
                    return gc.getNumberOfLocations(urn);
                }
            };
        }
        
    }
    
    @InspectionPoint("search result handler stats")
    @SuppressWarnings("unused")
    private final Inspectable searchResultHandler = new Inspectable() {
        public Object inspect() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("ver",1);
            synchronized(cncCounter) {
                for (GUID g : cncCounter.keySet()) {
                    Map<URN, ClassCNetworks[]> m = cncCounter.get(g);
                    List<Map<String,byte[]>> toPut = new ArrayList<Map<String,byte[]>>(2);
                    for (ClassCNetworks[] c : m.values()) {
                        Map<String,byte[]> cStats = new HashMap<String,byte[]>();
                        cStats.put("ip",c[0].getTopInspectable(10));
                        cStats.put("alt",c[1].getTopInspectable(10));
                        toPut.add(cStats);
                    }
                    ret.put(g.toHexString(), toPut);
                }
            }
            return ret;
        }
    };

}
