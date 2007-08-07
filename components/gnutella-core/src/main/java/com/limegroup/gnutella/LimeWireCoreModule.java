package com.limegroup.gnutella;

import java.util.concurrent.Executor;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.ManagedTorrentFactoryImpl;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentEventListener;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerFactoryImpl;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactoryImpl;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactoryImpl;
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
import com.limegroup.gnutella.dht.io.LimeMessageDispatcherFactoryImpl;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.DownloadReferencesFactoryImpl;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactoryImpl;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactoryImpl;
import com.limegroup.gnutella.downloader.InNetworkCallback;
import com.limegroup.gnutella.downloader.RequeryManagerFactory;
import com.limegroup.gnutella.downloader.RequeryManagerFactoryImpl;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
import com.limegroup.gnutella.http.DefaultHttpExecutor;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactoryImpl;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.HostDataFactoryImpl;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QueryHandlerFactoryImpl;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactoryImpl;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.xml.MetaFileManager;

public class LimeWireCoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LimeWireCore.class);
        
        bind(NetworkManager.class).to(NetworkManagerImpl.class);
        bind(DHTManager.class).to(DHTManagerImpl.class);
        bind(DHTControllerFactory.class).to(DHTControllerFactoryImpl.class);
        bind(PingReplyFactory.class).to(PingReplyFactoryImpl.class);
        bind(PushProxiesValueFactory.class).to(PushProxiesValueFactoryImpl.class);
        bind(HandshakeResponderFactory.class).to(HandshakeResponderFactoryImpl.class);
        bind(HeadersFactory.class).to(HeadersFactoryImpl.class);
        bind(PushEndpointFactory.class).to(PushEndpointFactoryImpl.class);
        bind(ManagedTorrentFactory.class).to(ManagedTorrentFactoryImpl.class);
        bind(TrackerManagerFactory.class).to(TrackerManagerFactoryImpl.class);
        bind(TrackerFactory.class).to(TrackerFactoryImpl.class);
        bind(DownloadWorkerFactory.class).to(DownloadWorkerFactoryImpl.class);
        bind(HTTPDownloaderFactory.class).to(HTTPDownloaderFactoryImpl.class);
        bind(HeadPongFactory.class).to(HeadPongFactoryImpl.class);
        bind(UploadManager.class).to(HTTPUploadManager.class);
        bind(HTTPUploadSessionManager.class).to(HTTPUploadManager.class);
        bind(QueryHandlerFactory.class).to(QueryHandlerFactoryImpl.class);
        bind(QueryRequestFactory.class).to(QueryRequestFactoryImpl.class);
        bind(ManagedConnectionFactory.class).to(ManagedConnectionFactoryImpl.class);
        bind(HostDataFactory.class).to(HostDataFactoryImpl.class);
        bind(AltLocValueFactory.class).to(AltLocValueFactoryImpl.class);
        bind(AlternateLocationFactory.class).to(AlternateLocationFactoryImpl.class);
        bind(LocalFileDetailsFactory.class).to(LocalFileDetailsFactoryImpl.class);
        bind(HttpExecutor.class).to(DefaultHttpExecutor.class);
        bind(HttpRequestHandlerFactory.class).to(HttpRequestHandlerFactoryImpl.class);
        bind(FileManagerController.class).to(FileManagerControllerImpl.class);
        bind(ResponseFactory.class).to(ResponseFactoryImpl.class);
        bind(QueryReplyFactory.class).to(QueryReplyFactoryImpl.class);
        bind(RequeryManagerFactory.class).to(RequeryManagerFactoryImpl.class);
        bind(DownloadReferencesFactory.class).to(DownloadReferencesFactoryImpl.class);
        bind(MessageDispatcherFactory.class).to(LimeMessageDispatcherFactoryImpl.class);
        bind(CapabilitiesVMFactory.class).to(CapabilitiesVMFactoryImpl.class);
        
        // DPINJ: statically injecting this for now...
        requestStaticInjection(SimppManager.class);
       
        // DPINJ: Need to add interface to these classes
        //----------------------------------------------
        //bind(UDPService.class)
        //bind(Acceptor.class);
        //bind(ConnectionManager.class);
        //bind(TorrentManager.class);
        //bind(HTTPHeaderUtils.class)
        //bind(FeaturesWriter.class);
        bind(FileManager.class).to(MetaFileManager.class);
        //bind(UploadSlotManager.class);
        //bind(SourceRankerFactory.class);
        //bind(SocketsManager.class);
        //bind(VerifyingFileFactory.class);
        //bind(DiskController.class);
        bind(new TypeLiteral<EventDispatcher<TorrentEvent, TorrentEventListener>>(){}).to(TorrentManager.class);
        //bind(ForMeReplyHandler.class);
        bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(ForMeReplyHandler.class);
        //bind(QueryUnicaster.class);
        //bind(OnDemandUnicaster.class);
        bind(MessageRouter.class).to(StandardMessageRouter.class);
        //bind(DownloadManager.class);
        //bind(AltLocFinder.class);
        //bind(ConnectionDispatcher.class);
        //bind(HTTPAcceptor.class); //the one in browser
        //bind(HostCatcher.class);
        //bind(HTTPAcceptor.class); // the one in gnutella
        //bind(PushManager.class);
        //bind(ResponseVerifier.class);
        //bind(SearchResultHandler.class);
        //bind(AltLocManager.class);
        //bind(ContentManager.class);
        bind(IPFilter.class).toProvider(new IPFilterProvider());
        //bind(HostileFilter.class);
        //bind(NetworkUpdateSanityChecker.class);
        //bind(BandwidthManager.class);
        //bind(QueryStats.class);
        //bind(NodeAssigner.class);
        bind(Statistics.class).asEagerSingleton(); // DPINJ: need to move time-capture to initialization
        bind(SecureMessageVerifier.class).toProvider(new SecureMessageVerifierProvider());
        //bind(CreationTimeCache.class);
        //bind(UrnCache.class);
        //bind(StaticMessages.class);
        //bind(SavedFileManager.class);
        bind(DownloadCallback.class).to(ActivityCallback.class);
        bind(DownloadCallback.class).annotatedWith(Names.named("inNetwork")).to(InNetworkCallback.class);
        //bind(InNetworkCallback.class);
        //bind(MessageDispatcher.class);
        //bind(MulticastService.class);        
        //bind(ChatManager.class);
        //bind(BTLinkManagerFactory.class);
        //bind(ChokerFactory.class);
        //bind(DiskManagerFactory.class);
        //bind(BTConnectionFetcherFactory.class);
        //bind(IncomingConnectionHandler.class);
        //bind(ConnectionWatchdog.class);
        //bind(Pinger.class);
        //bind(PongCacher.class);
        //bind(UPnPManager.class);
        //bind(MutableGUIDFilter.class);
        //bind(LicenseCache.class);
        //bind(MessagesSupportedVendorMessage.class);
        //bind(QueryDispatcher.class);
        //bind(RatingTable.class);
        //bind(SpamManager.class);
        //bind(HashTreeNodeManager.class);
        //bind(TigerTreeCache.class);
        //bind(UpdateHandler.class);
        //bind(LimeXMLProperties.class);
        //bind(LimeXMLSchemaRepository.class);
        //bind(SchemaReplyCollectionMapper.class);        
        
        //DPINJ: Don't need interfaces really, but listing them just 'cause I want to list everything.
        //bind(BrowseRequestHandler.class);
        //bind(FileRequestHandler.class);
        //bind(FreeLoaderRequestHandler.class);
        //bind(LimitReachedRequestHandler.class); // Not really bound, because factory creates it
        //bind(PushProxyRequestHandler.class);
        
        
        // For NodeAssigner...
        bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).to(UploadManager.class);
        bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).to(DownloadManager.class);
                
        
        // DPINJ: Could delay instantiation...
        //----------------------------------------------
        bind(Executor.class).annotatedWith(Names.named("dhtExecutor")).toInstance(ExecutorsHelper.newProcessingQueue("DHT-Executor"));
 
    }    
    
    // DPINJ: Do i need the lazy-singleton-ness here?
    @Singleton
    private static class IPFilterProvider extends AbstractLazySingletonProvider<IPFilter> {
        @Override
        protected IPFilter createObject() {
            return new IPFilter(false);
        }
    }
    
    @Singleton
    private static class SecureMessageVerifierProvider extends AbstractLazySingletonProvider<SecureMessageVerifier> {
        @Override
        protected SecureMessageVerifier createObject() {
            return new SecureMessageVerifier("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7" +
                    "VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5" +
                    "RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O" +
                    "5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV" +
                    "37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2B" +
                    "BOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOE" +
                    "EBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7Y" +
                    "L7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76" +
                    "Z5ESUA4BQUAAFAMBACDW4TNFXK772ZQN752VPKQSFXJWC6PPSIVTHKDNLRUIQ7UF" +
                    "4J2NF6J2HC5LVC4FO4HYLWEWSB3DN767RXILP37KI5EDHMFAU6HIYVQTPM72WC7FW" +
                    "SAES5K2KONXCW65VSREAPY7BF24MX72EEVCZHQOCWHW44N4RG5NPH2J4EELDPXMNR" +
                    "WNYU22LLSAMBUBKW3KU4QCQXG7NNY", null);    
        }
    };
        
}