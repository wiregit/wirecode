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
import org.limewire.inspection.Inspectable;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.limewire.util.ByteOrder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.util.ClassCNetworks;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Handles incoming search results from the network.  This class parses the 
 * results from <tt>QueryReply</tt> instances and performs the logic 
 * necessary to pass those results up to the UI.
 */
@Singleton
public final class SearchResultHandler implements Inspectable {
    
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
    
    public final NetworkManager networkManager;
    
    @Inject
    public SearchResultHandler(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /*---------------------------------------------------    
      PUBLIC INTERFACE METHODS
     ----------------------------------------------------*/

    /**
     * Adds the Query to the list of queries kept track of.  You should do this
     * EVERY TIME you start a query so we can leaf guide it when possible.
     * Also adds the query to the Spam Manager to adjust percentages.
     *
     * @param qr The query that has been started.  We really just acces the guid.
     */ 
    public void addQuery(QueryRequest qr) {
        LOG.trace("entered SearchResultHandler.addQuery(QueryRequest)");
        if (!qr.isBrowseHostQuery() && !qr.isWhatIsNewRequest())
            ProviderHacks.getSpamManager().startedQuery(qr);
        GuidCount gc = new GuidCount(qr);
        GUID_COUNTS.add(gc);
    }

    /**
     * Removes the Query frome the list of queries kept track of.  You should do
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
            ProviderHacks.getConnectionManager().updateQueryStatus(stat);
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

        List<Response> results = null;
        try {
            results = qr.getResultsAsList();
        } catch (BadPacketException e) {
            LOG.debug("Error gettig results", e);
            return;
        }

        // throw away results that aren't secure.
        int secureStatus = qr.getSecureStatus();
        if(secureStatus == SecureMessage.FAILED) {
            return;
        }
        
        boolean skipSpam = isWhatIsNew(qr) || qr.isBrowseHostReply();
        int numGoodSentToFrontEnd = 0;
        double numBadSentToFrontEnd = 0;
            
        for(Response response : results) {
            if (!qr.isBrowseHostReply() && secureStatus != SecureMessage.SECURE) {
                if (!ProviderHacks.getSearchServices().matchesType(data.getMessageGUID(), response)) {
                    continue;
                }

                if (!ProviderHacks.getSearchServices().matchesQuery(data.getMessageGUID(), response)) {
                    continue;
                }
            }

            // Throw away results from Mandragore Worm
            if (ProviderHacks.getSearchServices().isMandragoreWorm(data.getMessageGUID(), response)) {
                continue;
            }
            
            // If there was an action, only allow it if it's a secure message.
            LimeXMLDocument doc = response.getDocument();
            if(ApplicationSettings.USE_SECURE_RESULTS.getValue() &&
               doc != null && !"".equals(doc.getAction()) && secureStatus != SecureMessage.SECURE) {
                continue;
            }
            
            // we'll be showing the result to the user, count it
            countClassC(qr,response);
            RemoteFileDesc rfd = response.toRemoteFileDesc(data);
            rfd.setSecureStatus(secureStatus);
            Set<? extends IpPort> alts = response.getLocations();
            ProviderHacks.getActivityCallback().handleQueryResult(rfd, data, alts);
            
            if (skipSpam || !ProviderHacks.getSpamManager().isSpam(rfd))
                numGoodSentToFrontEnd++;
            else 
                numBadSentToFrontEnd++;
        } //end of response loop
        
        numBadSentToFrontEnd = Math.ceil(numBadSentToFrontEnd * SearchSettings.SPAM_RESULT_RATIO.getValue());
        accountAndUpdateDynamicQueriers(qr, numGoodSentToFrontEnd + (int)numBadSentToFrontEnd);
    }

    private void countClassC(QueryReply qr, Response r) {
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

    private void accountAndUpdateDynamicQueriers(final QueryReply qr,
                                                 final int numGoodSentToFrontEnd) {

        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): entered.");
        // we should execute if results were consumed
        // technically Ultrapeers don't use this info, but we are keeping it
        // around for further use
        if (numGoodSentToFrontEnd > 0) {
            // get the correct GuidCount
            GuidCount gc = retrieveGuidCount(new GUID(qr.getGUID()));
            if (gc == null)
                // 0. probably just hit lag, or....
                // 1. we could be under attack - hits not meant for us
                // 2. programmer error - ejected a query we should not have
                return;
            
            // update the object
            LOG.trace("SRH.accountAndUpdateDynamicQueriers(): incrementing.");
            gc.increment(numGoodSentToFrontEnd);

            // inform proxying Ultrapeers....
            if (ProviderHacks.getConnectionServices().isShieldedLeaf()) {
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
                    ProviderHacks.getConnectionManager().updateQueryStatus(stat);
                }

            }
        }
        LOG.trace("SRH.accountAndUpdateDynamicQueriers(): returning.");
    }


    private GuidCount removeQueryInternal(GUID guid) {
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


    private GuidCount retrieveGuidCount(GUID guid) {
        synchronized (GUID_COUNTS) {
            for(GuidCount currGC : GUID_COUNTS) {
                if (currGC.getGUID().equals(guid))
                    return currGC;
            }
        }
        return null;
    }
    
    private boolean isWhatIsNew(QueryReply reply) {
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
    private boolean isQueryStillValid(GuidCount gc, long now) {
        LOG.trace("entered SearchResultHandler.isQueryStillValid(GuidCount)");
        return (now < (gc.getTime() + QUERY_EXPIRE_TIME)) &&
               (gc.getNumResults() < QueryHandler.ULTRAPEER_RESULTS);
    }

    /*---------------------------------------------------    
      END OF PRIVATE INTERFACE METHODS
     ----------------------------------------------------*/
    
    /** A container that simply pairs a GUID and an int.  The int should
     *  represent the number of non-filtered results for the GUID.
     */
    private static class GuidCount {

        private final long _time;
        private final GUID _guid;
        private final QueryRequest _qr;
        private int _numGoodResults;
        private int _nextReportNum = REPORT_INTERVAL;
        private boolean markAsFinished = false;
        
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
        public boolean isFinished() { return markAsFinished; }
        public void tallyReport() { 
            _nextReportNum = _numGoodResults + REPORT_INTERVAL; 
        }

        public void increment(int good) {
			_numGoodResults += good;
		}
        
        public void markAsFinished() { markAsFinished = true; }

        public String toString() {
            return "" + _guid + ":" + _numGoodResults + ":" + _nextReportNum;
        }
    }
    
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

}
