package com.limegroup.gnutella.altlocs;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.listener.Event;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.Service;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.MagnetDownloader;

@Singleton
public class MagnetDownloaderPushEndpointFinder implements Service, EventListener<DownloadManagerEvent> {

    static final Log LOG = LogFactory.getLog(MagnetDownloaderPushEndpointFinder.class);
    
    private final Provider<DownloadManager> downloadManager;
    private final PushEndpointService pushEndpointManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final AltLocManager altLocManager;
    
    /**
     * Package access for testing.
     */
    final EventListener<DownloadStatusEvent> downloadStatusListener = new EventListener<DownloadStatusEvent>() { 
        public void handleEvent(DownloadStatusEvent event) {
            handleStatusEvent(event);
        }
    };
    
    @Inject
    public MagnetDownloaderPushEndpointFinder(Provider<DownloadManager> downloadManager, 
            @Named("pushEndpointManager") PushEndpointService pushEndpointManager, 
            AlternateLocationFactory alternateLocationFactory,
            AltLocManager altLocManager) {
        this.downloadManager = downloadManager;
        this.pushEndpointManager = pushEndpointManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
    }

    public void handleEvent(DownloadManagerEvent event) {
        switch (event.getType()) {
        case ADDED:
            CoreDownloader downloader = event.getSource();
            if (downloader instanceof MagnetDownloader) {
                MagnetDownloader magnetDownloader = (MagnetDownloader)downloader;
                long size = getSize(magnetDownloader);
                if (size != -1) {
                    // subscribe for status events so we can search when waiting for user
                    magnetDownloader.addListener(this, downloadStatusListener);
                }
                searchForPushEndpoints(magnetDownloader);
            }
            break;
        case REMOVED:
            downloader = event.getSource();
            if (downloader instanceof MagnetDownloader) {
                downloader.removeListener(this, downloadStatusListener);
            }
            break;
        } 
    }
    
    private long getSize(MagnetDownloader downloader) {
        long size = downloader.getMagnet().getFileSize();
        if (size == -1) {
            size = downloader.getContentLength();
        }
        return size;
    }
    
    private void searchForPushEndpoints(MagnetDownloader magnetDownloader) {
        MagnetOptions magnet = magnetDownloader.getMagnet();
        URN sha1Urn = magnet.getSHA1Urn();
        if (sha1Urn == null) {
            return;
        }
        long size = getSize(magnetDownloader);
        if (size == -1) {
            return;
        }
        searchForPushEndpoints(sha1Urn, magnet.getGUIDUrns());
    }
    
    void handleStatusEvent(Event<CoreDownloader, DownloadStatus> event) {
        if (event.getType() == DownloadStatus.WAITING_FOR_USER) {
            CoreDownloader downloader = event.getSource();
            if (downloader instanceof MagnetDownloader) {
                MagnetDownloader magnetDownloader = (MagnetDownloader)downloader;
                searchForPushEndpoints(magnetDownloader);
            }
        }
    }
    
    void searchForPushEndpoints(URN sha1Urn, Set<URN> guidUrns) {
        for (URN guidUrn : guidUrns) {
            try {
                GUID guid = new GUID(guidUrn.getNamespaceSpecificString());
                pushEndpointManager.findPushEndpoint(guid, new PushendpointHandler(sha1Urn));
            } catch (IllegalArgumentException iae) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("invalid hex string of guid", iae);
                }
            }
        }
    }

    public void start() {
        downloadManager.get().addListener(this, this);
    }

    public void stop() {
        downloadManager.get().removeListener(this, this);
    }
    
    private class PushendpointHandler implements SearchListener<PushEndpoint> {
        
        private final URN sha1;

        public PushendpointHandler(URN sha1Urn) {
            this.sha1 = sha1Urn;
        }

        public void handleResult(PushEndpoint result) {
            AlternateLocation alternateLocation = alternateLocationFactory.createPushAltLoc(result, sha1);
            altLocManager.add(alternateLocation, null);
        }

        public void handleSearchDone(boolean success) {
        }
        
    }
}
