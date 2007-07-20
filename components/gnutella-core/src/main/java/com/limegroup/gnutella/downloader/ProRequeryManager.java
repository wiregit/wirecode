package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;

public class ProRequeryManager extends RequeryManager {

    /** Whether a gnet query was already sent. */
    private volatile boolean sentGnet;
    /** If the user clicked FMS */
    private volatile boolean inited;
    
    public ProRequeryManager(ManagedDownloader downloader,
            DownloadManager manager, AltLocFinder finder, DHTManager dhtManager) {
        super(downloader, manager, finder, dhtManager);
    }

    @Override
    void handleGaveUpState() {
        // always sends a dht query if possible.
        if (canSendDHTQueryNow())
            sendDHTQuery();
    }

    @Override
    void init() {
        inited = true;
    } 


    @Override
    protected boolean isWaitingForGnetImpl() {
        return sentGnet;
    }

    @Override
    void sendRequery() {
        sendGnutellaQuery();
    }

    @Override
    boolean shouldGiveUp() {
        return sentGnet;
    }

    @Override
    boolean shouldSendRequeryImmediately() {
        return inited && !sentGnet;
    }

    @Override
    protected boolean canSendDHTQueryNowImpl() {
        return true;
    }

    @Override
    protected boolean shouldGiveUpGnutellaQueries() {
        return sentGnet;
    }

    @Override
    protected void updateStateForDHTQuery() {}

    @Override
    protected void updateStateForGnetQuery() {
        sentGnet = true;
    }
    
    @Override
    protected DownloadStatus getDownloadStateForDHTFailure() {
        return DownloadStatus.QUEUED;
    }

}
