package com.limegroup.gnutella;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.net.ConnectionDispatcher;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Guice;
import com.google.inject.Provider;
import com.limegroup.bittorrent.BTContextFactory;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactory;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactory;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.chat.InstantMessengerFactory;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.RequeryManagerFactory;
import com.limegroup.gnutella.downloader.SourceRankerFactory;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.MutableGUIDFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.licenses.LicenseCache;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.messagehandlers.AdvancedToggleHandler;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.uploader.FileResponseEntityFactory;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.version.UpdateCollectionFactory;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLProperties;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactory;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * A collection of Providers that are hacks during the interim change towards
 * dependency injection.
 */
// DPINJ: REMOVE THIS CLASS!!!
public class ProviderHacks {
    
    private static volatile boolean unusable = false;
    private static volatile boolean used = false;
    
    private static volatile boolean initialized = false;
    private static volatile boolean initializing = false;
    
    private static volatile Throwable initializedSource;
    
    private static LimeWireCore aReallyLongNameThatYouDontWantToTypeALot;
    
    public static void markUnusable() {
        if(used)
            throw new IllegalStateException("already init'd/used", initializedSource);
        unusable = true;
        initializedSource = new Exception();
    }
    
    private static LimeWireCore use() {
        if(unusable)
            throw new IllegalStateException("marked unusable", initializedSource);        
        used = true;
        
        if(initialized)
            return aReallyLongNameThatYouDontWantToTypeALot;        
        if(initializing)
            throw new IllegalStateException("already initializing!");
        initializing = true;
        aReallyLongNameThatYouDontWantToTypeALot = Guice.createInjector(new LimeWireCoreModule(ActivityCallbackAdapter.class), new ModuleHacks()).getInstance(LimeWireCore.class);
        initializedSource = new Exception();
        initializing = false;
        return aReallyLongNameThatYouDontWantToTypeALot;
    }
    
    public static ScheduledExecutorService getBackgroundExecutor() { return use().getBackgroundExecutor(); }
    public static UPnPManager getUPnPManager() { return use().getUPnPManager(); }
    public static PongCacher getPongCacher() { return use().getPongCacher(); }
    public static Pinger getPinger() { return use().getPinger(); }
    public static ChatManager getChatManager() { return use().getChatManager(); }
    public static MulticastService getMulticastService() { return use().getMulticastService(); }
    public static NodeAssigner getNodeAssigner() { return use().getNodeAssigner(); }
    public static NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() { return use().getNetworkUpdateSanityChecker(); }
    public static HostCatcher getHostCatcher() { return use().getHostCatcher(); }
    public static ConnectionDispatcher getConnectionDispatcher() { return use().getConnectionDispatcher(); }
    public static QueryUnicaster getQueryUnicaster() { return use().getQueryUnicaster(); }
    public static ManagedConnectionFactory getManagedConnectionFactory() { return use().getManagedConnectionFactory(); }
    public static UploadManager getUploadManager() { return use().getUploadManager(); }

