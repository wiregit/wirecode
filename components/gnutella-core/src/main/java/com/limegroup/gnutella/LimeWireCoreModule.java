package com.limegroup.gnutella;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.http.LimeWireHttpModule;
import org.limewire.inject.AbstractModule;
import org.limewire.inspection.Inspector;
import org.limewire.inspection.InspectorImpl;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.io.Pools;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.LimeWireNetModule;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.DefaultUDPSelectorProviderFactory;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.RUDPSettings;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.rudp.UDPSelectorProviderFactory;
import org.limewire.rudp.UDPService;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.SecureMessageVerifierImpl;
import org.limewire.security.SecurityToken;
import org.limewire.security.SettingsProvider;
import org.limewire.statistic.StatisticsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.BTConnectionFactoryImpl;
import com.limegroup.bittorrent.BTContextFactory;
import com.limegroup.bittorrent.BTContextFactoryImpl;
import com.limegroup.bittorrent.BTDownloaderFactory;
import com.limegroup.bittorrent.BTDownloaderFactoryImpl;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.BTUploaderFactoryImpl;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.ManagedTorrentFactoryImpl;
import com.limegroup.bittorrent.TorrentEvent;
import com.limegroup.bittorrent.TorrentEventListener;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.choking.ChokerFactoryImpl;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactoryImpl;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerFactoryImpl;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactoryImpl;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactoryImpl;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactory;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactoryImpl;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactory;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactoryImpl;
import com.limegroup.gnutella.chat.InstantMessengerFactory;
import com.limegroup.gnutella.chat.InstantMessengerFactoryImpl;
import com.limegroup.gnutella.connection.ConnectionBandwidthStatistics;
import com.limegroup.gnutella.connection.ConnectionBandwidthStatisticsImpl;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionCapabilitiesImpl;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.ConnectionCheckerManagerImpl;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.connection.MessageReaderFactoryImpl;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.connection.RoutedConnectionFactoryImpl;
import com.limegroup.gnutella.connection.UDPConnectionChecker;
import com.limegroup.gnutella.connection.UDPConnectionCheckerImpl;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTBootstrapperFactoryImpl;
import com.limegroup.gnutella.dht.DHTControllerFacade;
import com.limegroup.gnutella.dht.DHTControllerFacadeImpl;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTControllerFactoryImpl;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerImpl;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactoryImpl;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.AltLocValueFactoryImpl;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactoryImpl;
import com.limegroup.gnutella.dht.io.LimeMessageDispatcherFactoryImpl;
import com.limegroup.gnutella.downloader.AutoDownloadDetails;
import com.limegroup.gnutella.downloader.DownloadReferencesFactory;
import com.limegroup.gnutella.downloader.DownloadReferencesFactoryImpl;
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactoryImpl;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactory;
import com.limegroup.gnutella.downloader.GnutellaDownloaderFactoryImpl;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactoryImpl;
import com.limegroup.gnutella.downloader.InNetworkCallback;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.LWSIntegrationServices;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesDelegate;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesImpl;
import com.limegroup.gnutella.downloader.PurchasedStoreDownloaderFactory;
import com.limegroup.gnutella.downloader.PurchasedStoreDownloaderFactoryImpl;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;
import com.limegroup.gnutella.downloader.RequeryManagerFactory;
import com.limegroup.gnutella.downloader.RequeryManagerFactoryImpl;
import com.limegroup.gnutella.filters.HostileFilter;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.LocalIPFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.filters.SpamFilterFactoryImpl;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HandshakeServices;
import com.limegroup.gnutella.handshaking.HandshakeServicesImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
import com.limegroup.gnutella.http.DefaultHttpExecutor;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseFactoryImpl;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerImpl;
import com.limegroup.gnutella.messagehandlers.MessageHandlerBinderImpl;
import com.limegroup.gnutella.messages.LocalPongInfo;
import com.limegroup.gnutella.messages.LocalPongInfoImpl;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactoryImpl;
import com.limegroup.gnutella.messages.MessageParserBinder;
import com.limegroup.gnutella.messages.MessageParserBinderImpl;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactoryImpl;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PingRequestFactoryImpl;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinder;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinderImpl;
import com.limegroup.gnutella.rudp.LimeRUDPContext;
import com.limegroup.gnutella.rudp.LimeRUDPSettings;
import com.limegroup.gnutella.rudp.LimeUDPService;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageFactory;
import com.limegroup.gnutella.search.HostDataFactory;
import com.limegroup.gnutella.search.HostDataFactoryImpl;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryDispatcherImpl;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QueryHandlerFactoryImpl;
import com.limegroup.gnutella.settings.SettingsBackedProxySettings;
import com.limegroup.gnutella.settings.SettingsBackedSocketBindingSettings;
import com.limegroup.gnutella.spam.AddressToken;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.uploader.FileResponseEntityFactory;
import com.limegroup.gnutella.uploader.FileResponseEntityFactoryImpl;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactoryImpl;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadSlotManagerImpl;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.version.UpdateCollectionFactory;
import com.limegroup.gnutella.version.UpdateCollectionFactoryImpl;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.version.UpdateHandlerImpl;
import com.limegroup.gnutella.version.UpdateMessageVerifier;
import com.limegroup.gnutella.version.UpdateMessageVerifierImpl;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactoryImpl;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactory;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactoryImpl;
import com.limegroup.gnutella.xml.MetaFileManager;

