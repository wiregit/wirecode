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
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.security.SecureMessage;
import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
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
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.util.ClassCNetworks;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Search result information is now tabulated on a per-URN basis with GuidCount
 * pairing a URN with a ResourceLocationCounter. Whole and partial results are
 * accounted for correctly.
 *
 */
@Singleton
class SearchResultHandlerImpl implements SearchResultHandler {

    private static final Log LOG =
        LogFactory.getLog(SearchResultHandler.class);
        
    /**
     * The maximum amount of time to allow a query's processing
     * to pass before giving up on it as an 'old' query.
     */
    private static final int QUERY_EXPIRE_TIME = 30 * 1000; // 30 seconds.

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
    private final NetworkInstanceUtils networkInstanceUtils;

    @Inject
    public SearchResultHandlerImpl(NetworkManager networkManager,
            SearchServices searchServices,
            Provider<ActivityCallback> activityCallback,
            Provider<ConnectionManager> connectionManager,
            ConnectionServices connectionServices,
            Provider<SpamManager> spamManager,
            RemoteFileDescFactory remoteFileDescFactory,
            NetworkInstanceUtils networkInstanceUtils) {
        this.networkManager = networkManager;
        this.searchServices = searchServices;
        this.activityCallback = activityCallback;
        this.connectionManager = connectionManager;
        this.connectionServices = connectionServices;
        this.spamManager = spamManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    public SearchResultStats addQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        if (!qr.isBrowseHostQuery() && !qr.isWhatIsNewRequest())
            spamManager.get().startedQuery(qr);
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
        
        return gc;
    }

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

