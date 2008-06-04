package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.ssl.SSLEngineTest;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.promotion.PromotionServices;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.service.ErrorService;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.statistic.StatisticAccumulator;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.SystemUtils;
import org.limewire.util.VersionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.dht.DHTPeerLocator;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;
import com.limegroup.bittorrent.handshaking.IncomingConnectionHandler;
import com.limegroup.gnutella.altlocs.DownloaderGuidAlternateLocationFinder;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.ControlRequestAcceptor;
import com.limegroup.gnutella.browser.LocalAcceptor;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.db.PushProxiesPublisher;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.LWSIntegrationServices;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.serial.conversion.DownloadUpgradeTask;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageHandler;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.LWSSettings;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.spam.RatingTable;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Provides an implementation of the <code>LifecycleManager</code> to start and
 * stop the LimeWire components.
 */
@Singleton
public class LifecycleManagerImpl implements LifecycleManager {
    
    private static final Log LOG = LogFactory.getLog(LifecycleManagerImpl.class);
   
    private final AtomicBoolean preinitializeBegin = new AtomicBoolean(false);
    private final AtomicBoolean preinitializeDone = new AtomicBoolean(false);
    private final AtomicBoolean backgroundBegin = new AtomicBoolean(false);
    private final AtomicBoolean backgroundDone = new AtomicBoolean(false);
    private final AtomicBoolean startBegin = new AtomicBoolean(false);
    private final AtomicBoolean startDone = new AtomicBoolean(false);
    private final AtomicBoolean shutdownBegin = new AtomicBoolean(false);
    private final AtomicBoolean shutdownDone = new AtomicBoolean(false);
    
    private final CountDownLatch startLatch = new CountDownLatch(1);
    
    private static enum State { NONE, STARTING, STARTED, STOPPED };

    private final Provider<IPFilter> ipFilter;
    private final Provider<SimppManager> simppManager;
    private final Provider<Acceptor> acceptor;
    private final Provider<ActivityCallback> activityCallback;
    private final Provider<ContentManager> contentManager;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<UploadManager> uploadManager;
    private final Provider<HTTPAcceptor> httpUploadAcceptor;
    private final Provider<StaticMessages> staticMessages;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<DownloadManager> downloadManager;
    private final Provider<PushDownloadManager> pushDownloadManager;
    private final Provider<NodeAssigner> nodeAssigner;
    private final Provider<HostCatcher> hostCatcher;
    private final Provider<FileManager> fileManager;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<ConnectionDispatcher> connectionDispatcher;
    private final Provider<UpdateHandler> updateHandler;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final Provider<LocalHTTPAcceptor> localHttpAcceptor;
    private final Provider<LocalAcceptor> localAcceptor;
    private final Provider<Pinger> pinger;
    private final Provider<ConnectionWatchdog> connectionWatchdog;
    private final Provider<SavedFileManager> savedFileManager;
    private final Provider<RatingTable> ratingTable;
    private final Provider<ChatManager> chatManager;
    private final Provider<UDPMultiplexor> udpMultiplexor;
    private final Provider<HashTreeCache> tigerTreeCache;
    private final Provider<DHTManager> dhtManager;
    private final Provider<ByteBufferCache> byteBufferCache;
    private final Provider<NetworkManager> networkManager;
    private final Provider<Statistics> statistics;
    private final Provider<ConnectionServices> connectionServices;
    private final Provider<SpamServices> spamServices;
    private final Provider<ControlRequestAcceptor> controlRequestAcceptor;
    private final Provider<LimeCoreGlue> limeCoreGlue;
    private final Provider<LWSManager> lwsManager;
    private final Provider<LWSIntegrationServices> lwsItegrationServices;
    private final Provider<OutOfBandThroughputMeasurer> outOfBandThroughputMeasurer;
    private final Provider<BrowseHostHandlerManager> browseHostHandlerManager;
    private final Provider<DownloadUpgradeTask> downloadUpgradeTask;
    private final Provider<StatisticAccumulator> statisticAccumulator;
    