/**
 * The module that defines what implementations are used within
 * LimeWire's core.  This class can be constructed with or without
 * an ActivitiyCallback class.  If it is without, then another module
 * must explicitly identify which class is going to define the
 * ActivityCallback.
 */
public class LimeWireCoreModule extends AbstractModule {
 
	private final Class<? extends ActivityCallback> activityCallbackClass;
    
    public LimeWireCoreModule() {
        this(null);
    }
    
    public LimeWireCoreModule(Class<? extends ActivityCallback> activityCallbackClass) {
        this.activityCallbackClass = activityCallbackClass;
    }
    
    @Override
    protected void configure() {
        binder().install(new LimeWireCommonModule());
        binder().install(new LimeWireNetModule(SettingsBackedProxySettings.class, SettingsBackedSocketBindingSettings.class));
        
        // TODO  - is there a way to make it more obvious what is happeneing here?
        // httpExecutor is bound (and used) in LimeWireHttpModule; 
        // LimeWireCoreModule overrides this.
        // httpExecutor is an alias
        //bind(Key.get(ScheduledExecutorService.class, Names.named("httpExecutor"))).to(Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor")));
        
        binder().install(new LimeWireHttpModule());
        
        bind(LimeWireCore.class);
        
        if(activityCallbackClass != null) {
            bind(ActivityCallback.class).to(activityCallbackClass);
        }        

        bind(DownloadCallback.class).to(ActivityCallback.class);
        bind(DownloadCallback.class).annotatedWith(Names.named("inNetwork")).to(InNetworkCallback.class);        
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
        bind(RoutedConnectionFactory.class).to(RoutedConnectionFactoryImpl.class);
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
        bind(UDPSelectorProviderFactory.class).to(DefaultUDPSelectorProviderFactory.class);
        bind(RUDPContext.class).to(LimeRUDPContext.class);
        bind(UDPSelectorProvider.class).toProvider(DefaultUDPSelectorProviderFactory.class);
        bind(UDPMultiplexor.class).toProvider(UDPMultiplexorProvider.class);
        bind(UDPService.class).to(LimeUDPService.class);
        bind(RUDPMessageFactory.class).to(LimeRUDPMessageFactory.class);
        bind(RUDPSettings.class).to(LimeRUDPSettings.class);
        bind(RUDPMessageFactory.class).annotatedWith(Names.named("delegate")).to(DefaultMessageFactory.class);
        bind(BTContextFactory.class).to(BTContextFactoryImpl.class);
        bind(BTDownloaderFactory.class).to(BTDownloaderFactoryImpl.class);
        bind(LifecycleManager.class).to(LifecycleManagerImpl.class);
        bind(LocalPongInfo.class).to(LocalPongInfoImpl.class);
        bind(ConnectionServices.class).to(ConnectionServicesImpl.class);
        bind(SearchServices.class).to(SearchServicesImpl.class);
        bind(DownloadServices.class).to(DownloadServicesImpl.class);
        bind(UploadServices.class).to(UploadServicesImpl.class);
        bind(ApplicationServices.class).to(ApplicationServicesImpl.class);
        bind(SpamServices.class).to(SpamServicesImpl.class);
        bind(SpamFilterFactory.class).to(SpamFilterFactoryImpl.class);
        bind(DHTControllerFacade.class).to(DHTControllerFacadeImpl.class);
        bind(ChokerFactory.class).to(ChokerFactoryImpl.class);
        bind(BTConnectionFetcherFactory.class).to(BTConnectionFetcherFactoryImpl.class);
        bind(ConnectionCheckerManager.class).to(ConnectionCheckerManagerImpl.class);
        bind(DHTBootstrapperFactory.class).to(DHTBootstrapperFactoryImpl.class);
        bind(DHTNodeFetcherFactory.class).to(DHTNodeFetcherFactoryImpl.class);
        bind(UDPReplyHandlerFactory.class).to(UDPReplyHandlerFactoryImpl.class);
        bind(UDPReplyHandlerCache.class).to(UDPReplyHandlerCacheImpl.class);
        bind(BTConnectionFactory.class).to(BTConnectionFactoryImpl.class);
        bind(SocketProcessor.class).to(AcceptorImpl.class);
        bind(PushedSocketHandlerRegistry.class).to(PushDownloadManager.class);
        bind(DownloadManager.class).to(DownloadManagerImpl.class).asEagerSingleton();
        bind(BrowseHostHandlerManagerImpl.class).asEagerSingleton();
        bind(ReplyNumberVendorMessageFactory.class).to(ReplyNumberVendorMessageFactoryImpl.class);
        bind(GuidMapManager.class).to(GuidMapManagerImpl.class);
        bind(BrowseHostHandlerManager.class).to(BrowseHostHandlerManagerImpl.class);
        bind(PushEndpointCache.class).to(PushEndpointCacheImpl.class);
        bind(FileResponseEntityFactory.class).to(FileResponseEntityFactoryImpl.class);
        bind(MessageFactory.class).to(MessageFactoryImpl.class);
        bind(MessageReaderFactory.class).to(MessageReaderFactoryImpl.class);
        bind(MessageParserBinder.class).to(MessageParserBinderImpl.class);
        bind(VendorMessageFactory.class).to(VendorMessageFactoryImpl.class);
        bind(VendorMessageParserBinder.class).to(VendorMessageParserBinderImpl.class);
        bind(UDPCrawlerPongFactory.class).to(UDPCrawlerPongFactoryImpl.class);
        bind(UDPHostCacheFactory.class).to(UDPHostCacheFactoryImpl.class);
        bind(LimeXMLReplyCollectionFactory.class).to(LimeXMLReplyCollectionFactoryImpl.class);
        bind(LicenseFactory.class).to(LicenseFactoryImpl.class);
        bind(LimeXMLDocumentFactory.class).to(LimeXMLDocumentFactoryImpl.class);
        bind(InstantMessengerFactory.class).to(InstantMessengerFactoryImpl.class);
        bind(SaveLocationManager.class).to(DownloadManager.class);
        bind(GnutellaDownloaderFactory.class).to(GnutellaDownloaderFactoryImpl.class);
        bind(PurchasedStoreDownloaderFactory.class).to(PurchasedStoreDownloaderFactoryImpl.class);
        bind(BTUploaderFactory.class).to(BTUploaderFactoryImpl.class);
        bind(PingRequestFactory.class).to(PingRequestFactoryImpl.class);
        bind(IpPortContentAuthorityFactory.class).to(IpPortContentAuthorityFactoryImpl.class);
        bind(UpdateCollectionFactory.class).to(UpdateCollectionFactoryImpl.class);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("global")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("local")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(UDPPinger.class).to(UDPPingerImpl.class);
        bind(UDPConnectionChecker.class).to(UDPConnectionCheckerImpl.class);
        bind(Inspector.class).to(InspectorImpl.class);
        bind(ConnectionCapabilities.class).to(ConnectionCapabilitiesImpl.class);
        bind(ConnectionBandwidthStatistics.class).to(ConnectionBandwidthStatisticsImpl.class);
        bind(LWSManager.class).to(LWSManagerImpl.class);
        bind(LWSIntegrationServices.class).to(LWSIntegrationServicesImpl.class);
        bind(LWSIntegrationServicesDelegate.class).to(DownloadManager.class);
        bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderImpl.class);
        bind(SettingsProvider.class).to(MacCalculatorSettingsProviderImpl.class);
        bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(ForMeReplyHandler.class);
        bind(MessageRouter.class).to(StandardMessageRouter.class);
        bind(IPFilter.class).annotatedWith(Names.named("ipFilter")).to(LocalIPFilter.class);
        bind(IPFilter.class).annotatedWith(Names.named("hostileFilter")).to(HostileFilter.class);
        bind(UploadSlotManager.class).to(UploadSlotManagerImpl.class);
        bind(new TypeLiteral<EventDispatcher<TorrentEvent, TorrentEventListener>>(){}).to(TorrentManager.class);
        bind(BandwidthManager.class).to(BandwidthManagerImpl.class);
        bind(SecureMessageVerifier.class).toProvider(SecureMessageVerifierProvider.class);
        bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toProvider(InspectionVerifierProvider.class);
        bind(PongCacher.class).to(PongCacherImpl.class);        
        bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).to(UploadManager.class);     // For NodeAssigner.
        bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).to(DownloadManager.class); // For NodeAssigner.
        bind(NIODispatcher.class).toProvider(NIODispatcherProvider.class);
        bind(ByteBufferCache.class).toProvider(ByteBufferCacheProvider.class);
        bind(ResponseVerifier.class).to(ResponseVerifierImpl.class);
        bind(HandshakeServices.class).to(HandshakeServicesImpl.class);
        bind(ConnectionManager.class).to(ConnectionManagerImpl.class);
        bind(MessageHandlerBinder.class).to(MessageHandlerBinderImpl.class);
        bind(QueryDispatcher.class).to(QueryDispatcherImpl.class);
        bind(Acceptor.class).to(AcceptorImpl.class);        
        bind(UpdateHandler.class).to(UpdateHandlerImpl.class);
        bind(SecurityToken.TokenProvider.class).to(SecurityToken.AddressSecurityTokenProvider.class);
        bind(UpdateMessageVerifier.class).to(UpdateMessageVerifierImpl.class);
        
        
        bindAll(Names.named("unlimitedExecutor"), ExecutorService.class, UnlimitedExecutorProvider.class, Executor.class);
        bindAll(Names.named("backgroundExecutor"), ScheduledExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class);
        bindAll(Names.named("httpExecutor"), ScheduledExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class);
        bindAll(Names.named("dhtExecutor"), ExecutorService.class, DHTExecutorProvider.class, Executor.class);
        bindAll(Names.named("messageExecutor"), ExecutorService.class, MessageExecutorProvider.class, Executor.class);
        bindAll(Names.named("nioExecutor"), ScheduledExecutorService.class, NIOScheduledExecutorServiceProvider.class, ExecutorService.class, Executor.class);

        // TODO: statically injecting this for now...
        requestStaticInjection(UDPSelectorProvider.class);  // This one might need to stay
        requestStaticInjection(AddressToken.class);
        requestStaticInjection(RemoteFileDesc.class);
        requestStaticInjection(HashTree.class);
        requestStaticInjection(IncompleteFileManager.class);
        requestStaticInjection(AutoDownloadDetails.class);
        requestStaticInjection(LimeXMLDocument.class);
        requestStaticInjection(StatisticsManager.class);
        requestStaticInjection(Pools.class);
        requestStaticInjection(LocalSocketAddressService.class);
                        
        // TODO: This is odd -- move to initialize & LifecycleManager?
        bind(Statistics.class).asEagerSingleton();
        
        // TODO: Need to add interface to these classes
        //----------------------------------------------
        bind(FileManager.class).to(MetaFileManager.class);
    }
    
    @Singleton
    private static class SecureMessageVerifierProvider extends AbstractLazySingletonProvider<SecureMessageVerifier> {
        @Override
        protected SecureMessageVerifier createObject() {
            return new SecureMessageVerifierImpl("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7" +
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
    
    @Singleton
    private static class InspectionVerifierProvider extends AbstractLazySingletonProvider<SecureMessageVerifier> {
        protected SecureMessageVerifier createObject() {
            return new SecureMessageVerifierImpl("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA" +
                    "7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WW" +
                    "J5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7" +
                    "O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37" +
                    "SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655" +
                    "S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2" +
                    "ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7" +
                    "IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76" +
                    "Z5ESUA4BQUAAFAMBACDJO4PTIV3332EWTALOMF5V3RO5BVEMHPVD4INLMQRIZ5" +
                    "PW5RS7QJUGSINVNG4OTDO4FWJY5C3MQBQP7DXNOPQFJAVBCUE2VG3HWA34FPSLRIYBBGQVSQDQTQUS4" +
                    "T6HW3OQNG2DPVGCIIWTCK6XMW3SK6PEQBWH6MIAL4FX3OYVWRG2ZKVBHBMJ564CKEPYDW3" +
                    "TJRPIU4UA24I", null);
        }
    }
    
    @Singleton
    private static class UDPMultiplexorProvider extends AbstractLazySingletonProvider<UDPMultiplexor> {
        private final UDPSelectorProvider provider;
        private final NIODispatcher nioDispatcher;
        
        @Inject
        public UDPMultiplexorProvider(UDPSelectorProvider provider,
                NIODispatcher nioDispatcher) {
            this.provider = provider;
            this.nioDispatcher = nioDispatcher;
        }
        
        @Override
        protected UDPMultiplexor createObject() {
            UDPMultiplexor multiplexor = provider.openSelector();
            SelectableChannel socketChannel = provider.openSocketChannel();
            try {
                socketChannel.close();
            } catch(IOException ignored) {}
            nioDispatcher.registerSelector(multiplexor, socketChannel.getClass());
            return multiplexor;
        }
    }
    
    @Singleton
    private static class UnlimitedExecutorProvider extends AbstractLazySingletonProvider<ExecutorService> {
        protected ExecutorService createObject() {
            return ExecutorsHelper.newThreadPool(ExecutorsHelper.daemonThreadFactory("IdleThread"));
        }
    }
    
    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledExecutorService> {
        protected ScheduledExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
    
    @Singleton
    private static class MessageExecutorProvider extends AbstractLazySingletonProvider<ExecutorService> {
        protected ExecutorService createObject() {
            return ExecutorsHelper.newProcessingQueue("Message-Executor");
        }
    }

    @Singleton
    private static class DHTExecutorProvider extends AbstractLazySingletonProvider<ExecutorService> {
        protected ExecutorService createObject() {
            return ExecutorsHelper.newProcessingQueue("DHT-Executor");
        }
    }    
    
    ///////////////////////////////////////////////////////////////////////////
    /// BELOW ARE ALL HACK PROVIDERS THAT NEED TO BE UPDATED TO CONSTRUCT OBJECTS!
    // (This needs to wait till components are injected and stop using singletons too.)
    
    @Singleton
    private static class NIODispatcherProvider implements Provider<NIODispatcher> {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };
    
    @Singleton
    private static class ByteBufferCacheProvider implements Provider<ByteBufferCache> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public ByteBufferCacheProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    @Singleton
    private static class NIOScheduledExecutorServiceProvider implements Provider<ScheduledExecutorService> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public NIOScheduledExecutorServiceProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };
        
    ///////////////////////////////////////////////////////////////
    // !!! DO NOT ADD THINGS BELOW HERE !!!  PUT THEM ABOVE THE HACKS!
}