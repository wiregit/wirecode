package com.limegroup.gnutella;

import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Guice;
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
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.SocketsManager;

/**
 * A collection of Providers that are hacks during the interim change towards
 * dependency injection.
 */
// DPINJ: REMOVE THIS CLASS!!!
public class ProviderHacks {
    
    private static volatile boolean initialized = false;
    private static volatile boolean initializing = false;
    
    private static LimeWireCore core;
    
    public static void setLimeWireCore(LimeWireCore core) {
        if(initialized || initializing)
            throw new IllegalStateException("already initialized, or initializing!");
        ProviderHacks.core = core;
        initialized = true;
    }
    
    private static void i() {
        if(initialized)
            return;
        if(initializing)
            throw new IllegalStateException("already initializing!");
        initializing = true;
        core = Guice.createInjector(new LimeWireCoreModule(), new ModuleHacks()).getInstance(LimeWireCore.class);
        initializing = false;
        initialized = true;
    }
    
    public static Acceptor getAcceptor() { i(); return core.getAcceptor(); }    
    public static UDPService getUdpService() { i(); return core.getUdpService(); }    
    public static NetworkManager getNetworkManager() { i(); return core.getNetworkManager(); }    
    public static ConnectionManager getConnectionManager() { i(); return core.getConnectionManager(); }    
    public static DHTManager getDHTManager() { i(); return core.getDhtManager(); }    
    public static DHTControllerFactory getDHTControllerFactory() { i(); return core.getDhtControllerFactory(); }    
    public static PingReplyFactory getPingReplyFactory() { i(); return core.getPingReplyFactory(); }    
    public static PushProxiesValueFactory getPushProxiesValueFactory() { i(); return core.getPushProxiesValueFactory(); }    
    public static HandshakeResponderFactory getHandshakeResponderFactory() { i(); return core.getHandshakeResponderFactory(); }   
    public static HeadersFactory getHeadersFactory() { i(); return core.getHeadersFactory(); }    
    public static PushEndpointFactory getPushEndpointFactory() { i(); return core.getPushEndpointFactory(); } 
    public static ManagedTorrentFactory getManagedTorrentFactory() { i(); return core.getManagedTorrentFactory(); }    
    public static TorrentManager getTorrentManager() { i(); return core.getTorrentManager(); }    
    public static TrackerManagerFactory getTrackerManagerFactory() { i(); return core.getTrackerManagerFactory(); }
    public static TrackerFactory getTrackerFactory() { i();  return core.getTrackerFactory(); }    
    public static HTTPHeaderUtils getHTTPHeaderUtils() { i(); return core.getHttpHeaderUtils(); }    
    public static FeaturesWriter getFeaturesWriter() { return core.getFeaturesWriter(); }
    public static DownloadWorkerFactory getDownloadWorkerFactory() {i();  return core.getDownloadWorkerFactory(); } 
    public static HeadPongFactory getHeadPongFactory() {i(); return core.getHeadPongFactory(); } 
    public static UploadManager getUploadManager() {i();  return core.getUploadManager(); }
    public static FileManager getFileManager() {i();  return core.getFileManager(); }
    public static UploadSlotManager getUploadSlotManager() {i();  return core.getUploadSlotManager(); }
    public static QueryHandlerFactory getQueryHandlerFactory() {i();  return core.getQueryHandlerFactory(); }    
    public static QueryRequestFactory getQueryRequestFactory() {i();  return core.getQueryRequestFactory(); }    
    public static ManagedConnectionFactory getManagedConnectionFactory() {i();  return core.getManagedConnectionFactory(); }       
    public static HostDataFactory getHostDataFactory() {i();  return core.getHostDataFactory(); }
    public static SourceRankerFactory getSourceRankerFactory() {i();  return core.getSourceRankerFactory(); }
    public static SocketsManager getSocketsManager() {i();  return core.getSocketsManager(); }    
    public static VerifyingFileFactory getVerifyingFileFactory() {i();  return core.getVerifyingFileFactory(); }    
    public static DiskController getDiskController() {i();  return core.getDiskController(); }    
    public static AltLocValueFactory getAltLocValueFactory() {i();  return core.getAltLocValueFactory(); }    
    public static AlternateLocationFactory getAlternateLocationFactory() {i();  return core.getAlternateLocationFactory(); }      
    public static LocalFileDetailsFactory getLocalFileDetailsFactory() {i();  return core.getLocalFileDetailsFactory(); }
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() {i();  return core.getHttpDownloaderFactory(); }
    public static ForMeReplyHandler getForMeReplyHandler() {i();  return core.getForMeReplyHandler(); }
    public static QueryUnicaster getQueryUnicaster() {i();  return core.getQueryUnicaster(); }
    public static OnDemandUnicaster getOnDemandUnicaster() {i();  return core.getOnDemandUnicaster(); }
    public static MessageRouter getMessageRouter() {i();  return core.getMessageRouter(); } // DPINJ: Figure out what's going on with RS.getMessageRouter
    public static DownloadManager getDownloadManager() {i();  return core.getDownloadManager(); }
    public static AltLocFinder getAltLocFinder() { i(); return core.getAltLocFinder(); }
    public static ConnectionDispatcher getConnectionDispatcher() { i(); return core.getConnectionDispatcher(); }
    public static HTTPAcceptor getHTTPAcceptor() { i(); return core.getHTTPAcceptor(); }
    public static HostCatcher getHostCatcher() { i(); return core.getHostCatcher(); }
    public static com.limegroup.gnutella.HTTPAcceptor getHTTPUploadAcceptor() { i(); return core.getHttpUploadAcceptor(); }
    public static PushManager getPushManager() { i(); return core.getPushManager(); }
    public static ResponseVerifier getResponseVerifier() { i(); return core.getResponseVerifier(); }
    public static SearchResultHandler getSearchResultHandler() { i(); return core.getSearchResultHandler(); }
    public static AltLocManager getAltLocManager() { i(); return core.getAltLocManager(); }
    public static ContentManager getContentManager() { i(); return core.getContentManager(); }
    public static IPFilter getIpFilter() { i(); return core.getIpFilter(); }
    public static HostileFilter getHostileFilter() { i(); return core.getHostileFilter(); }
    public static NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() { i(); return core.getNetworkUpdateSanityChecker(); }
    public static BandwidthManager getBandwidthManager() { i(); return core.getBandwidthManager(); }
    public static HttpExecutor getHttpExecutor() { i(); return core.getHttpExecutor(); }
    public static QueryStats getQueryStats() { i(); return core.getQueryStats(); }
    public static NodeAssigner getNodeAssigner() { i(); return core.getNodeAssigner(); }
    public static SecureMessageVerifier getSecureMessageVerifier() { i(); return core.getSecureMessageVerifier(); }
    public static CreationTimeCache getCreationTimeCache() { i(); return core.getCreationTimeCache(); }
    
    
    // Cleaned up in all but RS & tests
    public static Statistics getStatistics() { i(); return core.getStatistics(); }
    
}
