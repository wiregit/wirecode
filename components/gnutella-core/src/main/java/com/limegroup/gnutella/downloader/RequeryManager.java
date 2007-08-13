package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 *  A manager for controlling how requeries are sent in downloads.
 *  This abstract class has specific functionality that differs for
 *  Basic & PRO.
 *  
 *  The manager keeps track of what queries have been sent out,
 *  when queries can begin, and how long queries should wait for results.
 */
public class RequeryManager implements DHTEventListener, AltLocSearchListener {

    private static final Log LOG = LogFactory.getLog(RequeryManager.class);
    
    /** The types of requeries that can be currently active. */
    public static enum QueryType { DHT, GNUTELLA };
    
    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * 
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_GNUTELLA_REQUERIES 
     */
    static long TIME_BETWEEN_REQUERIES = 5L * 60L * 1000L;  //5 minutes
    
    private final ManagedDownloader downloader;
    
    private final DownloadManager manager;
    
    private final AltLocFinder finder;
    
    private final DHTManager dhtManager;
    
    /** The type of the last query this sent out. */
    private volatile QueryType lastQueryType;
    
    /** The number of DHT queries already done for this downloader. */
    private volatile int numDHTQueries;
    
    /** The time the last query of either type was sent out. */
    private volatile long lastQuerySent;
    
    /** True if a gnutella query has been sent. */
    protected volatile boolean sentGnutellaQuery;
    
    /** True if requerying has been activated by the user. */
    protected volatile boolean activated;
    
    /** 
     * a dht lookup to Shutdownable if we get shut down 
     * not null if a DHT query is currently out (and not finished, success or failure)
     */
    private volatile Shutdownable dhtQuery;
    
    private final ConnectionServices connectionServices;
    
    RequeryManager(ManagedDownloader downloader, 
            DownloadManager manager,
            AltLocFinder finder,
            DHTManager dhtManager,
            ConnectionServices connectionServices) {
        this.downloader = downloader;
        this.manager = manager;
        this.finder = finder;
        this.dhtManager = dhtManager;
        this.connectionServices = connectionServices;
        dhtManager.addEventListener(this);
    }
    
    /** Returns true if we're currently waiting for any kinds of results. */
    boolean isWaitingForResults() {
        if(lastQueryType == null)
            return false;
        
        switch(lastQueryType) {
        case DHT: return dhtQuery != null && getTimeLeftInQuery() > 0;
        case GNUTELLA: return getTimeLeftInQuery() > 0;
        }
        
        return false;
    }
    
    /** Returns the kind of last requery we sent. */
    QueryType getLastQueryType() {
        return lastQueryType;
    }
    
    /** Returns how much time, in milliseconds, is left in the current query. */
    long getTimeLeftInQuery() {
        return TIME_BETWEEN_REQUERIES - (System.currentTimeMillis() - lastQuerySent);
    }
        
    /** Sends a requery, if allowed. */
    void sendQuery() {
        if(canSendQueryNow()) {
            if(canSendDHTQueryNow())
                sendDHTQuery();
            else if(!sentGnutellaQuery)
                sendGnutellaQuery();
        }
    }
        
    /** True if a requery can immediately be performed or can be triggered from a user action. */
    boolean canSendQueryAfterActivate() {
        return !sentGnutellaQuery || canSendDHTQueryNow();
    }
    
    /** Returns true if a query can be sent right now. */
    boolean canSendQueryNow() {
        // PRO users can always send the DHT query, but only Gnutella after activate.
        if(LimeWireUtils.isPro())
            return canSendDHTQueryNow() || (activated && canSendQueryAfterActivate());
        else
            return activated && canSendQueryAfterActivate();
    }
    
    /** Allows activated queries to begin. */
    void activate() {
        activated = true;
    }
    
   /** Removes all event listeners, cancels ongoing searches and cleans up references. */
    void cleanUp() {
        Shutdownable f = dhtQuery;
        dhtQuery = null;
        if (f != null)
            f.shutdown();
        dhtManager.removeEventListener(this);
    }
    
    /** Specifically, this cancels the DHT query if the DHT is stopped. */
    public void handleDHTEvent(DHTEvent evt) {
        if (evt.getType() == DHTEvent.Type.STOPPED) {
            handleAltLocSearchDone(false);
            numDHTQueries = 0;
        }
    }
    
    public void handleAltLocSearchDone(boolean success){
        dhtQuery = null;
        // This changes the state to GAVE_UP regardless of success,
        // because even if this was a success (it found results),
        // it's possible the download isn't going to want to use
        // those results.
        downloader.setStateIfExistingStateIs(DownloadStatus.GAVE_UP, DownloadStatus.QUERYING_DHT);
    }
    
    /**
     * @return true if the dht is up and can be used for altloc queries.
     */
    private boolean isDHTUp() {
        return DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.getValue()
                && dhtManager.isMemberOfDHT();
    }
    
    /** True if another DHT query can be sent right now. */
    private boolean canSendDHTQueryNow() {
        if (!isDHTUp())
            return false;
        return numDHTQueries == 0 || 
        (numDHTQueries < DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.getValue()
                && System.currentTimeMillis() - lastQuerySent >= 
                    DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.getValue()
        );
    }
    
    private void sendDHTQuery() {
        lastQuerySent = System.currentTimeMillis();
        lastQueryType = QueryType.DHT;
        numDHTQueries++;
        downloader.setState(DownloadStatus.QUERYING_DHT, 
                Math.max(TIME_BETWEEN_REQUERIES, 
                        LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue()));
        dhtQuery = finder.findAltLocs(downloader.getSHA1Urn(), this);
    }
    
    /** Sends a Gnutella Query */
    private void sendGnutellaQuery() {
        // If we don't have stable connections, wait until we do.
        if (hasStableConnections()) {
            try {
                QueryRequest qr = downloader.newRequery(0);
                if(manager.sendQuery(downloader, qr)) {
                    sentGnutellaQuery = true;
                    lastQueryType = QueryType.GNUTELLA;
                    lastQuerySent = System.currentTimeMillis();
                    downloader.setState(DownloadStatus.WAITING_FOR_GNET_RESULTS, TIME_BETWEEN_REQUERIES);
                } else {
                    throw new IllegalStateException("manager must sent query!");
                }
            } catch(CantResumeException cre) {
                sentGnutellaQuery = true;
                downloader.setState(DownloadStatus.GAVE_UP);
                LOG.debug("CantResumeException", cre);
            }
        } else {
            downloader.setState(DownloadStatus.WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
        }
    }
    
    /**
     * How long we'll wait before attempting to download again after checking
     * for stable connections (and not seeing any)
     */
    private static final int CONNECTING_WAIT_TIME = 750;
    private static final int MIN_NUM_CONNECTIONS      = 2;
    private static final int MIN_CONNECTION_MESSAGES  = 6;
    private static final int MIN_TOTAL_MESSAGES       = 45;
    static boolean   NO_DELAY                 = false; // For testing
    
    /**
     *  Determines if we have any stable connections to send a requery down.
     */
    private boolean hasStableConnections() {
        if ( NO_DELAY )
            return true;  // For Testing without network connection

        // TODO: Note that on a private network, these conditions might
        //       be too strict.
        
        // Wait till your connections are stable enough to get the minimum 
        // number of messages
        return connectionServices.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
                    >= MIN_NUM_CONNECTIONS &&
                    connectionServices.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }
}
