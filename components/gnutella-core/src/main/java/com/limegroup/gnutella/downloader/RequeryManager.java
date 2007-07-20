package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.settings.LookupSettings;

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
import com.limegroup.gnutella.util.LimeWireUtils;

public abstract class RequeryManager implements DHTEventListener, AltLocSearchListener {

    private static final Log LOG = LogFactory.getLog(RequeryManager.class);
    
    /** 
     * @return an appropriate RequeryManager for this LimeWire.
     */
    public static RequeryManager getManager(ManagedDownloader dm,
            DownloadManager md,
            AltLocFinder alf,
            DHTManager dhm) {
        if (LimeWireUtils.isPro())
            return new ProRequeryManager(dm,md, alf, dhm);
        else
            return new BasicRequeryManager(dm,md,alf,dhm);
    }
    
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
     * The time the last query of either type was sent out.
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
        if (evt.getType() == DHTEvent.Type.STOPPED) {
            handleAltLocSearchDone(false);
            numDHTQueries = 0;
        }
    }
    
    public void handleAltLocSearchDone(boolean success){
        dhtQueryInProgress = false;
        if (!success && downloader.getState() == DownloadStatus.QUERYING_DHT)  
            downloader.setState(getDownloadStateForDHTFailure());
    }
    
    protected abstract DownloadStatus getDownloadStateForDHTFailure();
    
    abstract void sendRequery();
    
    /**
     * @return true if the dht is up and can be used for altloc queries.
     */
    protected boolean isDHTUp() {
        return DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.getValue()
        && dhtManager.isMemberOfDHT();
    }
    
    /**
     * Returns whether or not we should give up 
     */
    abstract boolean shouldGiveUp();
    
    /**
     * Returns whether or not we should give up Gnutella queries
     */
    protected abstract boolean shouldGiveUpGnutellaQueries();
    
    
    /**
     * Returns whether or not we should give up DHT queries
     */
    protected boolean shouldGiveUpDHTQueries() {
        return !isDHTUp() || numDHTQueries >= DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.getValue();
    }
    
    
    /**
     * Notification that the download is in GAVE_UP state and inactive.
     */
    abstract void handleGaveUpState();
    
    
    /**
     * @return true if we can send a dht query right now.
     */
    protected final boolean canSendDHTQueryNow() {
        // dht on & can we do more requeries?
        if (shouldGiveUpDHTQueries()) 
            return false;
        
        // is it too soon?
        if (numDHTQueries > 0 &&
                System.currentTimeMillis() - lastQuerySent < 
                DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.getValue())
            return false;
        
        return canSendDHTQueryNowImpl();
    }
    
    protected abstract boolean canSendDHTQueryNowImpl();
    
    protected final void sendDHTQuery() {
        lastQuerySent = System.currentTimeMillis();
        dhtQueryInProgress = true;
        numDHTQueries++;
        downloader.setState(DownloadStatus.QUERYING_DHT, 
                Math.max(TIME_BETWEEN_REQUERIES, 
                        LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue()));
        
        
        finder.findAltLocs(downloader.getSHA1Urn(), this);
    }
    
    protected abstract void updateStateForDHTQuery();
    
    int getNumGnutellaQueries() {
        return numGnutellaQueries;
    }
    
    /**
     * Sends a Gnutella Query
     */
    protected final boolean sendGnutellaQuery() {
        // If we don't have stable connections, wait until we do.
        if (hasStableConnections()) {
            try {
                QueryRequest qr = downloader.newRequery(numGnutellaQueries);
                if(manager.sendQuery(downloader, qr)) {
                    lastQuerySent = System.currentTimeMillis();
                    numGnutellaQueries++;
                    updateStateForGnetQuery();
                    downloader.setState(DownloadStatus.WAITING_FOR_GNET_RESULTS, TIME_BETWEEN_REQUERIES);
                    return true;
                } else {
                    return false;
                }
            } catch(CantResumeException cre) {
                LOG.debug("CantResumeException", cre);
                return false;
            }
        } else {
            downloader.setState(DownloadStatus.WAITING_FOR_CONNECTIONS, CONNECTING_WAIT_TIME);
            return false;
        }
    }
    
    protected abstract void updateStateForGnetQuery();
    
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
 
    
    final boolean isWaitingForGnutellaResults() {
        return System.currentTimeMillis() - lastQuerySent <  TIME_BETWEEN_REQUERIES &&
        isWaitingForGnetImpl();
    }
    
    protected abstract boolean isWaitingForGnetImpl();
    
    final boolean isWaitingForDHTResults() {
        return dhtQueryInProgress;
    }
    
    long getTimeLeftInQuery() {
        return TIME_BETWEEN_REQUERIES - (System.currentTimeMillis() - lastQuerySent);
    }
    
    abstract boolean shouldSendRequeryImmediately();
    
    abstract void init();
}
