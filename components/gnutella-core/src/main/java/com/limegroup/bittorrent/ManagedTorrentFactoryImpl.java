package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.util.EventDispatcher;

@Singleton
public class ManagedTorrentFactoryImpl implements ManagedTorrentFactory {
    
    private final EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NetworkManager networkManager;
    private final TrackerManagerFactory trackerManagerFactory;
    private final ChokerFactory chokerFactory;

    @Inject
    public ManagedTorrentFactoryImpl(
            EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher,
            @Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            NetworkManager networkManager,
            TrackerManagerFactory trackerManagerFactory,
            ChokerFactory chokerFactory) {
        this.eventDispatcher = eventDispatcher;
        this.scheduledExecutorService = scheduledExecutorService;
        this.networkManager = networkManager;
        this.trackerManagerFactory = trackerManagerFactory;
        this.chokerFactory = chokerFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.ManagedTorrentFactory#create(com.limegroup.bittorrent.TorrentContext)
     */
    public ManagedTorrent create(TorrentContext context) {
        return new ManagedTorrent(context, eventDispatcher, scheduledExecutorService,
                networkManager, trackerManagerFactory, chokerFactory);
    }


}
