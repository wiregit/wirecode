package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Guice;
import com.limegroup.bittorrent.BTContextFactory;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.BTLinkManagerFactory;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory;
import com.limegroup.bittorrent.handshaking.IncomingConnectionHandler;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.RequeryManagerFactory;
import com.limegroup.gnutella.downloader.SourceRankerFactory;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.filters.HostileFilter;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.MutableGUIDFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.licenses.LicenseCache;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.spam.RatingTable;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.tigertree.HashTreeNodeManager;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLProperties;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * A collection of Providers that are hacks during the interim change towards
 * dependency injection.
 */
// DPINJ: REMOVE THIS CLASS!!!
public class ProviderHacks {
    
    private static volatile boolean initialized = false;
    private static volatile boolean initializing = false;
    
    private static LimeWireCore aReallyLongNameThatYouDontWantToTypeALot;
    
    public static void setLimeWireCore(LimeWireCore core) {
        if(initialized || initializing)
            throw new IllegalStateException("already initialized, or initializing!");
        ProviderHacks.aReallyLongNameThatYouDontWantToTypeALot = core;
        initialized = true;
    }
    
    private static LimeWireCore i() {
        if(initialized)
            return aReallyLongNameThatYouDontWantToTypeALot;
        if(initializing)
            throw new IllegalStateException("already initializing!");
        initializing = true;
        aReallyLongNameThatYouDontWantToTypeALot = 
            Guice.createInjector(new LimeWireCoreModule(ActivityCallbackAdapter.class), new ModuleHacks()).getInstance(LimeWireCore.class);
        initializing = false;
        initialized = true;
        return aReallyLongNameThatYouDontWantToTypeALot;
    }
    
    // Still needed in core...
    public static Acceptor getAcceptor() { return i().getAcceptor(); }    
    public static UDPService getUdpService() { return i().getUdpService(); }    
    public static NetworkManager getNetworkManager() { return i().getNetworkManager(); }    
    public static ConnectionManager getConnectionManager() { return i().getConnectionManager(); }    
    public static DHTManager getDHTManager() { return i().getDhtManager(); }    
    public static PushEndpointFactory getPushEndpointFactory() { return i().getPushEndpointFactory(); } 
    public static TorrentManager getTorrentManager() { return i().getTorrentManager(); }  
    public static UploadManager getUploadManager() { return i().getUploadManager(); }
    public static FileManager getFileManager() { return i().getFileManager(); }
    public static UploadSlotManager getUploadSlotManager() { return i().getUploadSlotManager(); }
    public static QueryRequestFactory getQueryRequestFactory() { return i().getQueryRequestFactory(); }    
    public static ManagedConnectionFactory getManagedConnectionFactory() { return i().getManagedConnectionFactory(); }   
    public static SocketsManager getSocketsManager() { return i().getSocketsManager(); }  
    public static AlternateLocationFactory getAlternateLocationFactory() { return i().getAlternateLocationFactory(); }    
    public static ForMeReplyHandler getForMeReplyHandler() { return i().getForMeReplyHandler(); }
    public static QueryUnicaster getQueryUnicaster() { return i().getQueryUnicaster(); }
    public static MessageRouter getMessageRouter() { return i().getMessageRouter(); }
    public static DownloadManager getDownloadManager() { return i().getDownloadManager(); }
    public static ConnectionDispatcher getConnectionDispatcher() { return i().getConnectionDispatcher(); }
    public static HostCatcher getHostCatcher() { return i().getHostCatcher(); }
    public static AltLocManager getAltLocManager() { return i().getAltLocManager(); }
    public static ContentManager getContentManager() { return i().getContentManager(); }
    public static IPFilter getIpFilter() { return i().getIpFilter(); }
    public static HostileFilter getHostileFilter() { return i().getHostileFilter(); }
    public static NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() { return i().getNetworkUpdateSanityChecker(); }
    public static BandwidthManager getBandwidthManager() { return i().getBandwidthManager(); }
    public static HttpExecutor getHttpExecutor() { return i().getHttpExecutor(); }
    public static NodeAssigner getNodeAssigner() { return i().getNodeAssigner(); }
    public static MulticastService getMulticastService() { return i().getMulticastService(); }
    public static ChatManager getChatManager() { return i().getChatManager(); }
    public static BTLinkManagerFactory getBTLinkManagerFactory() { return i().getBTLinkManagerFactory(); }
    public static ChokerFactory getChokerFactory() { return i().getChokerFactory(); }
    public static BTConnectionFetcherFactory getBTConnectionFetcherFactory() { return i().getBTConnectionFetcherFactory(); }
    public static IncomingConnectionHandler getIncomingConnectionHandler() { return i().getIncomingConnectionHandler(); }
    public static Pinger getPinger() { return i().getPinger(); }
    public static PongCacher getPongCacher() { return i().getPongCacher(); }
    public static UPnPManager getUPnPManager() { return i().getUPnPManager(); }
    public static MutableGUIDFilter getMutableGUIDFilter() { return i().getMutableGUIDFilter(); }
    public static LicenseCache getLicenseCache() { return i().getLicenseCache(); }
    public static MessagesSupportedVendorMessage getMessagesSupportedVendorMessage() { return i().getMessagesSupportedVendorMessage(); }
    public static QueryDispatcher getQueryDispatcher() { return i().getQueryDispatcher(); }
    public static RatingTable getRatingTable() { return i().getRatingTable(); }
    public static SpamManager getSpamManager() { return i().getSpamManager(); }
    public static HashTreeNodeManager getHashTreeNodeManager()  { return i().getHashTreeNodeManager(); }
    public static TigerTreeCache getTigerTreeCache() { return i().getTigerTreeCache(); }
    public static UpdateHandler getUpdateHandler() { return i().getUpdateHandler(); }
    public static LimeXMLProperties getLimeXMLProperties() { return i().getLimeXMLProperties(); }
    public static LimeXMLSchemaRepository getLimeXMLSchemaRepository() { return i().getLimeXMLSchemaRepository(); }
    public static SchemaReplyCollectionMapper getSchemaReplyCollectionMapper() { return i().getSchemaReplyCollectionMapper(); }    
    public static SimppManager getSimppManager() { return i().getSimppManager(); }
    public static ActivityCallback getActivityCallback() { return i().getActivityCallback(); }
    public static LifecycleManager getLifecycleManager() { return i().getLifecycleManager(); }
    public static ConnectionServices getConnectionServices() { return i().getConnectionServices(); }

