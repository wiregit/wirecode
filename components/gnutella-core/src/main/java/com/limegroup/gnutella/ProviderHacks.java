package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTManager;
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

/**
 * A collection of Providers that are hacks during the interim change towards
 * dependency injection.
 */
// DPINJ: REMOVE THIS CLASS!!!
public class ProviderHacks {
    

    public static final Provider<ActivityCallback> activityCallback = new Provider<ActivityCallback>() {
        public ActivityCallback get() {
            return RouterService.getCallback();
        }
    };
    
    public static final Provider<NIODispatcher> nioDispatcher = new Provider<NIODispatcher>() {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };
    
    public static final Provider<DownloadManager> downloadManager = new Provider<DownloadManager>() {
        public DownloadManager get() {
            return RouterService.getDownloadManager();
        }
    };
    
    public static final Provider<Statistics> statistics = new Provider<Statistics>() {
        public Statistics get() {
            return Statistics.instance();
        }
    };
    
    public static final Provider<ContentManager> contentManager = new Provider<ContentManager>() {
        public ContentManager get() {
            return RouterService.getContentManager();
        }
    };
    
      
    public static final Provider<CreationTimeCache> creationTimeCache = new Provider<CreationTimeCache>() {
       public CreationTimeCache get() {
           return CreationTimeCache.instance();
       }
    };

    public static final Provider<ByteBufferCache> byteBufferCache = new Provider<ByteBufferCache>() {
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    public static final Provider<ScheduledExecutorService> nioScheduledExecutorService = new Provider<ScheduledExecutorService>() {
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };
    
    //-------------------------------------------
    // hack providers that construct new things
    
    public static final Provider<QueryUnicaster> queryUnicaster = new AbstractLazySingletonProvider<QueryUnicaster>() {
        @Override
        public QueryUnicaster createObject() {
            return new QueryUnicaster(getNetworkManager(), getQueryRequestFactory());
        }
    };
    
    public static final Provider<OnDemandUnicaster> onDemandUnicaster = new AbstractLazySingletonProvider<OnDemandUnicaster>() {
        protected OnDemandUnicaster createObject() {
            return new OnDemandUnicaster(getQueryRequestFactory());
        }
    };
    
    public static final Provider<ForMeReplyHandler> forMeReplyHandler = new AbstractLazySingletonProvider<ForMeReplyHandler>() {
        protected ForMeReplyHandler createObject() {
            return new ForMeReplyHandler(getNetworkManager());
        }
    };
    public static ForMeReplyHandler getForMeReplyHandler() { return forMeReplyHandler.get(); }
    
    
    //-------------------------------------------
    // hack that always constructs new things -- hard to guicify...
    public static final Provider<StandardMessageRouter> newStandardMessageRouter = new Provider<StandardMessageRouter>() {
      public StandardMessageRouter get() {
          return new StandardMessageRouter(getNetworkManager(), getQueryRequestFactory(), getQueryHandlerFactory(), onDemandUnicaster, getHeadPongFactory(), getPingReplyFactory());
        }  
    };
    public static final StandardMessageRouter getNewStandardMessageRouter() { return newStandardMessageRouter.get(); }
    
    
    //-------------------------------------------
    // hack providers that construct new things, and getters for getting them easily.

    static {
        new LimeWireCore();
    }


    @Inject public static Provider<Acceptor> acceptor;
    public static Acceptor getAcceptor() { return acceptor.get(); }
    
    @Inject public static Provider<UDPService> udpService;
    public static UDPService getUdpService() { return udpService.get(); }
    
    @Inject public static Provider<NetworkManager> networkManager;
    public static final NetworkManager getNetworkManager() { return networkManager.get(); }
    
    @Inject public static Provider<ConnectionManager> connectionManager;
    public static ConnectionManager getConnectionManager() { return connectionManager.get(); }
    
    @Inject public static Provider<DHTManager> dhtManager;
    public static DHTManager getDHTManager() { return dhtManager.get(); }
    
    @Inject public static Provider<DHTControllerFactory> dhtControllerFactory;
    public static DHTControllerFactory getDHTControllerFactory() { return dhtControllerFactory.get(); }
    
    @Inject public static Provider<PingReplyFactory> pingReplyFactory;
    public static final PingReplyFactory getPingReplyFactory() { return pingReplyFactory.get(); }
    
    @Inject public static Provider<PushProxiesValueFactory> pushProxiesValueFactory;
    public static final PushProxiesValueFactory getPushProxiesValueFactory() { return pushProxiesValueFactory.get(); }
    
    @Inject public static Provider<HandshakeResponderFactory> handshakeResponderFactory;
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return handshakeResponderFactory.get(); }
   
