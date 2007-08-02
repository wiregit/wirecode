package com.limegroup.bittorrent.tracking;

import org.apache.commons.httpclient.URI;

import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.gnutella.NetworkManager;

public class TrackerFactoryImpl implements TrackerFactory {

    private final NetworkManager networkManager;

    public TrackerFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.tracking.TrackerFactory#create(org.apache.commons.httpclient.URI,
     *      com.limegroup.bittorrent.TorrentContext,
     *      com.limegroup.bittorrent.ManagedTorrent)
     */
    public Tracker create(URI uri, TorrentContext context,
            ManagedTorrent torrent) {
        return new Tracker(uri, context, torrent, networkManager);
    }

}
