package com.limegroup.bittorrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;

public class TorrentDHTManagerImpl implements TorrentDHTManager {    

    private static final Log LOG = LogFactory.getLog(TorrentDHTManager.class);
    private final DHTPeerPublisher dhtPeerPublisher;    

    @Inject
    public TorrentDHTManagerImpl(DHTPeerPublisher dhtPeerPublisher) {
        this.dhtPeerPublisher = dhtPeerPublisher;
    }

    public void handleTorrentEvent(TorrentEvent evt) {
        if (evt.getType() == TorrentEvent.Type.CHUNK_VERIFIED) {
            LOG.debug("CHUNK_VERIFIED_EVENT");
            dhtPeerPublisher.publishYourself(evt.getTorrent());
        }
    }
}