    @Inject public static Provider<HeadersFactory> headersFactory;
    public static HeadersFactory getHeadersFactory() { return headersFactory.get(); }
    
    @Inject public static Provider<PushEndpointFactory> pushEndpointFactory;
    public static final PushEndpointFactory getPushEndpointFactory() { return pushEndpointFactory.get(); }
 
    @Inject public static Provider<ManagedTorrentFactory> managedTorrentFactory;
    public static ManagedTorrentFactory getManagedTorrentFactory() { return managedTorrentFactory.get(); }
    
    @Inject public static Provider<TorrentManager> torrentManager;
    public static TorrentManager getTorrentManager() { return torrentManager.get(); }
    
    @Inject public static Provider<TrackerManagerFactory> trackerManagerFactory;
    public static TrackerManagerFactory getTrackerManagerFactory() { return trackerManagerFactory.get(); }

    @Inject public static Provider<TrackerFactory> trackerFactory;
    public static TrackerFactory getTrackerFactory() { return trackerFactory.get(); }
    
    @Inject public static Provider<HTTPHeaderUtils> httpHeaderUtils;
    public static final HTTPHeaderUtils getHTTPHeaderUtils() { return httpHeaderUtils.get(); }
    
    @Inject public static Provider<FeaturesWriter> featuresWriter;
    public static FeaturesWriter getFeaturesWriter() { return featuresWriter.get(); }

    @Inject public static Provider<DownloadWorkerFactory> downloadWorkerFactory;
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return downloadWorkerFactory.get(); }
 
    @Inject public static Provider<HTTPDownloaderFactory> httpDownloaderFactory;
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return httpDownloaderFactory.get(); }

    @Inject public static Provider<HeadPongFactory> headPongFactory;
    public static HeadPongFactory getHeadPongFactory() { return headPongFactory.get(); }
 
    @Inject public static Provider<UploadManager> uploadManager;
    public static UploadManager getUploadManager() { return uploadManager.get(); }

    @Inject public static Provider<FileManager> fileManager;
    public static FileManager getFileManager() { return fileManager.get(); }
    
    @Inject public static Provider<UploadSlotManager> uploadSlotManager;
    public static UploadSlotManager getUploadSlotManager() { return uploadSlotManager.get(); }

    @Inject public static Provider<QueryHandlerFactory> queryHandlerFactory;
    public static QueryHandlerFactory getQueryHandlerFactory() { return queryHandlerFactory.get(); }
    
    @Inject public static Provider<QueryRequestFactory> queryRequestFactory;
    public static QueryRequestFactory getQueryRequestFactory() { return queryRequestFactory.get(); }
    
    @Inject public static Provider<ManagedConnectionFactory> managedConnectionFactory;
    public static ManagedConnectionFactory getManagedConnectionFactory() { return managedConnectionFactory.get(); }
       
    @Inject public static Provider<HostDataFactory> hostDataFactory;
    public static HostDataFactory getHostDataFactory() { return hostDataFactory.get(); }

    @Inject public static Provider<SourceRankerFactory> sourceRankerFactory ;
    public static SourceRankerFactory getSourceRankerFactory() { return sourceRankerFactory.get(); }

    @Inject public static Provider<SocketsManager> socketsManager;
    public static SocketsManager getSocketsManager() { return socketsManager.get(); }
    
    @Inject public static Provider<VerifyingFileFactory> verifyingFileFactory;
    public static VerifyingFileFactory getVerifyingFileFactory() { return verifyingFileFactory.get(); }
    
    @Inject public static Provider<DiskController> diskController;
    public static DiskController getDiskController() { return diskController.get(); }    
    
    @Inject public static Provider<AltLocValueFactory> altLocValueFactory;
    public static final AltLocValueFactory getAltLocValueFactory() { return altLocValueFactory.get(); }
    
    @Inject public static Provider<AlternateLocationFactory> alternateLocationFactory;
    public static final AlternateLocationFactory getAlternateLocationFactory() { return alternateLocationFactory.get(); }
      
    @Inject public static Provider<LocalFileDetailsFactory> localFileDetailsFactory ;
    public static final LocalFileDetailsFactory getLocalFileDetailsFactory() { return localFileDetailsFactory.get(); }

    
}
