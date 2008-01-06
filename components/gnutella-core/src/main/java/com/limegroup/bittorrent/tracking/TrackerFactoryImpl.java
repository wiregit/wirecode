package com.limegroup.bittorrent.tracking;

import java.net.URI;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class TrackerFactoryImpl implements TrackerFactory {

    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;

    @Inject
    public TrackerFactoryImpl(NetworkManager networkManager,
            ApplicationServices applicationServices) {
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
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
        return new Tracker(uri, context, torrent, networkManager, applicationServices);
    }

}
