package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.ManagedTorrentFactoryImpl;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerFactoryImpl;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactoryImpl;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactoryImpl;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.ManagedConnectionFactory;
import com.limegroup.gnutella.connection.ManagedConnectionFactoryImpl;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTControllerFactoryImpl;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerImpl;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.AltLocValueFactoryImpl;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactoryImpl;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactoryImpl;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactoryImpl;
import com.limegroup.gnutella.downloader.SourceRankerFactory;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactoryImpl;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.HostDataFactoryImpl;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QueryHandlerFactoryImpl;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
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
    
    public static final Provider<UploadManager> uploadManager = new Provider<UploadManager>() {
        public UploadManager get() {
            return RouterService.getUploadManager();
        }
    };
    
    public static final Provider<FileManager> fileManager = new Provider<FileManager>() {
        public FileManager get() {
            return RouterService.getFileManager();
        }
    };
    
    public static final Provider<TorrentManager> torrentManager = new Provider<TorrentManager>() {
        public TorrentManager get() {
            return RouterService.getTorrentManager();
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
    // hack that constructs things -- closest to the real world!
    public static final Provider<StandardMessageRouter> newStandardMessageRouter = new Provider<StandardMessageRouter>() {
      public StandardMessageRouter get() {
          return new StandardMessageRouter(getNetworkManager(), getQueryRequestFactory(), getQueryHandlerFactory(), onDemandUnicaster, getHeadPongFactory(), getPingReplyFactory());
        }  
    };
    public static final StandardMessageRouter getNewStandardMessageRouter() { return newStandardMessageRouter.get(); }
    
    
    //-------------------------------------------
    // hack providers that construct new things, and getters for getting them easily.
    

    
    public static final Provider<LocalFileDetailsFactory> localFileDetailsFactory = new AbstractLazySingletonProvider<LocalFileDetailsFactory>() {
        @Override
        protected LocalFileDetailsFactory createObject() {
            return new LocalFileDetailsFactoryImpl(getNetworkManager());
        }
    };
    public static final LocalFileDetailsFactory getLocalFileDetailsFactory() { return localFileDetailsFactory.get(); }

    public static final Provider<PushEndpointFactory> pushEndpointFactory = new AbstractLazySingletonProvider<PushEndpointFactory>() {
        @Override
        protected PushEndpointFactory createObject() {
            return new PushEndpointFactoryImpl(getNetworkManager());
        }
    };
    public static final PushEndpointFactory getPushEndpointFactory() { return pushEndpointFactory.get(); }
    
    public static final Provider<AlternateLocationFactory> alternateLocationFactory = new AbstractLazySingletonProvider<AlternateLocationFactory>() {
      @Override
        protected AlternateLocationFactory createObject() {
          return new AlternateLocationFactoryImpl(getNetworkManager());
        }  
    };
    public static final AlternateLocationFactory getAlternateLocationFactory() { return alternateLocationFactory.get(); }
    
    public static final Provider<AltLocValueFactory> altLocValueFactory = new AbstractLazySingletonProvider<AltLocValueFactory>() {
        @Override
        protected AltLocValueFactory createObject() {
            return new AltLocValueFactoryImpl(getNetworkManager());
        }  
    };
    public static final AltLocValueFactory getAltLocValueFactory() { return altLocValueFactory.get(); }
    
    public static final Provider<DiskController> diskController = new AbstractLazySingletonProvider<DiskController>() {
        protected DiskController createObject() {
            return new DiskController();
        }
    };
    public static DiskController getDiskController() { return diskController.get(); }
    
    public static final Provider<VerifyingFileFactory> verifyingFileFactory = new AbstractLazySingletonProvider<VerifyingFileFactory>() {
        protected VerifyingFileFactory createObject() {
            return new VerifyingFileFactory(diskController.get());
        }
    };
    public static VerifyingFileFactory getVerifyingFileFactory() { return verifyingFileFactory.get(); }
    
    public static final Provider<SocketsManager> socketsManager = new AbstractLazySingletonProvider<SocketsManager>() {
        protected SocketsManager createObject() {
            return new SocketsManager();
        }
    };
    public static SocketsManager getSocketsManager() { return socketsManager.get(); }
    
    public static final Provider<SourceRankerFactory> sourceRankerFactory = new AbstractLazySingletonProvider<SourceRankerFactory>() {
        protected SourceRankerFactory createObject() {
            return new SourceRankerFactory(getNetworkManager());
        }
    };
    public static SourceRankerFactory getSourceRankerFactory() { return sourceRankerFactory.get(); }

    public static final Provider<HostDataFactory> hostDataFactory = new AbstractLazySingletonProvider<HostDataFactory>() {
        @Override
        protected HostDataFactory createObject() {
            return new HostDataFactoryImpl(getNetworkManager());
        }
    };
    public static HostDataFactory getHostDataFactory() { return hostDataFactory.get(); }
    
    public static final Provider<QueryRequestFactory> queryRequestFactory = new AbstractLazySingletonProvider<QueryRequestFactory>() {
        @Override
        protected QueryRequestFactory createObject() {
            return new QueryRequestFactoryImpl(getNetworkManager());
        }
    };
    public static QueryRequestFactory getQueryRequestFactory() { return queryRequestFactory.get(); }
    
    public static Provider<ManagedConnectionFactory> managedConnectionFactory = new AbstractLazySingletonProvider<ManagedConnectionFactory>() {
        @Override
        protected ManagedConnectionFactory createObject() {
            return new ManagedConnectionFactoryImpl(connectionManager.get(), getNetworkManager(), getQueryRequestFactory(), getHeadersFactory(), getHandshakeResponderFactory());
        }
    };
    public static ManagedConnectionFactory getManagedConnectionFactory() { return managedConnectionFactory.get(); }
   
    public static Provider<QueryHandlerFactory> queryHandlerFactory = new AbstractLazySingletonProvider<QueryHandlerFactory>() {
        protected QueryHandlerFactory createObject() {
            return new QueryHandlerFactoryImpl(getQueryRequestFactory());
        }
    };
    public static QueryHandlerFactory getQueryHandlerFactory() { return queryHandlerFactory.get(); }
    
    public static final Provider<HeadPongFactory> headPongFactory = new AbstractLazySingletonProvider<HeadPongFactory>() {
        @Override
        protected HeadPongFactory createObject() {
            return new HeadPongFactoryImpl(getNetworkManager(), uploadManager, fileManager);
        }
    };
    public static HeadPongFactory getHeadPongFactory() { return headPongFactory.get(); }
    
    public static final Provider<HTTPDownloaderFactory> httpDownloaderFactory = new AbstractLazySingletonProvider<HTTPDownloaderFactory>() {
        protected HTTPDownloaderFactory createObject() {
            return new HTTPDownloaderFactoryImpl(getNetworkManager());
        }
    };
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return httpDownloaderFactory.get(); }
    
    public static final Provider<DownloadWorkerFactory> downloadWorkerFactory = new AbstractLazySingletonProvider<DownloadWorkerFactory>() {
        protected DownloadWorkerFactory createObject() {
            return new DownloadWorkerFactoryImpl(getHTTPDownloaderFactory());
        }
    };
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return downloadWorkerFactory.get(); }
    
    public static final Provider<FeaturesWriter> featuresWriter = new AbstractLazySingletonProvider<FeaturesWriter>() {
        protected FeaturesWriter createObject() {
            return new FeaturesWriter(getNetworkManager());
        }
    };
    public static FeaturesWriter getFeaturesWriter() { return featuresWriter.get(); }
    
    public static final Provider<HTTPHeaderUtils> httpHeaderUtils = new AbstractLazySingletonProvider<HTTPHeaderUtils>() {
        protected HTTPHeaderUtils createObject() {
            return new HTTPHeaderUtils(getFeaturesWriter(), getNetworkManager());
        }
    };
    public static final HTTPHeaderUtils getHTTPHeaderUtils() { return httpHeaderUtils.get(); }
    
    public static final Provider<TrackerFactory> trackerFactory = new AbstractLazySingletonProvider<TrackerFactory>() {
        protected TrackerFactory createObject() {
            return new TrackerFactoryImpl(getNetworkManager());
        }
    };
    public static TrackerFactory getTrackerFactory() { return trackerFactory.get(); }
    
    public static final Provider<TrackerManagerFactory> trackerManagerFactory = new AbstractLazySingletonProvider<TrackerManagerFactory>() {
        protected TrackerManagerFactory createObject() {
            return new TrackerManagerFactoryImpl(getTrackerFactory());
        }
    };
    public static TrackerManagerFactory getTrackerManagerFactory() { return trackerManagerFactory.get(); }

    public static final Provider<ManagedTorrentFactory> managedTorrentFactory = new AbstractLazySingletonProvider<ManagedTorrentFactory>() {
        protected ManagedTorrentFactory createObject() {
            return new ManagedTorrentFactoryImpl(torrentManager.get(), nioScheduledExecutorService.get(), getNetworkManager(), getTrackerManagerFactory());
        }
    };
    public static ManagedTorrentFactory getManagedTorrentFactory() { return managedTorrentFactory.get(); }
    
    public static final Provider<HeadersFactory> headersFactory = new AbstractLazySingletonProvider<HeadersFactory>() {
        protected HeadersFactory createObject() {
            return new HeadersFactoryImpl(getNetworkManager());
        }
    };
    public static HeadersFactory getHeadersFactory() { return headersFactory.get(); }
    
    public static final Provider<HandshakeResponderFactory> handshakeResponderFactory = new AbstractLazySingletonProvider<HandshakeResponderFactory>() {
        protected HandshakeResponderFactory createObject() {
            return new HandshakeResponderFactoryImpl(getHeadersFactory(), getNetworkManager());
        }
    };
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return handshakeResponderFactory.get(); }
    
    public static final Provider<PushProxiesValueFactory> pushProxiesValueFactory = new AbstractLazySingletonProvider<PushProxiesValueFactory>() {
        protected PushProxiesValueFactory createObject() {
            return new PushProxiesValueFactoryImpl(getNetworkManager(), getPushEndpointFactory());
        }
    };
    public static final PushProxiesValueFactory getPushProxiesValueFactory() { return pushProxiesValueFactory.get(); }

    public static final Provider<PingReplyFactory> pingReplyFactory = new AbstractLazySingletonProvider<PingReplyFactory>() {
        protected PingReplyFactory createObject() {
            return new PingReplyFactoryImpl(getNetworkManager());
        }        
    };
    public static final PingReplyFactory getPingReplyFactory() { return pingReplyFactory.get(); }
    
    
    static {
        LimeWireCore core = new LimeWireCore();
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
    
}
