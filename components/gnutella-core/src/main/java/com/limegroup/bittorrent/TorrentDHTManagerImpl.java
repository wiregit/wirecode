package com.limegroup.bittorrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.dht.DHTPeerLocator;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;

@Singleton
public class TorrentDHTManagerImpl implements TorrentDHTManager {    

    private static final Log LOG = LogFactory.getLog(TorrentDHTManager.class);
    private final DHTPeerPublisher dhtPeerPublisher;
    private final DHTPeerLocator dhtPeerLocator;

    @Inject
    public TorrentDHTManagerImpl(DHTPeerPublisher dhtPeerPublisher, DHTPeerLocator dhtPeerLocator) {
        this.dhtPeerPublisher = dhtPeerPublisher;
        this.dhtPeerLocator = dhtPeerLocator;
    }

    public void init () {
        dhtPeerPublisher.init();
        dhtPeerLocator.init();
    }
    
    public void handleTorrentEvent(TorrentEvent evt) {
        if (evt.getType() == TorrentEvent.Type.CHUNK_VERIFIED) {
            LOG.debug("CHUNK_VERIFIED_EVENT");
            dhtPeerPublisher.publishYourself(evt.getTorrent());
        }
        else if(evt.getType() == TorrentEvent.Type.TRACKER_FAILED) {
            LOG.debug("TRACKER_FAILED_EVENT");
            dhtPeerLocator.locatePeer(evt.getTorrent());
        }
    }
}
