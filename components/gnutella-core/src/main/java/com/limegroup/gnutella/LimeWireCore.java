package com.limegroup.gnutella;

import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.HTTPAcceptor;
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
import com.limegroup.gnutella.filters.HostileFilter;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.statistics.QueryStats;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;

public class LimeWireCore {
        
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
    @Inject private Provider<ConnectionDispatcher> connectionDispatcher;
    @Inject private Provider<HTTPAcceptor> httpAcceptor;
    @Inject private Provider<HostCatcher> hostCatcher;
    @Inject private Provider<com.limegroup.gnutella.HTTPAcceptor> httpUploadAcceptor;
    @Inject private Provider<PushManager> pushManager;
    @Inject private Provider<ResponseVerifier> responseVerifier;
    @Inject private Provider<SearchResultHandler> searchResultHandler;
    @Inject private Provider<AltLocManager> altLocManager;
    @Inject private Provider<ContentManager> contentManager;
    @Inject private Provider<IPFilter> ipFilter;
    @Inject private Provider<HostileFilter> hostileFilter;
    @Inject private Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    @Inject private Provider<BandwidthManager> bandwidthManager;
    @Inject private Provider<HttpExecutor> httpExecutor;
    @Inject private Provider<QueryStats> queryStats;
    @Inject private Provider<NodeAssigner> nodeAssigner;
    @Inject private Provider<Statistics> statistics;
    @Inject private Provider<SecureMessageVerifier> secureMessageVerifier;
    @Inject private Provider<CreationTimeCache> creationTimeCache;
    @Inject private Provider<HttpRequestHandlerFactory> httpRequestHandlerFactory;
    @Inject private Provider<UrnCache> urnCache;
    @Inject private Provider<FileManagerController> fileManagerController;
    @Inject private Provider<ResponseFactory> responseFactory;

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

    public ConnectionDispatcher getConnectionDispatcher() {
        return connectionDispatcher.get();
    }

    public HTTPAcceptor getHTTPAcceptor() {
        return httpAcceptor.get();
    }

    public HostCatcher getHostCatcher() {
        return hostCatcher.get();
    }

    public com.limegroup.gnutella.HTTPAcceptor getHttpUploadAcceptor() {
        return httpUploadAcceptor.get();
    }

    public PushManager getPushManager() {
        return pushManager.get();
    }

    public ResponseVerifier getResponseVerifier() {
        return responseVerifier.get();
    }

    public SearchResultHandler getSearchResultHandler() {
        return searchResultHandler.get();
    }

    public AltLocManager getAltLocManager() {
        return altLocManager.get();
    }

    public ContentManager getContentManager() {
        return contentManager.get();
    }

    public IPFilter getIpFilter() {
        return ipFilter.get();
    }

    public HostileFilter getHostileFilter() {
        return hostileFilter.get();
    }

    public NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() {
        return networkUpdateSanityChecker.get();
    }

    public BandwidthManager getBandwidthManager() {
        return bandwidthManager.get();
    }

    public HttpExecutor getHttpExecutor() {
        return httpExecutor.get();
    }

    public QueryStats getQueryStats() {
        return queryStats.get();
    }

    public NodeAssigner getNodeAssigner() {
        return nodeAssigner.get();
    }

    public Statistics getStatistics() {
        return statistics.get();
    }

    public SecureMessageVerifier getSecureMessageVerifier() {
        return secureMessageVerifier.get();
    }

    public CreationTimeCache getCreationTimeCache() {
        return creationTimeCache.get();
    }

    public HttpRequestHandlerFactory getHttpRequestHandlerFactory() {
        return httpRequestHandlerFactory.get();
    }

    public UrnCache getUrnCache() {
        return urnCache.get();
    }

    public FileManagerController getFileManagerController() {
        return fileManagerController.get();
    }

    public ResponseFactory getResponseFactory() {
        return responseFactory.get();
    }

}
