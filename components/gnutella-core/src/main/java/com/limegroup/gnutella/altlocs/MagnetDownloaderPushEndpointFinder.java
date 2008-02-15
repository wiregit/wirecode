package com.limegroup.gnutella.altlocs;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.listener.DefaultEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.dht.db.PushEndpointManager;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadManagerEvent;
import com.limegroup.gnutella.downloader.DownloadManagerListener;
import com.limegroup.gnutella.downloader.MagnetDownloader;

@Singleton
public class MagnetDownloaderPushEndpointFinder implements DownloadManagerListener {

    static final Log LOG = LogFactory.getLog(MagnetDownloaderPushEndpointFinder.class);
    
    private final DownloadManager downloadManager;
    private final PushEndpointManager pushEndpointManager;
    private final AlternateLocationFactory alternateLocationFactory;
    private final AltLocManager altLocManager;
    
    @Inject
    public MagnetDownloaderPushEndpointFinder(DownloadManager downloadManager, 
            PushEndpointManager pushEndpointManager, AlternateLocationFactory alternateLocationFactory,
            AltLocManager altLocManager) {
        this.downloadManager = downloadManager;
        this.pushEndpointManager = pushEndpointManager;
        this.alternateLocationFactory = alternateLocationFactory;
        this.altLocManager = altLocManager;
    }

    public void handleEvent(DefaultEvent<CoreDownloader, DownloadManagerEvent> event) {
        switch (event.getType()) {
        case ADDED:
            CoreDownloader downloader = event.getSource();
            // TODO also when running out of sources
            if (downloader instanceof MagnetDownloader) {
                MagnetDownloader magnetDownloader = (MagnetDownloader)downloader;
                MagnetOptions magnet = magnetDownloader.getMagnet();
                URN sha1Urn = magnet.getSHA1Urn();
                if (sha1Urn == null) {
                    return;
                }
                searchForPushEndpoints(sha1Urn, magnet.getGUIDUrns());
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
        downloadManager.addDownloadManagerListener(this);
    }

    public void stop() {
        downloadManager.removeDownloadManagerListener(this);
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
