package com.limegroup.gnutella;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.ExecutorsHelper;

import com.google.inject.AbstractModule;
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
import com.limegroup.gnutella.downloader.DownloadWorkerFactory;
import com.limegroup.gnutella.downloader.DownloadWorkerFactoryImpl;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactory;
import com.limegroup.gnutella.downloader.HTTPDownloaderFactoryImpl;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
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
        bind(QueryHandlerFactory.class).to(QueryHandlerFactoryImpl.class);
        bind(QueryRequestFactory.class).to(QueryRequestFactoryImpl.class);
        bind(ManagedConnectionFactory.class).to(ManagedConnectionFactoryImpl.class);
        bind(HostDataFactory.class).to(HostDataFactoryImpl.class);
        bind(AltLocValueFactory.class).to(AltLocValueFactoryImpl.class);
        bind(AlternateLocationFactory.class).to(AlternateLocationFactoryImpl.class);
        bind(LocalFileDetailsFactory.class).to(LocalFileDetailsFactoryImpl.class);
       
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
        
        
        // DPINJ: Need to figure out what the hell to do with these.
        //----------------------------------------------        
        bind(ActivityCallback.class).toProvider(ProviderHacks.activityCallback);
        
        
        // DPINJ: Could delay instantiation...
        //----------------------------------------------
        bind(Executor.class).annotatedWith(Names.named("dhtExecutor")).toInstance(ExecutorsHelper.newProcessingQueue("DHT-Executor"));
        
        // DPINJ: Need to create & inject the provider w/ NIODispatcher somehow...
        //----------------------------------------------
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toProvider(ProviderHacks.nioScheduledExecutorService);
    }    
}