package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.util.SyncWrapper;

public class RequeryManager implements DHTEventListener, AltLocSearchListener {

    private static final Log LOG = LogFactory.getLog(RequeryManager.class);
    
    /**
     * The number of times to requery the network. All requeries are
     * user-driven.
     */
    private static final int GNUTELLA_REQUERY_ATTEMPTS = 1;
    
    /**
     * The time to wait between requeries, in milliseconds.  This time can
     * safely be quite small because it is overridden by the global limit in
     * DownloadManager.  Package-access and non-final for testing.
     * 
     * @see com.limegroup.gnutella.DownloadManager#TIME_BETWEEN_GNUTELLA_REQUERIES 
     */
    static long TIME_BETWEEN_REQUERIES = 5L * 60L * 1000L;  //5 minutes
    
    private static enum RequeryStatus {
        INITIAL(true, true),  // idle  
        FIRST_DHT(true, false),  // first dht request triggered by user.
        GNUTELLA(false, true),  // gnutella request
        AUTO_DHT(false, true); // querying dht continuously. 
        
        private final boolean canSendGnet, canSendDHT;
        RequeryStatus(boolean canSendGnet, boolean canSendDHT) {
            this.canSendGnet = canSendGnet;
            this.canSendDHT = canSendDHT;
        }
        boolean canSendGnet() {
            return canSendGnet;
        }
        boolean canSendDHT() {
            return canSendDHT;
        }
    }
    
    private final SyncWrapper<RequeryStatus> requeryStatus = 
        new SyncWrapper<RequeryStatus>(RequeryStatus.INITIAL);
    
    private final ManagedDownloader downloader;
    
    private final DownloadManager manager;
    
    private final AltLocFinder finder;
    
    private final DHTManager dhtManager;
    
    private volatile boolean dhtQueryInProgress;
    
    /**
     * The number of Gnutella queries already done for this downloader.
     * Influenced by the type of downloader & whether or not it was started
     * from disk or from scratch.
     */
    private volatile int numGnutellaQueries;
    
    /**
     * The number of DHT queries already done for this downloader.
     */
    private volatile int numDHTQueries;
    
    /**
     * The time the last query was sent out.
     */
    private volatile long lastQuerySent;
    
    RequeryManager(ManagedDownloader downloader, 
            DownloadManager manager,
            AltLocFinder finder,
            DHTManager dhtManager) {
        this.downloader = downloader;
        this.manager = manager;
        this.finder = finder;
        this.dhtManager = dhtManager;
        dhtManager.addEventListener(this);
    }
    
    void cleanUp() {
        dhtManager.removeEventListener(this);
    }
    
    public void handleDHTEvent(DHTEvent evt) {
        if (evt.getType() == DHTEvent.Type.STOPPED)
            handleAltLocSearchDone(false);
    }
    
    public void handleAltLocSearchDone(boolean success){
        dhtQueryInProgress = false;
        if (!success && downloader.getState() == DownloadStatus.QUERYING_DHT) 
            downloader.setState(DownloadStatus.QUEUED);
    }
 
    /**
     * Attempts to send a requery.
     * 
     * Try in this order:
     * 
     *     1) DHT
     *     2) Gnutella
     *  -->3) DHT
     *  |__|
     */
    void sendRequery() {
        RequeryStatus status = requeryStatus.get();
        if (isDHTUp() && status.canSendDHT()) 
            sendDHTQuery();
        else if (status.canSendGnet())  
            sendGnutellaQuery();
    }
    
    /**
     * @return true if the dht is up and can be used for altloc queries.
     */
    private boolean isDHTUp() {
        return DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.getValue()
        && dhtManager.isMemberOfDHT();
    }
    
    /**
     * Returns whether or not we should give up 
     */
    boolean shouldGiveUp() {
        return shouldGiveUpGnutellaQueries();
    }
    
    /**
     * Returns whether or not we should give up Gnutella queries
     */
    private boolean shouldGiveUpGnutellaQueries() {
        return numGnutellaQueries >= GNUTELLA_REQUERY_ATTEMPTS;
    }
    
    /**
     * Returns whether or not we should give up DHT queries
     */
    private boolean shouldGiveUpDHTQueries() {
        return !isDHTUp() || numDHTQueries >= DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.getValue();
    }
    
    /**
     * Notification that the download is in GAVE_UP state and inactive.
     */
    void handleGaveUpState() {
        if (canSendDHTQueryNow()) 
            sendDHTQuery();
    }
    
    /**
     * @return true if we can send a dht query right now.
     */
    private boolean canSendDHTQueryNow() {
        // dht on & can we do more requeries?
        if (shouldGiveUpDHTQueries()) 
            return false;
        
        // is it too soon?
        if (System.currentTimeMillis() - lastQuerySent < 
                DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.getValue())
            return false;
        
        return requeryStatus.get().canSendDHT();
    }
    
    void sendDHTQuery() {
        lastQuerySent = System.currentTimeMillis();
        dhtQueryInProgress = true;
        numDHTQueries++;
        downloader.setState(DownloadStatus.QUERYING_DHT, TIME_BETWEEN_REQUERIES);
        switch(requeryStatus.get()) { 
        case INITIAL :
            requeryStatus.set(RequeryStatus.FIRST_DHT); break;
        case GNUTELLA:
        case AUTO_DHT:
            requeryStatus.set(RequeryStatus.AUTO_DHT); break;
        }
        finder.findAltLocs(downloader.getSHA1Urn(), this);
    }
    
    int getNumGnutellaQueries() {
        return numGnutellaQueries;
    }
    
    /**
     * Sends a Gnutella Query
     */
    boolean sendGnutellaQuery() {
        // If we don't have stable connections, wait until we do.
        if (hasStableConnections()) {
            try {
                QueryRequest qr = downloader.newRequery(numGnutellaQueries);
                if(manager.sendQuery(downloader, qr)) {
                    lastQuerySent = System.currentTimeMillis();
                    numGnutellaQueries++;
                    requeryStatus.set(RequeryStatus.GNUTELLA);
                    downloader.setState(DownloadStatus.WAITING_FOR_GNET_RESULTS, TIME_BETWEEN_REQUERIES);
                    return true;
                } else {
                    lastQuerySent = -1; // mark as wanting to requery.
                    return false;
                }
            } catch(CantResumeException cre) {
                LOG.debug("CantResumeException", cre);
                return false;
            }
        } else {
            lastQuerySent = -1; // mark as wanting to requery.
            downloader.setState(DownloadStatus.WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
            return false;
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
        return RouterService.countConnectionsWithNMessages(MIN_CONNECTION_MESSAGES) 
                    >= MIN_NUM_CONNECTIONS &&
               RouterService.getActiveConnectionMessages() >= MIN_TOTAL_MESSAGES;
    }
 
    boolean isWaitingForGnutellaResults() {
        return requeryStatus.get() == RequeryStatus.GNUTELLA && 
        System.currentTimeMillis() - lastQuerySent < TIME_BETWEEN_REQUERIES;
    }
    
    boolean isWaitingForDHTResults() {
        return dhtQueryInProgress;
    }
    
    long getTimeLeftInQuery() {
        return TIME_BETWEEN_REQUERIES - (System.currentTimeMillis() - lastQuerySent);
    }
    boolean shouldSendRequeryImmediately() {
        return lastQuerySent == -1;
    }
    
    void resetState() {
        lastQuerySent = -1; // inform requerying that we wanna go.
        requeryStatus.set(RequeryStatus.INITIAL);
    }
}