    public static UploadSlotManager getUploadSlotManager() { return use().getUploadSlotManager(); }
    public static DHTNodeFetcherFactory getDHTNodeFetcherFactory() { return use().getDHTNodeFetcherFactory(); }
    public static DHTBootstrapperFactory getDHTBootstrapperFactory() { return use().getDHTBootstrapperFactory(); }
    public static SearchServices getSearchServices() { return use().getSearchServices(); }
    public static DownloadServices getDownloadServices() { return use().getDownloadServices(); }
    public static ConnectionCheckerManager getConnectionCheckerManager() { return use().getConnectionCheckerManager(); }
    public static SpamServices getSpamServices() { return use().getSpamServices(); }
    public static HTTPAcceptor getHTTPUploadAcceptor() { return use().getHttpUploadAcceptor(); }
    public static SavedFileManager getSavedFileManager() { return use().getSavedFileManager(); }
    public static StaticMessages getStaticMessages() { return use().getStaticMessages(); }
    public static Statistics getStatistics() { return use().getStatistics(); }
    public static SearchResultHandler getSearchResultHandler() { return use().getSearchResultHandler(); }
    public static DHTControllerFactory getDHTControllerFactory() { return use().getDhtControllerFactory(); }
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return use().getHandshakeResponderFactory(); } 
    public static HeadersFactory getHeadersFactory() { return use().getHeadersFactory(); }    
    public static ManagedTorrentFactory getManagedTorrentFactory() { return use().getManagedTorrentFactory(); }    
    public static HTTPHeaderUtils getHTTPHeaderUtils() { return use().getHttpHeaderUtils(); }
    public static FeaturesWriter getFeaturesWriter() { return use().getFeaturesWriter(); }
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return use().getDownloadWorkerFactory(); }
    public static HeadPongFactory getHeadPongFactory() { return use().getHeadPongFactory(); }
    public static QueryHandlerFactory getQueryHandlerFactory() { return use().getQueryHandlerFactory(); }    
    public static SourceRankerFactory getSourceRankerFactory() { return use().getSourceRankerFactory(); }
    public static VerifyingFileFactory getVerifyingFileFactory() { return use().getVerifyingFileFactory(); }    
    public static DiskController getDiskController() { return use().getDiskController(); }    
    public static AltLocValueFactory getAltLocValueFactory() { return use().getAltLocValueFactory(); }    
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return use().getHttpDownloaderFactory(); }
    public static OnDemandUnicaster getOnDemandUnicaster() { return use().getOnDemandUnicaster(); }
    public static AltLocFinder getAltLocFinder() { return use().getAltLocFinder(); }
    public static SecureMessageVerifier getSecureMessageVerifier() { return use().getSecureMessageVerifier(); }
    public static MessageDispatcher getMessageDispatcher() { return use().getMessageDispatcher(); }
    public static UrnCache getUrnCache() { return use().getUrnCache(); }
    public static ResponseFactory getResponseFactory() { return use().getResponseFactory(); }
    public static HttpRequestHandlerFactory getHttpRequestHandlerFactory() { return use().getHttpRequestHandlerFactory(); }
    public static FileManagerController getFileManagerController() { return use().getFileManagerController(); }
    public static CreationTimeCache getCreationTimeCache() { return use().getCreationTimeCache(); }
    public static DownloadReferencesFactory getDownloadReferencesFactory() { return use().getDownloadReferencesFactory(); }
    public static DownloadCallback getInNetworkCallback() { return use().getInNetworkCallback(); }
    public static RequeryManagerFactory getRequeryManagerFactory() { return use().getRequeryManagerFactory(); }
    public static RUDPContext getRUDPContext() { return use().getRUDPContext(); }
    public static BTContextFactory getBTContextFactory() { return use().getBTContextFactory(); }
    public static BTDownloaderFactory getBTDownloaderFactory() { return use().getBTDownloaderFactory(); }
    public static UDPSelectorProvider getUDPSelectorProvider() { return use().getUDPSelectorProvider(); }
    public static CapabilitiesVMFactory getCapabilitiesVMFactory() { return use().getCapabilitiesVMFactory(); }
    public static ConnectionFactory getConnectionFactory() { return use().getConnectionFactory(); };
    public static LifecycleManager getLifecycleManager() { return use().getLifecycleManager(); }
    public static SimppManager getSimppManager() { return use().getSimppManager(); }
    public static QueryDispatcher getQueryDispatcher() { return use().getQueryDispatcher(); }
    public static SpamManager getSpamManager() { return use().getSpamManager(); }
    public static MessagesSupportedVendorMessage getMessagesSupportedVendorMessage() { return use().getMessagesSupportedVendorMessage(); }
    public static DownloadCallback getDownloadCallback() { return use().getDownloadCallback(); }
    public static PushDownloadManager getPushDownloadManager() { return use().getPushDownloadManager(); }
    public static UniqueHostPinger getUniqueHostPinger() { return use().getUniqueHostPinger(); }
    public static UDPPinger getUDPPinger() { return use().getUDPPinger(); }
    public static ScheduledExecutorService getNIOExecutor() { return use().getNIOExecutor(); }
    public static GuidMapManager getGuidMapManager() { return use().getGuidMapManager(); }
    public static BrowseHostHandlerManager getBrowseHostHandlerManager() { return use().getBrowseHostHandlerManager(); }
    public static PushEndpointCache getPushEndpointCache() { return use().getPushEndpointCache(); }
    public static SpamFilterFactory getSpamFilterFactory() { return use().getSpamFilterFactory(); };
    public static UDPReplyHandlerFactory getUDPReplyHandlerFactory() { return use().getUDPReplyHandlerFactory(); }
    public static UDPReplyHandlerCache getUDPReplyHandlerCache() { return use().getUDPReplyHandlerCache(); }
    public static Provider<InspectionRequestHandler> getInspectionRequestHandlerFactory() { return use().getInspectionRequestHandlerFactory(); }
    public static Provider<UDPCrawlerPingHandler> getUDPCrawlerPingHandlerFactory() { return use().getUDPCrawlerPingHandlerFactory(); }
    public static Provider<AdvancedToggleHandler> getAdvancedToggleHandlerFactory() { return use().getAdvancedToggleHandlerFactory(); }
    public static FileResponseEntityFactory getFileResponseEntityFactory() { return use().getFileRepsoneEntityFactory(); }
    public static BandwidthManager getBandwidthManager() { return use().getBandwidthManager(); }
    public static MutableGUIDFilter getMutableGUIDFilter() { return use().getMutableGUIDFilter(); }
    public static IPFilter getIpFilter() { return use().getIpFilter(); }
    public static MessageFactory getMessageFactory() { return use().getMessageFactory(); }
    public static MessageReaderFactory getMessageReaderFactory() { return use().getMessageReaderFactory(); }
    public static PingReplyFactory getPingReplyFactory() { return use().getPingReplyFactory(); }
    public static QueryReplyFactory getQueryReplyFactory() { return use().getQueryReplyFactory(); }
    public static VendorMessageFactory getVendorMessageFactory() { return use().getVendorMessageFactory(); }
    public static ReplyNumberVendorMessageFactory getReplyNumberVendorMessageFactory() { return use().getReplyNumberVendorMessageFactory(); }
    public static ForMeReplyHandler getForMeReplyHandler() { return use().getForMeReplyHandler(); }
    public static UDPCrawlerPongFactory getUDPCrawlerPongFactory() { return use().getUDPCrawlerPongFactory(); }
    public static DHTManager getDHTManager() { return use().getDhtManager(); }    
    public static AltLocManager getAltLocManager() { return use().getAltLocManager(); }
    public static AlternateLocationFactory getAlternateLocationFactory() { return use().getAlternateLocationFactory(); }
    public static UDPHostCacheFactory getUDPHostCacheFactory() { return use().getUDPHostCacheFactory(); }
    public static MessageRouter getMessageRouter() { return use().getMessageRouter(); }
    public static UpdateHandler getUpdateHandler() { return use().getUpdateHandler(); }
    public static TigerTreeCache getTigerTreeCache() { return use().getTigerTreeCache(); }
    public static PushEndpointFactory getPushEndpointFactory() { return use().getPushEndpointFactory(); }
    public static ConnectionManager getConnectionManager() { return use().getConnectionManager(); }
    public static LimeXMLReplyCollectionFactory getLimeXMLReplyCollectionFactory() { return use().getLimeXMLReplyCollectionFactory(); }
    public static FileManager getFileManager() { return use().getFileManager(); }
    public static LicenseFactory getLicenseFactory() { return use().getLicenseFactory(); }
    public static LimeXMLDocumentFactory getLimeXMLDocumentFactory() { return use().getLimeXMLDocumentFactory(); }
    public static LimeXMLDocumentHelper getLimeXMLDocumentHelper()  { return use().getLimeXMLDocumentHelper(); }
    public static MetaDataReader getMetaDataReader() { return use().getMetaDataReader(); }
    public static LicenseCache getLicenseCache() { return use().getLicenseCache(); }
    public static LimeXMLProperties getLimeXMLProperties() { return use().getLimeXMLProperties(); }
    public static InstantMessengerFactory getInstantMessengerFactory() { return use().getInstantMessengerFactory(); };
    public static SocketsManager getSocketsManager() { return use().getSocketsManager(); }
    public static QueryRequestFactory getQueryRequestFactory() { return use().getQueryRequestFactory(); }
    public static SchemaReplyCollectionMapper getSchemaReplyCollectionMapper() { return use().getSchemaReplyCollectionMapper(); }
    public static LimeXMLSchemaRepository getLimeXMLSchemaRepository() { return use().getLimeXMLSchemaRepository(); }
    public static Acceptor getAcceptor() { return use().getAcceptor(); }    
    public static GnutellaDownloaderFactory getGnutellaDownloaderFactory() { return use().getGnutellaDownloaderFactory(); }
    public static DownloadManager getDownloadManager() { return use().getDownloadManager(); }
    public static ContentManager getContentManager() { return use().getContentManager(); }
    public static BTUploaderFactory getBTUploaderFactory() { return use().getBTUploaderFactory(); }
    public static TorrentManager getTorrentManager() { return use().getTorrentManager(); }      
    public static ActivityCallback getActivityCallback() { return use().getActivityCallback(); }
    public static PingRequestFactory getPingRequestFactory() { return use().getPingRequestFactory(); }
    public static UDPService getUdpService() { return use().getUdpService(); }     
    public static IpPortContentAuthorityFactory getIpPortContentAuthorityFactory() { return use().getIpPortContentAuthorityFactory(); }
    public static UpdateCollectionFactory getUpdateCollectionFactory() { return use().getUpdateCollectionFactory(); }
    public static ApplicationServices getApplicationServices() { return use().getApplicationServices(); }
    public static NetworkManager getNetworkManager() { return use().getNetworkManager(); }
    public static ConnectionServices getConnectionServices() { return use().getConnectionServices(); }
    
    
}