    public int getNumResultsForQuery(GUID guid) {
        GuidCount gc = retrieveResultStats(guid);
        if (gc != null)
            return gc.getNumResults();
        else
            return -1;
    }
    
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
            if(qr.getSpeed() < SearchSettings.MINIMUM_SEARCH_SPEED.getValue()) {
                LOG.debug("Ignoring because low speed");
                return;
            }
            // if the other side is firewalled AND
            // we're not on close IPs AND
            // (we are firewalled OR we are a private IP) AND 
            // no chance for FW transfer then drop the reply.
            if(data.isFirewalled() && 
               !networkInstanceUtils.isVeryCloseIP(qr.getIPBytes()) &&               
               (!networkManager.acceptedIncomingConnection() ||
                networkInstanceUtils.isPrivateAddress(networkManager.getAddress())) &&
               !(networkManager.canDoFWT() && 
                 qr.getSupportsFWTransfer())
               )  {
               LOG.debug("Ignoring from firewall funkiness");
               return;
            }
        }

        int numGoodToSendToFrontEnd = addQueryReply(qr, data);
        accountAndUpdateDynamicQueriers(qr, data, numGoodToSendToFrontEnd);
    }
    
    public int addQueryReply(QueryReply qr, HostData data) {
            List<Response> results = null;
            double numBad = 0;
            int numGood = 0;
            
            try {
                results = qr.getResultsAsList();
            } catch (BadPacketException e) {
                LOG.debug("Error getting results", e);
                return 0;
            }

            // throw away results that aren't secure.
            int secureStatus = qr.getSecureStatus();
            if(secureStatus == SecureMessage.FAILED)
                return 0;
            
            boolean skipSpam = isWhatIsNew(qr) || qr.isBrowseHostReply();

            for (Response response : results) {
                if (!qr.isBrowseHostReply() && secureStatus != SecureMessage.SECURE) {
                    if (!searchServices.matchesType(data.getMessageGUID(), response))
                        continue;

                    if (!searchServices.matchesQuery(data.getMessageGUID(), response))
                        continue;
                }

                // Throw away results from Mandragore Worm
                if (searchServices.isMandragoreWorm(data.getMessageGUID(), response))
                    continue;
                
                // If there was an action, only allow it if it's a secure message.
                LimeXMLDocument doc = response.getDocument();
                if (ApplicationSettings.USE_SECURE_RESULTS.getValue() &&
                   doc != null && !"".equals(doc.getAction()) && secureStatus != SecureMessage.SECURE) {
                   continue;
                }
                
                // we'll be showing the result to the user, count it
                countClassC(qr,response);
                RemoteFileDesc rfd = response.toRemoteFileDesc(data, remoteFileDescFactory);
                rfd.setSecureStatus(secureStatus);
                
                if (skipSpam || !spamManager.get().isSpam(rfd)) {
                    numGood += addSource(response, qr);
                }
                else {
                    numBad++;
                }
                
                activityCallback.get().handleQueryResult(rfd, data, response.getLocations());
                    
            } //end of response loop
            
            numGood += Math.ceil(numBad * SearchSettings.SPAM_RESULT_RATIO.getValue());
            GuidCount gc = retrieveResultStats(new GUID(qr.getGUID()));
            if(gc != null) {
                gc.increment(numGood);
            }
            
            return numGood;
        }

    private int addSource(Response response, QueryReply reply) {
        if (response.getRanges() != null)
            return addPartialSource(response, reply);
        else
            return addCompleteSource(response, reply);
    }

    private int addPartialSource(Response response, QueryReply qr) {
        Set<URN> urns = response.getUrns();
        IntervalSet is = response.getRanges();
        long size = response.getSize();
        ResourceLocationCounter rlc = null;
        URN urn = getFirstSha1Urn(urns);
        int count_dif = 0;
        
        if (urn != null) {
            GuidCount gc = retrieveResultStats(new GUID(qr.getGUID()));
            if(gc != null) {
                if (null == (rlc = gc.getIntervalSets().get(urn)))
                    gc.getIntervalSets().put(urn, (rlc = new ResourceLocationCounter(urn, size)));
                
                int count_before = rlc.getLocationCount();
                rlc.addPartialSource( is );
                int count_after = rlc.getLocationCount();
                
                count_dif = count_after - count_before;
                
                if (count_dif > 0)
                    rlc.updateDisplayLocationCount(count_dif);    
            }
        }
        
        return count_dif;
    }
        
    private int addCompleteSource(Response response, QueryReply qr) {
        Set<URN> urns = response.getUrns();
        long size = response.getSize();
        int altCnt = response.getLocations().size();
        ResourceLocationCounter rlc = null;
        URN urn = getFirstSha1Urn(urns);
        int maxAlts = FilterSettings.MAX_ALTS_TO_DISPLAY.getValue();
        
        if (urn != null) {
            GuidCount gc = retrieveResultStats(new GUID(qr.getGUID()));
            if(gc != null) {
                if (null == (rlc = gc.getIntervalSets().get(urn)))
                    gc.getIntervalSets().put(urn, (rlc = new ResourceLocationCounter(urn, size)));
                
                rlc.incrementWholeSources();
                rlc.updateDisplayLocationCount(altCnt > maxAlts ? maxAlts+1 : altCnt+1);
            }
        }
        return 1;
    }
    
    /**
     * Returns the first SHA1 URN from the urns set, if there is one.
     * Null, otherwise.
     */
    private URN getFirstSha1Urn(Set<URN> urns) {
        for (URN urn : urns) {
            if (urn.isSHA1())
                return urn;
        }        
        return null;
    }

    void countClassC(QueryReply qr, Response r) {
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
                cnc[0].add(ByteUtils.beb2int(qr.getIPBytes(), 0), 1);
                cnc[1].addAll(r.getLocations());
            }
        }
    }

    void accountAndUpdateDynamicQueriers(final QueryReply qr, HostData data,
                                                 final int numGoodSentToFrontEnd) {
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");
        
        // get the correct GuidCount
        GuidCount gc = retrieveResultStats(new GUID(qr.getGUID()));
        
        if (gc == null)
            // 0. probably just hit lag, or....
            // 1. we could be under attack - hits not meant for us
            // 2. programmer error - ejected a query we should not have
            return;
            
        // we should execute if results were consumed. technically Ultrapeers 
        // don't use this info, but we are keeping it around for further use.
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


    GuidCount removeQueryInternal(GUID guid) {
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


    /**
     * 
     * @param guid
     * @return the object by which the stats for the specified GUID can be 
     *         retrieved.
     */
    GuidCount retrieveResultStats(GUID guid) {
        synchronized (GUID_COUNTS) {
            for(GuidCount currGC : GUID_COUNTS) {
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        
        return null;
    }
    
    boolean isWhatIsNew(QueryReply reply) {
        GuidCount gc = retrieveResultStats(new GUID(reply.getGUID()));
        return gc != null && gc.getQueryRequest().isWhatIsNewRequest();
    }
    
    /**
     * Determines whether or not the query contained in the
     * specified GuidCount is still valid.
     * This depends on values such as the time the query was
     * created and the amount of results we've received so far
     * for this query.
     */
    boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    class GuidCount implements SearchResultStats {

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
            return _numGoodResults;
        }
        
        public int getNumResultsForURN(URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return 0;
            else
                return rlc.getLocationCount();
        }
        
        public int getNumDisplayResultsForURN(URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return 0;
            else
                return rlc.getDisplayLocationCount();
        }
        
        public int getNextReportNum() { return _nextReportNum; }
        
        /**
         * Time at which query was started.
         */
        public long getTime() { return _time; }
        
        /**
         * Returns the QueryRequest instance associated with this GuidCount.
         */
        public QueryRequest getQueryRequest() { return _qr; }
        
        /**
         * Notes whether this query has been marked as being finished.
         */
        public boolean isFinished() { return markAsFinished; }
        
        public void tallyReport() { 
            _nextReportNum = _numGoodResults + REPORT_INTERVAL; 
        }

        public float getPercentAvailable (URN urn) {
            ResourceLocationCounter rlc = _isets.get(urn);
            
            if (rlc == null)
                return (float)0.0;
            
            return rlc.getPercentAvailable();
        }
        
        /**
         * Increases the "good" search result count by good.
         */
        public void increment(int good) {
            _numGoodResults += good;
        }
        
        /**
         * Marks this search as being finished.
         */
        public void markAsFinished() { markAsFinished = true; }

        public String toString() {
            return "" + _guid + ":" + _numGoodResults + ":" + _nextReportNum;
        }

        public Map<URN, ResourceLocationCounter> getIntervalSets() {
            return _isets;
        }

        public QueryResultHandler getResultHandler (final URN urn) {
            final GuidCount gc = this;
            
            return new QueryResultHandler () {
                public float getPercentAvailable() {
                    return gc.getPercentAvailable(urn);
                }
                public int getNumberOfLocations() {
                    return gc.getNumResultsForURN(urn);
                }
                public int getNumberOfDisplayLocations() {
                    return gc.getNumDisplayResultsForURN(urn);
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
