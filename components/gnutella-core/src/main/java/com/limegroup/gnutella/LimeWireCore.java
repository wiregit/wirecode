package com.limegroup.gnutella;



import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.SourceRankerFactory;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;

public class LimeWireCore {
    
    // DPINJ:  This would go away eventually, once we don't need ProviderHacks to have a single instance of LimeWireCore
    public static LimeWireCore create(Module... modules) {
        Module[] allModules = new Module[modules.length + 1];
        System.arraycopy(modules, 0, allModules, 0, modules.length);
        allModules[allModules.length-1] = new LimeWireCoreModule();
        Injector injector = Guice.createInjector(allModules);
        LimeWireCore core = injector.getInstance(LimeWireCore.class);
        return core;
    }
    
    @Inject private Injector injector;
    @Inject private Provider<LocalFileDetailsFactory> localFileDetailsFactory;
    @Inject private Provider<AlternateLocationFactory> alternateLocationFactory;
    @Inject private Provider<AltLocValueFactory> altLocValueFactory;
    @Inject private Provider<DiskController> diskController;
    @Inject private Provider<VerifyingFileFactory> verifyingFileFactory;
    @Inject private Provider<SocketsManager> socketsManager;
    @Inject private Provider<SourceRankerFactory> sourceRankerFactory;
    @Inject private Provider<HostDataFactory> hostDataFactory;
    @Inject private Provider<ManagedConnectionFactory> managedConnectionFactory;
    @Inject private Provider<QueryRequestFactory> queryRequestFactory;
    @Inject private Provider<QueryHandlerFactory> queryHandlerFactory;
    @Inject private Provider<UploadSlotManager> uploadSlotManager;
    @Inject private Provider<FileManager> fileManager;
    @Inject private Provider<UploadManager> uploadManager;
    @Inject private Provider<HeadPongFactory> headPongFactory;
    @Inject private Provider<HTTPDownloaderFactory> httpDownloaderFactory;
    @Inject private Provider<DownloadWorkerFactory> downloadWorkerFactory;
    @Inject private Provider<FeaturesWriter> featuresWriter;
    @Inject private Provider<HTTPHeaderUtils> httpHeaderUtils;
    @Inject private Provider<TrackerFactory> trackerFactory;
    @Inject private Provider<TrackerManagerFactory> trackerManagerFactory;
    @Inject private Provider<TorrentManager> torrentManager;
    @Inject private Provider<ManagedTorrentFactory> managedTorrentFactory;
    @Inject private Provider<PushEndpointFactory> pushEndpointFactory;
    @Inject private Provider<HeadersFactory> headersFactory;
    @Inject private Provider<HandshakeResponderFactory> handshakeResponderFactory;
    @Inject private Provider<PushProxiesValueFactory> pushProxiesValueFactory;
    @Inject private Provider<PingReplyFactory> pingReplyFactory;
    @Inject private Provider<DHTControllerFactory> dhtControllerFactory;
    @Inject private Provider<DHTManager> dhtManager;
    @Inject private Provider<ConnectionManager> connectionManager;
    @Inject private Provider<NetworkManager> networkManager;
    @Inject private Provider<UDPService> udpService;
    @Inject private Provider<Acceptor> acceptor;
    @Inject private Provider<ForMeReplyHandler> forMeReplyHandler;
    @Inject private Provider<QueryUnicaster> queryUnicaster;
    @Inject private Provider<OnDemandUnicaster> onDemandUnicaster;
    @Inject private Provider<MessageRouter> messageRouter;
    @Inject private Provider<DownloadManager> downloadManager;
    @Inject private Provider<AltLocFinder> altLocFinder;

    public Injector getInjector() {
        return injector;
    }

    public LocalFileDetailsFactory getLocalFileDetailsFactory() {
        return localFileDetailsFactory.get();
    }

    public AlternateLocationFactory getAlternateLocationFactory() {
        return alternateLocationFactory.get();
    }

    public AltLocValueFactory getAltLocValueFactory() {
        return altLocValueFactory.get();
    }

    public DiskController getDiskController() {
        return diskController.get();
    }

    public VerifyingFileFactory getVerifyingFileFactory() {
        return verifyingFileFactory.get();
    }

    public SocketsManager getSocketsManager() {
        return socketsManager.get();
    }

    public SourceRankerFactory getSourceRankerFactory() {
        return sourceRankerFactory.get();
    }

    public HostDataFactory getHostDataFactory() {
        return hostDataFactory.get();
    }

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return managedConnectionFactory.get();
    }

    public QueryRequestFactory getQueryRequestFactory() {
        return queryRequestFactory.get();
    }

    public QueryHandlerFactory getQueryHandlerFactory() {
        return queryHandlerFactory.get();
    }

    public UploadSlotManager getUploadSlotManager() {
        return uploadSlotManager.get();
    }

    public FileManager getFileManager() {
        return fileManager.get();
    }

    public UploadManager getUploadManager() {
        return uploadManager.get();
    }

    public HeadPongFactory getHeadPongFactory() {
        return headPongFactory.get();
    }

    public HTTPDownloaderFactory getHttpDownloaderFactory() {
        return httpDownloaderFactory.get();
    }

    public DownloadWorkerFactory getDownloadWorkerFactory() {
        return downloadWorkerFactory.get();
    }

    public FeaturesWriter getFeaturesWriter() {
        return featuresWriter.get();
    }

    public HTTPHeaderUtils getHttpHeaderUtils() {
        return httpHeaderUtils.get();
    }

    public TrackerFactory getTrackerFactory() {
        return trackerFactory.get();
    }

    public TrackerManagerFactory getTrackerManagerFactory() {
        return trackerManagerFactory.get();
    }

    public TorrentManager getTorrentManager() {
        return torrentManager.get();
    }

    public ManagedTorrentFactory getManagedTorrentFactory() {
        return managedTorrentFactory.get();
    }

    public PushEndpointFactory getPushEndpointFactory() {
        return pushEndpointFactory.get();
    }

    public HeadersFactory getHeadersFactory() {
        return headersFactory.get();
    }

    public HandshakeResponderFactory getHandshakeResponderFactory() {
        return handshakeResponderFactory.get();
    }

    public PushProxiesValueFactory getPushProxiesValueFactory() {
        return pushProxiesValueFactory.get();
    }

    public PingReplyFactory getPingReplyFactory() {
        return pingReplyFactory.get();
    }

    public DHTControllerFactory getDhtControllerFactory() {
        return dhtControllerFactory.get();
    }

    public DHTManager getDhtManager() {
        return dhtManager.get();
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager.get();
    }

    public NetworkManager getNetworkManager() {
        return networkManager.get();
    }

    public UDPService getUdpService() {
        return udpService.get();
    }

    public Acceptor getAcceptor() {
        return acceptor.get();
    }
    
    public ForMeReplyHandler getForMeReplyHandler() {
        return forMeReplyHandler.get(); 
    }
    
    public QueryUnicaster getQueryUnicaster() {
        return queryUnicaster.get();
    }
    
    public OnDemandUnicaster getOnDemandUnicaster() {
        return onDemandUnicaster.get(); 
    }

    public MessageRouter getMessageRouter() {
        return messageRouter.get();
    }

    public DownloadManager getDownloadManager() {
        return downloadManager.get();
    }

    public AltLocFinder getAltLocFinder() {
        return altLocFinder.get();
    }

}