    // Requires some major reworking -- used statically in a lot of places
    public static ScheduledExecutorService getBackgroundExecutor() { return i().getBackgroundExecutor(); }
    
    // Requires some factories...
    public static ApplicationServices getApplicationServices() { return i().getApplicationServices(); }
    
    // Cleaned up in all but message parsers & tests
    public static QueryReplyFactory getQueryReplyFactory() { return i().getQueryReplyFactory(); }
    public static PingReplyFactory getPingReplyFactory() { return i().getPingReplyFactory(); }    
    
    // Cleaned up in all but tests
    public static DHTNodeFetcherFactory getDHTNodeFetcherFactory() { return i().getDHTNodeFetcherFactory(); }
    public static DHTBootstrapperFactory getDHTBootstrapperFactory() { return i().getDHTBootstrapperFactory(); }
    public static SearchServices getSearchServices() { return i().getSearchServices(); }
    public static DownloadServices getDownloadServices() { return i().getDownloadServices(); }
    public static ConnectionCheckerManager getConnectionCheckerManager() { return i().getConnectionCheckerManager(); }
    public static SpamServices getSpamServices() { return i().getSpamServices(); }
    public static com.limegroup.gnutella.HTTPAcceptor getHTTPUploadAcceptor() { return i().getHttpUploadAcceptor(); }
    public static SavedFileManager getSavedFileManager() { return i().getSavedFileManager(); }
    public static StaticMessages getStaticMessages() { return i().getStaticMessages(); }
    public static Statistics getStatistics() { return i().getStatistics(); }
    public static SearchResultHandler getSearchResultHandler() { return i().getSearchResultHandler(); }
    public static DHTControllerFactory getDHTControllerFactory() { return i().getDhtControllerFactory(); }
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return i().getHandshakeResponderFactory(); } 
    public static HeadersFactory getHeadersFactory() { return i().getHeadersFactory(); }    
    public static ManagedTorrentFactory getManagedTorrentFactory() { return i().getManagedTorrentFactory(); }    
    public static HTTPHeaderUtils getHTTPHeaderUtils() { return i().getHttpHeaderUtils(); }
    public static FeaturesWriter getFeaturesWriter() { return i().getFeaturesWriter(); }
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return i().getDownloadWorkerFactory(); }
    public static HeadPongFactory getHeadPongFactory() { return i().getHeadPongFactory(); }
    public static QueryHandlerFactory getQueryHandlerFactory() { return i().getQueryHandlerFactory(); }    
    public static SourceRankerFactory getSourceRankerFactory() { return i().getSourceRankerFactory(); }
    public static VerifyingFileFactory getVerifyingFileFactory() { return i().getVerifyingFileFactory(); }    
    public static DiskController getDiskController() { return i().getDiskController(); }    
    public static AltLocValueFactory getAltLocValueFactory() { return i().getAltLocValueFactory(); }    
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return i().getHttpDownloaderFactory(); }
    public static OnDemandUnicaster getOnDemandUnicaster() { return i().getOnDemandUnicaster(); }
    public static AltLocFinder getAltLocFinder() { return i().getAltLocFinder(); }
    public static SecureMessageVerifier getSecureMessageVerifier() { return i().getSecureMessageVerifier(); }
    public static MessageDispatcher getMessageDispatcher() { return i().getMessageDispatcher(); }
    public static UrnCache getUrnCache() { return i().getUrnCache(); }
    public static ResponseFactory getResponseFactory() { return i().getResponseFactory(); }
    public static HttpRequestHandlerFactory getHttpRequestHandlerFactory() { return i().getHttpRequestHandlerFactory(); }
    public static FileManagerController getFileManagerController() { return i().getFileManagerController(); }
    public static CreationTimeCache getCreationTimeCache() { return i().getCreationTimeCache(); }
    public static DownloadReferencesFactory getDownloadReferencesFactory() { return i().getDownloadReferencesFactory(); }
    public static DownloadCallback getInNetworkCallback() { return i().getInNetworkCallback(); }
    public static RequeryManagerFactory getRequeryManagerFactory() { return i().getRequeryManagerFactory(); }
    public static RUDPContext getRUDPContext() { return i().getRUDPContext(); }
    public static BTContextFactory getBTContextFactory() { return i().getBTContextFactory(); }
    public static BTDownloaderFactory getBTDownloaderFactory() { return i().getBTDownloaderFactory(); }
    public static UDPSelectorProvider getUDPSelectorProvider() { return i().getUDPSelectorProvider(); }
    public static CapabilitiesVMFactory getCapabilitiesVMFactory() { return i().getCapabilitiesVMFactory(); }
    public static ConnectionFactory getConnectionFactory() { return i().getConnectionFactory(); };
    
    // DO NOT ADD METHODS HERE -- PUT THEM IN THE RIGHT CATEGORY!

 
     
}
