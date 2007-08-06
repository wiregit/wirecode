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
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.RequeryManagerFactory;
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
            Guice.createInjector(new LimeWireCoreModule(), new ModuleHacks()).getInstance(LimeWireCore.class);
        initializing = false;
        initialized = true;
        return aReallyLongNameThatYouDontWantToTypeALot;
    }
    
    public static Acceptor getAcceptor() { return i().getAcceptor(); }    
    public static UDPService getUdpService() { return i().getUdpService(); }    
    public static NetworkManager getNetworkManager() { return i().getNetworkManager(); }    
    public static ConnectionManager getConnectionManager() { return i().getConnectionManager(); }    
    public static DHTManager getDHTManager() { return i().getDhtManager(); }    
    public static DHTControllerFactory getDHTControllerFactory() { return i().getDhtControllerFactory(); }    
    public static PingReplyFactory getPingReplyFactory() { return i().getPingReplyFactory(); }    
    public static PushProxiesValueFactory getPushProxiesValueFactory() { return i().getPushProxiesValueFactory(); }    
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return i().getHandshakeResponderFactory(); }   
    public static HeadersFactory getHeadersFactory() { return i().getHeadersFactory(); }    
    public static PushEndpointFactory getPushEndpointFactory() { return i().getPushEndpointFactory(); } 
    public static ManagedTorrentFactory getManagedTorrentFactory() { return i().getManagedTorrentFactory(); }    
    public static TorrentManager getTorrentManager() { return i().getTorrentManager(); }    
    public static TrackerManagerFactory getTrackerManagerFactory() { return i().getTrackerManagerFactory(); }
    public static TrackerFactory getTrackerFactory() { return i().getTrackerFactory(); }    
    public static HTTPHeaderUtils getHTTPHeaderUtils() { return i().getHttpHeaderUtils(); }    
    public static FeaturesWriter getFeaturesWriter() { return i().getFeaturesWriter(); }
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return i().getDownloadWorkerFactory(); } 
    public static HeadPongFactory getHeadPongFactory() { return i().getHeadPongFactory(); } 
    public static UploadManager getUploadManager() { return i().getUploadManager(); }
    public static FileManager getFileManager() { return i().getFileManager(); }
    public static UploadSlotManager getUploadSlotManager() { return i().getUploadSlotManager(); }
    public static QueryHandlerFactory getQueryHandlerFactory() { return i().getQueryHandlerFactory(); }    
    public static QueryRequestFactory getQueryRequestFactory() { return i().getQueryRequestFactory(); }    
    public static ManagedConnectionFactory getManagedConnectionFactory() { return i().getManagedConnectionFactory(); }       
    public static HostDataFactory getHostDataFactory() { return i().getHostDataFactory(); }
    public static SourceRankerFactory getSourceRankerFactory() { return i().getSourceRankerFactory(); }
    public static SocketsManager getSocketsManager() { return i().getSocketsManager(); }    
    public static VerifyingFileFactory getVerifyingFileFactory() { return i().getVerifyingFileFactory(); }    
    public static DiskController getDiskController() { return i().getDiskController(); }    
    public static AltLocValueFactory getAltLocValueFactory() { return i().getAltLocValueFactory(); }    
    public static AlternateLocationFactory getAlternateLocationFactory() { return i().getAlternateLocationFactory(); }      
    public static LocalFileDetailsFactory getLocalFileDetailsFactory() { return i().getLocalFileDetailsFactory(); }
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return i().getHttpDownloaderFactory(); }
    public static ForMeReplyHandler getForMeReplyHandler() { return i().getForMeReplyHandler(); }
    public static QueryUnicaster getQueryUnicaster() { return i().getQueryUnicaster(); }
    public static OnDemandUnicaster getOnDemandUnicaster() { return i().getOnDemandUnicaster(); }
    public static MessageRouter getMessageRouter() { return i().getMessageRouter(); } // DPINJ: Figure out what's going on with RS.getMessageRouter
    public static DownloadManager getDownloadManager() { return i().getDownloadManager(); }
    public static AltLocFinder getAltLocFinder() { return i().getAltLocFinder(); }
    public static ConnectionDispatcher getConnectionDispatcher() { return i().getConnectionDispatcher(); }
    public static HTTPAcceptor getHTTPAcceptor() { return i().getHTTPAcceptor(); }
    public static HostCatcher getHostCatcher() { return i().getHostCatcher(); }
    public static com.limegroup.gnutella.HTTPAcceptor getHTTPUploadAcceptor() { return i().getHttpUploadAcceptor(); }
    public static PushManager getPushManager() { return i().getPushManager(); }
    public static ResponseVerifier getResponseVerifier() { return i().getResponseVerifier(); }
    public static SearchResultHandler getSearchResultHandler() { return i().getSearchResultHandler(); }
    public static AltLocManager getAltLocManager() { return i().getAltLocManager(); }
    public static ContentManager getContentManager() { return i().getContentManager(); }
    public static IPFilter getIpFilter() { return i().getIpFilter(); }
    public static HostileFilter getHostileFilter() { return i().getHostileFilter(); }
    public static NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() { return i().getNetworkUpdateSanityChecker(); }
    public static BandwidthManager getBandwidthManager() { return i().getBandwidthManager(); }
    public static HttpExecutor getHttpExecutor() { return i().getHttpExecutor(); }
    public static QueryStats getQueryStats() { return i().getQueryStats(); }
    public static NodeAssigner getNodeAssigner() { return i().getNodeAssigner(); }
    public static SecureMessageVerifier getSecureMessageVerifier() { return i().getSecureMessageVerifier(); }

      // Cleaned up in all but message parsers & tests
    public static QueryReplyFactory getQueryReplyFactory() { return i().getQueryReplyFactory(); }    
    
    // Cleaned up in all but RS & tests
    public static SavedFileManager getSavedFileManager() { return i().getSavedFileManager(); }
    public static Statistics getStatistics() { return i().getStatistics(); }
    public static StaticMessages getStaticMessages() { return i().getStaticMessages(); }
    
    // Cleaned up in all but tests
    public static UrnCache getUrnCache() { return i().getUrnCache(); }
    public static ResponseFactory getResponseFactory() { return i().getResponseFactory(); }
    public static HttpRequestHandlerFactory getHttpRequestHandlerFactory() { return i().getHttpRequestHandlerFactory(); }
    public static FileManagerController getFileManagerController() { return i().getFileManagerController(); }
    public static CreationTimeCache getCreationTimeCache() { return i().getCreationTimeCache(); }
    public static DownloadReferencesFactory getDownloadReferencesFactory() { return i().getDownloadReferencesFactory(); }
    public static DownloadCallback getInNetworkCallback() { return i().getInNetworkCallback(); }
    public static RequeryManagerFactory getRequeryManagerFactory() { return i().getRequeryManagerFactory(); }

     
}
