package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.util.EventDispatcher;

@Singleton
public class ManagedTorrentFactoryImpl implements ManagedTorrentFactory {
    
    private final EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NetworkManager networkManager;
    private final TrackerManagerFactory trackerManagerFactory;
    private final ChokerFactory chokerFactory;
    private final BTLinkManagerFactory linkManagerFactory;
    private final BTConnectionFetcherFactory connectionFetcherFactory;
    private final Provider<ContentManager> contentManager;
    private final Provider<IPFilter> ipFilter;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<FileManager> fileManager;
    private final PeerLocatorFactory peerLocatorFactory;

    @Inject
    public ManagedTorrentFactoryImpl(
            EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher,
            @Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            NetworkManager networkManager,
            TrackerManagerFactory trackerManagerFactory,
            ChokerFactory chokerFactory,
            BTLinkManagerFactory linkManagerFactory,
            BTConnectionFetcherFactory connectionFetcherFactory,
            Provider<ContentManager> contentManager,
            @Named("ipFilter") Provider<IPFilter> ipFilter,
            Provider<TorrentManager> torrentManager,
            Provider<FileManager> fileManager,
            PeerLocatorFactory peerLocatorFactory) {
        this.eventDispatcher = eventDispatcher;
        this.scheduledExecutorService = scheduledExecutorService;
        this.networkManager = networkManager;
        this.trackerManagerFactory = trackerManagerFactory;
        this.chokerFactory = chokerFactory;
        this.linkManagerFactory = linkManagerFactory;
        this.connectionFetcherFactory = connectionFetcherFactory;
        this.contentManager = contentManager;
        this.ipFilter = ipFilter;
        this.torrentManager = torrentManager;
        this.fileManager = fileManager;
        this.peerLocatorFactory = peerLocatorFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.ManagedTorrentFactory#create(com.limegroup.bittorrent.TorrentContext)
     */
    public ManagedTorrent create(TorrentContext context) {
        return new ManagedTorrent(context, eventDispatcher, scheduledExecutorService,
                networkManager, trackerManagerFactory, chokerFactory, linkManagerFactory,
                connectionFetcherFactory, contentManager.get(), ipFilter.get(), torrentManager.get(),
                fileManager.get(), peerLocatorFactory);
    }


}
