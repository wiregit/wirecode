package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

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
    
    
    //-------------------------------------------
    // hack that always constructs new things -- hard to guicify...
    public static final Provider<StandardMessageRouter> newStandardMessageRouter = new Provider<StandardMessageRouter>() {
      public StandardMessageRouter get() {
          return new StandardMessageRouter(getNetworkManager(), getQueryRequestFactory(), getQueryHandlerFactory(), onDemandUnicaster, getHeadPongFactory(), getPingReplyFactory());
        }  
    };
    public static final StandardMessageRouter getNewStandardMessageRouter() { return newStandardMessageRouter.get(); }
    
    
    //-------------------------------------------
    public final static LimeWireCore core = LimeWireCore.create();
    public static LimeWireCore getCore() { return core; }
    public static Acceptor getAcceptor() { return core.getAcceptor(); }    
    public static UDPService getUdpService() { return core.getUdpService(); }    
    public static NetworkManager getNetworkManager() { return core.getNetworkManager(); }    
    public static ConnectionManager getConnectionManager() { return core.getConnectionManager(); }    
    public static DHTManager getDHTManager() { return core.getDhtManager(); }    
    public static DHTControllerFactory getDHTControllerFactory() { return core.getDhtControllerFactory(); }    
    public static PingReplyFactory getPingReplyFactory() { return core.getPingReplyFactory(); }    
    public static PushProxiesValueFactory getPushProxiesValueFactory() { return core.getPushProxiesValueFactory(); }    
    public static HandshakeResponderFactory getHandshakeResponderFactory() { return core.getHandshakeResponderFactory(); }   
    public static HeadersFactory getHeadersFactory() { return core.getHeadersFactory(); }    
    public static PushEndpointFactory getPushEndpointFactory() { return core.getPushEndpointFactory(); } 
    public static ManagedTorrentFactory getManagedTorrentFactory() { return core.getManagedTorrentFactory(); }    
    public static TorrentManager getTorrentManager() { return core.getTorrentManager(); }    
    public static TrackerManagerFactory getTrackerManagerFactory() { return core.getTrackerManagerFactory(); }
    public static TrackerFactory getTrackerFactory() { return core.getTrackerFactory(); }    
    public static HTTPHeaderUtils getHTTPHeaderUtils() { return core.getHttpHeaderUtils(); }    
    public static FeaturesWriter getFeaturesWriter() { return core.getFeaturesWriter(); }
    public static DownloadWorkerFactory getDownloadWorkerFactory() { return core.getDownloadWorkerFactory(); } 
    public static HeadPongFactory getHeadPongFactory() { return core.getHeadPongFactory(); } 
    public static UploadManager getUploadManager() { return core.getUploadManager(); }
    public static FileManager getFileManager() { return core.getFileManager(); }    
    public static UploadSlotManager getUploadSlotManager() { return core.getUploadSlotManager(); }
    public static QueryHandlerFactory getQueryHandlerFactory() { return core.getQueryHandlerFactory(); }    
    public static QueryRequestFactory getQueryRequestFactory() { return core.getQueryRequestFactory(); }    
    public static ManagedConnectionFactory getManagedConnectionFactory() { return core.getManagedConnectionFactory(); }       
    public static HostDataFactory getHostDataFactory() { return core.getHostDataFactory(); }
    public static SourceRankerFactory getSourceRankerFactory() { return core.getSourceRankerFactory(); }
    public static SocketsManager getSocketsManager() { return core.getSocketsManager(); }    
    public static VerifyingFileFactory getVerifyingFileFactory() { return core.getVerifyingFileFactory(); }    
    public static DiskController getDiskController() { return core.getDiskController(); }    
    public static AltLocValueFactory getAltLocValueFactory() { return core.getAltLocValueFactory(); }    
    public static AlternateLocationFactory getAlternateLocationFactory() { return core.getAlternateLocationFactory(); }      
    public static LocalFileDetailsFactory getLocalFileDetailsFactory() { return core.getLocalFileDetailsFactory(); }
    public static HTTPDownloaderFactory getHTTPDownloaderFactory() { return core.getHttpDownloaderFactory(); }
    public static ForMeReplyHandler getForMeReplyHandler() { return core.getForMeReplyHandler(); }

    
}
