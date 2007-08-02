package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.util.EventDispatcher;

public class ManagedTorrentFactoryImpl implements ManagedTorrentFactory {
    
    private final EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NetworkManager networkManager;
    private final TrackerManagerFactory trackerManagerFactory;

    public ManagedTorrentFactoryImpl(
            EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher,
            ScheduledExecutorService scheduledExecutorService,
            NetworkManager networkManager,
            TrackerManagerFactory trackerManagerFactory) {
        this.eventDispatcher = eventDispatcher;
        this.scheduledExecutorService = scheduledExecutorService;
        this.networkManager = networkManager;
        this.trackerManagerFactory = trackerManagerFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.ManagedTorrentFactory#create(com.limegroup.bittorrent.TorrentContext)
     */
    public ManagedTorrent create(TorrentContext context) {
        return new ManagedTorrent(context, eventDispatcher, scheduledExecutorService,
                networkManager, trackerManagerFactory);
    }


}
