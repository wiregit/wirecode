package com.limegroup.gnutella;

import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
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
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.statistics.QueryStats;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;

/**
 * Contains mostly all references to singletons within LimeWire.
 * This class should only be used if it is not possible to inject
 * the correct values into what you're using.  In most cases,
 * it should be possible to just get the injector and call
 * injector.injectMembers(myObject), which is still a superior
 * option to retrieving the individual objects from this class.
 */
@Singleton
public class LimeWireCore {
        
    private final Injector injector;
    
    @Inject
    public LimeWireCore(Injector injector) {
        this.injector = injector;
    }

    public Injector getInjector() {
        return injector;
    }

    public LocalFileDetailsFactory getLocalFileDetailsFactory() {
        return injector.getInstance(LocalFileDetailsFactory.class);
    }

    public AlternateLocationFactory getAlternateLocationFactory() {
        return injector.getInstance(AlternateLocationFactory.class);
    }

    public AltLocValueFactory getAltLocValueFactory() {
        return injector.getInstance(AltLocValueFactory.class);
    }

    public DiskController getDiskController() {
        return injector.getInstance(DiskController.class);
    }

    public VerifyingFileFactory getVerifyingFileFactory() {
        return injector.getInstance(VerifyingFileFactory.class);
    }

    public SocketsManager getSocketsManager() {
        return injector.getInstance(SocketsManager.class);
    }

    public SourceRankerFactory getSourceRankerFactory() {
        return injector.getInstance(SourceRankerFactory.class);
    }

    public HostDataFactory getHostDataFactory() {
        return injector.getInstance(HostDataFactory.class);
    }

    public ManagedConnectionFactory getManagedConnectionFactory() {
        return injector.getInstance(ManagedConnectionFactory.class);
    }

    public QueryRequestFactory getQueryRequestFactory() {
        return injector.getInstance(QueryRequestFactory.class);
    }

    public QueryHandlerFactory getQueryHandlerFactory() {
        return injector.getInstance(QueryHandlerFactory.class);
    }

    public UploadSlotManager getUploadSlotManager() {
        return injector.getInstance(UploadSlotManager.class);
    }

    public FileManager getFileManager() {
        return injector.getInstance(FileManager.class);
    }

    public UploadManager getUploadManager() {
        return injector.getInstance(UploadManager.class);
    }

    public HeadPongFactory getHeadPongFactory() {
        return injector.getInstance(HeadPongFactory.class);
    }

    public HTTPDownloaderFactory getHttpDownloaderFactory() {
        return injector.getInstance(HTTPDownloaderFactory.class);
    }

    public DownloadWorkerFactory getDownloadWorkerFactory() {
        return injector.getInstance(DownloadWorkerFactory.class);
    }

    public FeaturesWriter getFeaturesWriter() {
        return injector.getInstance(FeaturesWriter.class);
    }

    public HTTPHeaderUtils getHttpHeaderUtils() {
        return injector.getInstance(HTTPHeaderUtils.class);
    }

    public TrackerFactory getTrackerFactory() {
        return injector.getInstance(TrackerFactory.class);
    }

    public TrackerManagerFactory getTrackerManagerFactory() {
        return injector.getInstance(TrackerManagerFactory.class);
    }

    public TorrentManager getTorrentManager() {
        return injector.getInstance(TorrentManager.class);
    }

    public ManagedTorrentFactory getManagedTorrentFactory() {
        return injector.getInstance(ManagedTorrentFactory.class);
    }

    public PushEndpointFactory getPushEndpointFactory() {
        return injector.getInstance(PushEndpointFactory.class);
    }

    public HeadersFactory getHeadersFactory() {
        return injector.getInstance(HeadersFactory.class);
    }

    public HandshakeResponderFactory getHandshakeResponderFactory() {
        return injector.getInstance(HandshakeResponderFactory.class);
    }

    public PushProxiesValueFactory getPushProxiesValueFactory() {
        return injector.getInstance(PushProxiesValueFactory.class);
    }

    public PingReplyFactory getPingReplyFactory() {
        return injector.getInstance(PingReplyFactory.class);
    }

