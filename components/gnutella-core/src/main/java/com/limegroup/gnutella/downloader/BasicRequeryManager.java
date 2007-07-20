package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.util.SyncWrapper;

public class BasicRequeryManager extends RequeryManager {

    public BasicRequeryManager(ManagedDownloader downloader,
            DownloadManager manager, AltLocFinder finder, DHTManager dhtManager) {
        super(downloader, manager, finder, dhtManager);
    }

    private static enum RequeryStatus {
        OFF(false, false), // user hasn't clicked
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
        new SyncWrapper<RequeryStatus>(RequeryStatus.OFF);
    
    @Override
    protected DownloadStatus getDownloadStateForDHTFailure() {
        return requeryStatus.get().canSendGnet() ? 
                DownloadStatus.QUEUED : DownloadStatus.GAVE_UP;
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
    
    @Override
    boolean shouldGiveUp() {
        return isInited() && shouldGiveUpGnutellaQueries();
    }
    
    @Override
    protected boolean shouldGiveUpGnutellaQueries() {
        return !requeryStatus.get().canSendGnet();
    }
    
    @Override
    void handleGaveUpState() {
        if (isInited() && canSendDHTQueryNow()) 
            sendDHTQuery();
    }
    
    @Override
    protected boolean canSendDHTQueryNowImpl() {
        return requeryStatus.get().canSendDHT();
    }
    
    @Override
    protected void updateStateForDHTQuery() {
        switch(requeryStatus.get()) { 
        case INITIAL :
            requeryStatus.set(RequeryStatus.FIRST_DHT); break;
        case GNUTELLA:
        case AUTO_DHT:
            requeryStatus.set(RequeryStatus.AUTO_DHT); break;
        }
    }
    
    @Override
    protected void updateStateForGnetQuery() {
        requeryStatus.set(RequeryStatus.GNUTELLA);
    }
    
    @Override
    protected boolean isWaitingForGnetImpl() {
        return requeryStatus.get() == RequeryStatus.GNUTELLA; 
    }
    
    @Override
    boolean shouldSendRequeryImmediately() {
        return isInited() && !isWaitingForDHTResults() && !shouldGiveUpGnutellaQueries();
    }
    
    @Override
    void init() {
        requeryStatus.set(RequeryStatus.INITIAL);
    }
    
    private boolean isInited() {
        return requeryStatus.get() != RequeryStatus.OFF;
    }
}