    /** A list of items that require running prior to shutting down LW. */
    private final List<Thread> SHUTDOWN_ITEMS =  Collections.synchronizedList(new LinkedList<Thread>());
    /** The time when this finished starting. */
    @InspectablePrimitive("time lifecycle finished starting") 
    private long startFinishedTime;


    private final Provider<LicenseFactory> licenseFactory;
    private final Provider<ConnectionDispatcher> localConnectionDispatcher;
    private final Provider<DownloaderGuidAlternateLocationFinder> magnetDownloaderPushEndpointFinder;
    private final Provider<PushProxiesPublisher> pushProxiesPublisher;    
    private final Provider<DHTPeerLocator> dhtPeerLocator;    
    private final Provider<DHTPeerPublisher> dhtPeerPublisher;
    private final Provider<PromotionServices> promotionServices;

    @Inject
    public LifecycleManagerImpl(
             Provider<IPFilter> ipFilter,
            Provider<SimppManager> simppManager, Provider<Acceptor> acceptor,
            Provider<ActivityCallback> activityCallback,
            Provider<ContentManager> contentManager,
            Provider<MessageRouter> messageRouter,
            Provider<UploadManager> uploadManager,
            Provider<HTTPAcceptor> httpUploadAcceptor,
            Provider<StaticMessages> staticMessages,
            Provider<ConnectionManager> connectionManager,
            Provider<DownloadManager> downloadManager,
            Provider<PushDownloadManager> pushDownloadManager,
            Provider<NodeAssigner> nodeAssigner,
            Provider<HostCatcher> hostCatcher,
            Provider<FileManager> fileManager,
            Provider<TorrentManager> torrentManager,
            @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            @Named("local") Provider<ConnectionDispatcher> localConnectionDispatcher,
            Provider<UpdateHandler> updateHandler,
            Provider<QueryUnicaster> queryUnicaster,
            Provider<LocalHTTPAcceptor> localHttpAcceptor,
            Provider<LocalAcceptor> localAcceptor,
            Provider<Pinger> pinger,
            Provider<ConnectionWatchdog> connectionWatchdog,
            Provider<SavedFileManager> savedFileManager,
            Provider<RatingTable> ratingTable,
            Provider<ChatManager> chatManager,
            Provider<UDPMultiplexor> udpMultiplexor,
            Provider<HashTreeCache> tigerTreeCache,
            Provider<DHTManager> dhtManager,
            Provider<ByteBufferCache> byteBufferCache,
            @Named("backgroundExecutor") Provider<ScheduledExecutorService> backgroundExecutor,
            Provider<NetworkManager> networkManager,
            Provider<Statistics> statistics,
            Provider<ConnectionServices> connectionServices,
            Provider<SpamServices> spamServices,
            Provider<ControlRequestAcceptor> controlRequestAcceptor,
            Provider<IncomingConnectionHandler> incomingConnectionHandler,
            Provider<LicenseFactory> licenseFactory,
            Provider<LimeCoreGlue> limeCoreGlue,
            Provider<LWSManager> lwsManager,
            Provider<LWSIntegrationServices> lwsItegrationServices,
            Provider<OutOfBandThroughputMeasurer> outOfBandThroughputMeasurer,
            Provider<BrowseHostHandlerManager> browseHostHandlerManager,
            Provider<DownloadUpgradeTask> downloadUpgradeTask,
            Provider<StatisticAccumulator> statisticAccumulator,
            Provider<DownloaderGuidAlternateLocationFinder> magnetDownloaderPushEndpointFinder,
            Provider<PushProxiesPublisher> pushProxiesPublisher,
            Provider<DHTPeerLocator> dhtPeerLocator,
            Provider<DHTPeerPublisher> dhtPeerPublisher,
            Provider<PromotionServices> promotionServices) { 
        this.ipFilter = ipFilter;
        this.simppManager = simppManager;
        this.acceptor = acceptor;
        this.activityCallback = activityCallback;
        this.contentManager = contentManager;
        this.messageRouter = messageRouter;
        this.uploadManager = uploadManager;
        this.httpUploadAcceptor = httpUploadAcceptor;
        this.staticMessages = staticMessages;
        this.connectionManager = connectionManager;
        this.downloadManager = downloadManager;
        this.pushDownloadManager = pushDownloadManager;
        this.nodeAssigner = nodeAssigner;
        this.hostCatcher = hostCatcher;
        this.fileManager = fileManager;
        this.torrentManager = torrentManager;
        this.connectionDispatcher = connectionDispatcher;
        this.localConnectionDispatcher = localConnectionDispatcher;
        this.updateHandler = updateHandler;
        this.queryUnicaster = queryUnicaster;
        this.localHttpAcceptor = localHttpAcceptor;
        this.localAcceptor = localAcceptor;
        this.pinger = pinger;
        this.connectionWatchdog = connectionWatchdog;
        this.savedFileManager = savedFileManager;
        this.ratingTable = ratingTable;
        this.chatManager = chatManager;
        this.udpMultiplexor = udpMultiplexor;
        this.tigerTreeCache = tigerTreeCache;
        this.dhtManager = dhtManager;
        this.byteBufferCache = byteBufferCache;
        this.networkManager = networkManager;
        this.statistics = statistics;
        this.connectionServices = connectionServices;
        this.spamServices = spamServices;
        this.controlRequestAcceptor = controlRequestAcceptor;
        this.licenseFactory = licenseFactory;
        this.limeCoreGlue = limeCoreGlue;
        this.lwsManager = lwsManager;
        this.lwsItegrationServices = lwsItegrationServices;
        this.outOfBandThroughputMeasurer = outOfBandThroughputMeasurer;
        this.browseHostHandlerManager = browseHostHandlerManager;
        this.downloadUpgradeTask = downloadUpgradeTask;
        this.statisticAccumulator = statisticAccumulator;
        this.magnetDownloaderPushEndpointFinder = magnetDownloaderPushEndpointFinder;
        this.pushProxiesPublisher = pushProxiesPublisher;
        this.dhtPeerLocator = dhtPeerLocator;
        this.dhtPeerPublisher = dhtPeerPublisher;
        this.promotionServices = promotionServices;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isLoaded()
     */
    public boolean isLoaded() {
        State state = getCurrentState();
        return state == State.STARTED || state == State.STARTING;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isStarted()
     */
    public boolean isStarted() {
        State state = getCurrentState();
        return state == State.STARTED || state == State.STOPPED;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#isShutdown()
     */
    public boolean isShutdown() {
        return getCurrentState() == State.STOPPED;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#installListeners()
     */
    public void installListeners() {
        if(preinitializeBegin.getAndSet(true))
            return;
        
        LimeCoreGlue.preinstall();
        
        fileManager.get().addFileEventListener(activityCallback.get());
        //allow incoming RUDP messages to be forwarded correctly.
        LimeRUDPMessageHandler handler = new LimeRUDPMessageHandler(udpMultiplexor.get());
        handler.install(messageRouter.get());      
        
        connectionManager.get().addEventListener(activityCallback.get());
        connectionManager.get().addEventListener(dhtManager.get());
        LOG.debug("Installing push proxies publisher");
        dhtManager.get().addEventListener(pushProxiesPublisher.get());
        
        preinitializeDone.set(true);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#loadBackgroundTasks()
     */
    public void loadBackgroundTasks() {
        if(backgroundBegin.getAndSet(true))
            return;

        installListeners();
        
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                doBackgroundTasks();
            }
        }, "BackgroundTasks");
    }
    
    private void loadBackgroundTasksBlocking() {
        if(backgroundBegin.getAndSet(true))
            return;

        installListeners();
        
        doBackgroundTasks();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#start()
     */
    public void start() {
        if(startBegin.getAndSet(true))
            return;
        
        try {
            doStart();
        } finally {
            startLatch.countDown();
        }
    }
    
    /** Details about the shutdown */
    @SuppressWarnings("unused")
    @InspectionPoint("ungraceful shutdown details")
    private final Inspectable ungracefulShutdownDetails = new Inspectable() {
        public Object inspect() {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("JVM version", VersionUtils.getJavaVersion());
            data.put("OS", OSUtils.getOS());
            data.put("OS version", OSUtils.getOSVersion());
            data.put("graceful shutdown", ApplicationSettings.PREVIOUS_SHUTDOWN_WAS_GRACEFUL.getValue());
            return data;
        }
    };
    
    /*
     * Package private only for testing purposes, no methods outside this 
     * class should try to access this method.
     */
    void doStart() {                                
        //if previous instance of LimeWire was shutdown properly
        if(ApplicationSettings.CURRENTLY_RUNNING.getValue()) {
            ApplicationSettings.PREVIOUS_SHUTDOWN_WAS_GRACEFUL.setValue(false);             
        } else {
            ApplicationSettings.PREVIOUS_SHUTDOWN_WAS_GRACEFUL.setValue(true); 
        }
        //sets that an instance of LimeWire is running.
        ApplicationSettings.CURRENTLY_RUNNING.setValue(true);        
        
        // save limewire.props & other settings
        SettingsGroupManager.instance().save();
        
        loadBackgroundTasksBlocking();
        
	    LOG.trace("START RouterService");
	    
	    statisticAccumulator.get().start();

        if(SSLSettings.isIncomingTLSEnabled() || SSLSettings.isOutgoingTLSEnabled()) {
            LOG.trace("START SSL Test");
            activityCallback.get().componentLoading(I18nMarker.marktr("Loading TLS Encryption..."));
            SSLEngineTest sslTester = new SSLEngineTest(SSLUtils.getTLSContext(), SSLUtils.getTLSCipherSuites(), byteBufferCache.get());
            if(!sslTester.go()) {
                Throwable t = sslTester.getLastFailureCause();
                SSLSettings.disableTLS(t);
                if(!SSLSettings.IGNORE_SSL_EXCEPTIONS.getValue() && !sslTester.isIgnorable(t))
                    ErrorService.error(t);
            }
            LOG.trace("END SSL Test");
        }
		// Now, link all the pieces together, starting the various threads.            
        LOG.trace("START ContentManager");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Safe Content Management..."));
        contentManager.get().start();
        LOG.trace("STOP ContentManager");

        LOG.trace("START MessageRouter");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Message Router..."));
		messageRouter.get().start();
		LOG.trace("STOPMessageRouter");
		
        LOG.trace("START UpdateManager.instance");
        activityCallback.get().componentLoading(I18nMarker.marktr("Checking for Updates..."));
        updateHandler.get().initialize();
        LOG.trace("STOP UpdateManager.instance");

        LOG.trace("START HTTPUploadManager");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Upload Management..."));
        uploadManager.get().start(); 
        LOG.trace("STOP HTTPUploadManager");

        LOG.trace("START HTTPUploadAcceptor");
        httpUploadAcceptor.get().start(); 
        connectionDispatcher.get().addConnectionAcceptor(httpUploadAcceptor.get(), false, httpUploadAcceptor.get().getHttpMethods());
        LOG.trace("STOP HTTPUploadAcceptor");

        LOG.trace("START NetworkManager");
        networkManager.get().start();
        LOG.trace("STOP NetworkManager");
        
        LOG.trace("START Acceptor");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Connection Listener..."));
		acceptor.get().start();
		LOG.trace("STOP Acceptor");
		
        LOG.trace("START loading StaticMessages");
        staticMessages.get().initialize();
        LOG.trace("END loading StaticMessages");
        
		LOG.trace("START ConnectionManager");
		activityCallback.get().componentLoading(I18nMarker.marktr("Loading Connection Management..."));
        connectionManager.get().initialize();
		LOG.trace("STOP ConnectionManager");
		
		LOG.trace("Running download upgrade task");
		downloadUpgradeTask.get().upgrade();
		LOG.trace("Download upgrade task run!");

		LOG.trace("START DownloadManager");
		activityCallback.get().componentLoading(I18nMarker.marktr("Loading Download Management..."));
		downloadManager.get().initialize();
        connectionDispatcher.get().addConnectionAcceptor(pushDownloadManager.get(), false, "GIV");
		LOG.trace("STOP DownloadManager");
		
		LOG.trace("START NodeAssigner");
		activityCallback.get().componentLoading(I18nMarker.marktr("Loading Ultrapeer/DHT Management..."));
		nodeAssigner.get().start();
		LOG.trace("STOP NodeAssigner");
		
        LOG.trace("START HostCatcher.initialize");
        activityCallback.get().componentLoading(I18nMarker.marktr("Locating Peers..."));
		hostCatcher.get().initialize();
		LOG.trace("STOP HostCatcher.initialize");

		if(ConnectionSettings.CONNECT_ON_STARTUP.getValue()) {
			// Make sure connections come up ultra-fast (beyond default keepAlive)		
			int outgoing = ConnectionSettings.NUM_CONNECTIONS.getValue();
			if ( outgoing > 0 ) {
			    LOG.trace("START connect");
				connectionServices.get().connect();
                LOG.trace("STOP connect");
            }
		}
        // Asynchronously load files now that the GUI is up, notifying
        // callback.
        LOG.trace("START FileManager");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Shared Files..."));
        fileManager.get().start();
        LOG.trace("STOP FileManager");

        LOG.trace("START TorrentManager");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading BitTorrent Management..."));
		torrentManager.get().initialize(connectionDispatcher.get());
		LOG.trace("STOP TorrentManager");
        
		// add listener before downloads are read to get all add events
		downloadManager.get().addListener(magnetDownloaderPushEndpointFinder.get());
		
		// initialize DHTPeerLocator and DHTPeerPublishers
		LOG.trace("START DHTPeerLocator and DHTPeerPublisher");
        dhtPeerLocator.get().init();
        dhtPeerPublisher.get().init();
        LOG.trace("STOP DHTPeerLocator and DHTPeerPublisher");
		
        // Restore any downloads in progress.
        LOG.trace("START DownloadManager.postGuiInit");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Old Downloads..."));
        downloadManager.get().loadSavedDownloadsAndScheduleWriting();
        LOG.trace("STOP DownloadManager.postGuiInit");
        
        LOG.trace("START LWSIntegrationServices.postGuiInit");
        activityCallback.get().componentLoading(I18nMarker.marktr("Attaching LimeWire Store Listeners..."));
        lwsItegrationServices.get().init();
        LOG.trace("STOP LWSIntegrationServices.postGuiInit");
        
        LOG.trace("START QueryUnicaster");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Directed Querier..."));
		queryUnicaster.get().start();
		LOG.trace("STOP QueryUnicaster");
		
		LOG.trace("START LocalHTTPAcceptor");
		activityCallback.get().componentLoading(I18nMarker.marktr("Loading Magnet Listener..."));
        localHttpAcceptor.get().start();
        localConnectionDispatcher.get().addConnectionAcceptor(localHttpAcceptor.get(), true, localHttpAcceptor.get().getHttpMethods());
        LOG.trace("STOP LocalHTTPAcceptor");

        LOG.trace("START LocalAcceptor");
        initializeLocalConnectionDispatcher();
        localAcceptor.get().start();
        LOG.trace("STOP LocalAcceptor");
        
        LOG.trace("START Pinger");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Peer Listener..."));
        pinger.get().start();
        LOG.trace("STOP Pinger");
        
        LOG.trace("START ConnectionWatchdog");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Stale Connection Management..."));
        connectionWatchdog.get().start();
        LOG.trace("STOP ConnectionWatchdog");
        
        LOG.trace("START SavedFileManager");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Saved Files..."));
        savedFileManager.get();
        LOG.trace("STOP SavedFileManager");
		
		LOG.trace("START loading spam data");
		activityCallback.get().componentLoading(I18nMarker.marktr("Loading Spam Management..."));
		ratingTable.get();
		LOG.trace("START loading spam data");
        
        LOG.trace("START register connection dispatchers");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Network Listeners..."));
        initializeConnectionDispatcher();
        LOG.trace("STOP register connection dispatchers");
        
        LOG.trace("START Random Initializings");
        outOfBandThroughputMeasurer.get().initialize();
        browseHostHandlerManager.get().initialize();
        LOG.trace("STOP Random Initializings");

        // Allow us to disable this remotely is needed
        if (LWSSettings.LWS_IS_ENABLED.getValue()) {
            LOG.trace("START StoreServer");
            localHttpAcceptor.get().registerHandler("/" + LWSManager.PREFIX + "*",  lwsManager.get().getHandler());
            LOG.trace("END StoreServer");
        } else {
            LOG.trace("Disabling the StoreServer");
        }
        
        LOG.trace("START loading promotion system");
        activityCallback.get().componentLoading(I18nMarker.marktr("Loading Promotion System..."));
        promotionServices.get().init();

        if(ApplicationSettings.AUTOMATIC_MANUAL_GC.getValue())
            startManualGCThread();
        
        LOG.trace("STOP RouterService.");
        startDone.set(true);
        startFinishedTime = System.currentTimeMillis();
    }
    
    private void initializeConnectionDispatcher() {
        connectionDispatcher.get().addConnectionAcceptor(controlRequestAcceptor.get(),
                true, "MAGNET","TORRENT");
        connectionDispatcher.get().addConnectionAcceptor(chatManager.get(), false, "CHAT");
    }

    private void initializeLocalConnectionDispatcher() {
        localConnectionDispatcher.get().addConnectionAcceptor(controlRequestAcceptor.get(),
                true, "MAGNET", "TORRENT");
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#shutdown()
     */
    public void shutdown() {
        try {
            doShutdown();
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }
    
    private void doShutdown() {
        if(!startBegin.get() || shutdownBegin.getAndSet(true))
            return;
        
        try {
            // TODO: should we have a time limit on how long we wait?
            startLatch.await(); // wait for starting to finish...
        } catch(InterruptedException ie) {
            LOG.error("Interrupted while waiting to finish starting", ie);
            return;
        }
        
        nodeAssigner.get().stop();

        dhtManager.get().stop();
        
        try {
            acceptor.get().setListeningPort(0);
        } catch (IOException e) {
            LOG.error("Error stopping acceptor", e);
        }
        acceptor.get().shutdown();
        
        //clean-up connections and record connection uptime for this session
        connectionManager.get().disconnect(false);
        
        //Update fractional uptime statistics (before writing limewire.props)
        statistics.get().shutdown();
        
        //records that LimeWire is shutting down properly
        ApplicationSettings.CURRENTLY_RUNNING.setValue(false);
        
		// start closing all active torrents
		// torrentManager.shutdown();
		
        //Update firewalled status
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(networkManager.get().acceptedIncomingConnection());

        //Write gnutella.net
        try {
            hostCatcher.get().write();
        } catch (IOException e) {
            LOG.error("Error saving host catcher file", e);   
        }
        
        // save limewire.props & other settings
        SettingsGroupManager.instance().save();
		
		ratingTable.get().ageAndSave();
        
        cleanupPreviewFiles();
        
        cleanupTorrentMetadataFiles();
        
        downloadManager.get().writeSnapshot();
        
        downloadManager.get().removeListener(magnetDownloaderPushEndpointFinder.get());
        
       // torrentManager.writeSnapshot();
        
        fileManager.get().stop(); // Saves UrnCache and CreationTimeCache

        tigerTreeCache.get().persistCache(fileManager.get(), downloadManager.get());

        licenseFactory.get().persistCache();
        
        contentManager.get().stop();
        
        messageRouter.get().stop();
        
        localAcceptor.get().stop();
        
        statisticAccumulator.get().stop();
        
        promotionServices.get().shutDown();
        
        runShutdownItems();
        
        shutdownDone.set(true);
    }

    
    private static String parseCommand(String toCall) {
        if (toCall.startsWith("\"")) {
            int end;
            if ((end = toCall.indexOf("\"", 1)) > -1) {
                return toCall.substring(0,end+1);
            }
            else {
                return toCall+"\"";
            }
        }
        int space;
        if ((space = toCall.indexOf(" ")) > -1) {
            return toCall.substring(0, space);
        }
        
        return toCall;
    }
    
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#shutdown(java.lang.String)
     */
    public void shutdown(String toExecute) {
        shutdown();
        if (toExecute != null) {
            try {
                if (OSUtils.isWindowsVista()) {
                    String cmd = parseCommand(toExecute).trim();
                    String params = toExecute.substring(cmd.length()).trim();
                    SystemUtils.openFile(cmd, params);
                }
                else {
                    Runtime.getRuntime().exec(toExecute);
                }
            } catch (IOException tooBad) {}
        }
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#getStartFinishedTime()
     */
    public long getStartFinishedTime() {
        return startFinishedTime;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.LifecycleManager#addShutdownItem(java.lang.Thread)
     */
    public boolean addShutdownItem(Thread t) {
        if(shutdownBegin.get())
            return false;
    
        SHUTDOWN_ITEMS.add(t);
        return true;
    }

    /** Runs all shutdown items. */
    private void runShutdownItems() {
        if(!shutdownBegin.get())
            return;
        
        // Start each shutdown item.
        for(Thread t : SHUTDOWN_ITEMS)
            t.start();
        
        // Now that we started them all, iterate back and wait for each one to finish.
        for(Thread t : SHUTDOWN_ITEMS) {
            try {
                t.join();
            } catch(InterruptedException ie) {}
        }
    }
    
    /** Runs all tasks that can be done in the background while the gui inits. */
    private void doBackgroundTasks() {
        limeCoreGlue.get().install(); // ensure glue is set before running tasks.
        
        //add more while-gui init tasks here
        ipFilter.get().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                spamServices.get().adjustSpamFilters();
            }
        });
        simppManager.get().addListener(new SimppListener() {
            public void simppUpdated(int newVersion) {
                spamServices.get().reloadIPFilter();
            }
        });
        acceptor.get().init();
        backgroundDone.set(true);
    }

    /** Gets the current state of the lifecycle. */
    private State getCurrentState() {
        if(shutdownBegin.get())
            return State.STOPPED;
        else if(startDone.get())
            return State.STARTED;
        else if(startBegin.get())
            return State.STARTING;
        else
            return State.NONE;
    }

    /** Starts a manual GC thread. */
    private void startManualGCThread() {
        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(5 * 60 * 1000);
                    } catch(InterruptedException ignored) {}
                    LOG.trace("Running GC");
                    System.gc();
                    LOG.trace("GC finished, running finalizers");
                    System.runFinalization();
                    LOG.trace("Finalizers finished.");
                }
            }
        }, "ManualGC");
        t.setDaemon(true);
        t.start();
        LOG.trace("Started manual GC thread.");
    }

    private void cleanupTorrentMetadataFiles() {
        if(!fileManager.get().isLoadFinished()) {
            return;
        }
        
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                return "torrent".equals(FileUtils.getFileExtension(f));
            }
        };
        
        File[] file_list = SharingUtils.APPLICATION_SPECIAL_SHARE.listFiles(filter);
        if(file_list == null) {
            return;
        }
        long purgeLimit = System.currentTimeMillis() 
            - SharingSettings.TORRENT_METADATA_PURGE_TIME.getValue()*24L*60L*60L*1000L;
        File tFile;
        for(int i = 0; i < file_list.length; i++) {
            tFile = file_list[i];
            if(!fileManager.get().isFileShared(tFile) &&
                    tFile.lastModified() < purgeLimit) {
                tFile.delete();
            }
        }
    }

    /** Deletes all preview files. */
    private void cleanupPreviewFiles() {
        //Cleanup any preview files.  Note that these will not be deleted if
        //your previewer is still open.
        File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
        if (incompleteDir == null)
            return; // if we could not get the incomplete directory, simply return.
        
        
        File[] files = incompleteDir.listFiles();
        if(files == null)
            return;
        
        for (int i=0; i<files.length; i++) {
            String name = files[i].getName();
            if (name.startsWith(IncompleteFileManager.PREVIEW_PREFIX))
                files[i].delete();  //May or may not work; ignore return code.
        }
    }

 

}