    public DHTControllerFactory getDhtControllerFactory() {
        return injector.getInstance(DHTControllerFactory.class);
    }

    public DHTManager getDhtManager() {
        return injector.getInstance(DHTManager.class);
    }

    public ConnectionManager getConnectionManager() {
        return injector.getInstance(ConnectionManager.class);
    }

    public NetworkManager getNetworkManager() {
        return injector.getInstance(NetworkManager.class);
    }

    public UDPService getUdpService() {
        return injector.getInstance(UDPService.class);
    }

    public Acceptor getAcceptor() {
        return injector.getInstance(Acceptor.class);
    }
    
    public ForMeReplyHandler getForMeReplyHandler() {
        return injector.getInstance(ForMeReplyHandler.class);
    }
    
    public QueryUnicaster getQueryUnicaster() {
        return injector.getInstance(QueryUnicaster.class);
    }
    
    public OnDemandUnicaster getOnDemandUnicaster() {
        return injector.getInstance(OnDemandUnicaster.class);
    }

    public MessageRouter getMessageRouter() {
        return injector.getInstance(MessageRouter.class);
    }

    public DownloadManager getDownloadManager() {
        return injector.getInstance(DownloadManager.class);
    }

    public AltLocFinder getAltLocFinder() {
        return injector.getInstance(AltLocFinder.class);
    }

    public ConnectionDispatcher getConnectionDispatcher() {
        return injector.getInstance(ConnectionDispatcher.class);
    }

    public HTTPAcceptor getHTTPAcceptor() {
        return injector.getInstance(HTTPAcceptor.class);
    }

    public HostCatcher getHostCatcher() {
        return injector.getInstance(HostCatcher.class);
    }

    public com.limegroup.gnutella.HTTPAcceptor getHttpUploadAcceptor() {
        return injector.getInstance(com.limegroup.gnutella.HTTPAcceptor.class);
    }

    public PushManager getPushManager() {
        return injector.getInstance(PushManager.class);
    }

    public ResponseVerifier getResponseVerifier() {
        return injector.getInstance(ResponseVerifier.class);
    }

    public SearchResultHandler getSearchResultHandler() {
        return injector.getInstance(SearchResultHandler.class);
    }

    public AltLocManager getAltLocManager() {
        return injector.getInstance(AltLocManager.class);
    }

    public ContentManager getContentManager() {
        return injector.getInstance(ContentManager.class);
    }

    public IPFilter getIpFilter() {
        return injector.getInstance(IPFilter.class);
    }

    public HostileFilter getHostileFilter() {
        return injector.getInstance(HostileFilter.class);
    }

    public NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() {
        return injector.getInstance(NetworkUpdateSanityChecker.class);
    }

    public BandwidthManager getBandwidthManager() {
        return injector.getInstance(BandwidthManager.class);
    }

    public HttpExecutor getHttpExecutor() {
        return injector.getInstance(HttpExecutor.class);
    }

    public QueryStats getQueryStats() {
        return injector.getInstance(QueryStats.class);
    }

    public NodeAssigner getNodeAssigner() {
        return injector.getInstance(NodeAssigner.class);
    }

    public Statistics getStatistics() {
        return injector.getInstance(Statistics.class);
    }

    public SecureMessageVerifier getSecureMessageVerifier() {
        return injector.getInstance(SecureMessageVerifier.class);
    }

    public CreationTimeCache getCreationTimeCache() {
        return injector.getInstance(CreationTimeCache.class);
    }

    public HttpRequestHandlerFactory getHttpRequestHandlerFactory() {
        return injector.getInstance(HttpRequestHandlerFactory.class); 
    }

    public UrnCache getUrnCache() {
        return injector.getInstance(UrnCache.class);
    }

    public FileManagerController getFileManagerController() {
        return injector.getInstance(FileManagerController.class);
    }

    public ResponseFactory getResponseFactory() {
        return injector.getInstance(ResponseFactory.class);
    }

    public QueryReplyFactory getQueryReplyFactory() {
        return injector.getInstance(QueryReplyFactory.class);
    }

    public StaticMessages getStaticMessages() {
        return injector.getInstance(StaticMessages.class);
    }

}
