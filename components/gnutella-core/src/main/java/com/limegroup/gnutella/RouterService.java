package com.limegroup.gnutella;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectableChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.ssl.SSLEngineTest;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.rudp.DefaultUDPSelectorProviderFactory;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.rudp.UDPSelectorProviderFactory;
import org.limewire.service.ErrorService;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.browser.ControlRequestAcceptor;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.http.HTTPConnectionData;
import com.limegroup.gnutella.http.HttpClientManager;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.rudp.LimeRUDPContext;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageHandler;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.settings.SSLSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.SocketsManager.ConnectType;


/**
 * A facade for the entire LimeWire backend.  This is the GUI's primary way of
 * communicating with the backend.  RouterService constructs the backend 
 * components.  Typical use is as follows:
 *
 * <pre>
 * RouterService rs = new RouterService(ActivityCallback);
 * rs.start();
 * rs.query(...);
 * rs.download(...);
 * rs.shutdown();
 * </pre>
 *
 * The methods of this class are numerous, but they tend to fall into one of the
 * following categories:
 *
 * <ul> 
 * <li><b>Connecting and disconnecting</b>: connect, disconnect,
 *     connectToHostBlocking, connectToHostAsynchronously, 
 *     connectToGroup, removeConnection, getNumConnections
 * <li><b>Searching and downloading</b>: query, browse, score, matchesType,
 *     isMandragoreWorm, download
 * <li><b>Notification of SettingsManager changes</b>:
 *     setKeepAlive, setListeningPort, adjustSpamFilters, refreshBannedIPs
 * <li><b>HostCatcher and horizon</b>: clearHostCatcher, getHosts, removeHost,
 *     getNumHosts, getNumFiles, getTotalFileSize, setAlwaysNotifyKnownHost,
 *     updateHorizon.  <i>(HostCatcher has changed dramatically on
 *     pong-caching-branch and query-routing3-branch of CVS, so these methods
 *     will probably be obsolete in the future.)</i>
 * <li><b>Statistics</b>: getNumLocalSearches, getNumSharedFiles, 
 *      getTotalMessages, getTotalDroppedMessages, getTotalRouteErrors,
 *      getNumPendingShared
 * </ul> 
 */
public class RouterService {
    
    /////-------------------------
    // DPINJ: TODOS!!!! ----------
    //  UDPService was always init'd (which internally scheduled) immediately when RS was touched.
    //     It now is constructed lazily when needed.
    //     Check if this is a problem, and if so, fix it.
    
    private static final Log LOG = LogFactory.getLog(RouterService.class);
    
    static {
        LimeCoreGlue.preinstall();
    }
        
    /**
     * isShuttingDown flag
     */
    private static boolean isShuttingDown;

	/**
	 * Variable for the <tt>ActivityCallback</tt> instance.
	 */
    private static ActivityCallback callback;
    
    /**
     * The UDPMultiplexor.
     */
    private static UDPMultiplexor UDP_MULTIPLEXOR;
    
    /**
     * Selector provider for UDP selectors.
     */
    private static UDPSelectorProvider UDP_SELECTOR_PROVIDER;
           
    static {
        // Link the multiplexor & NIODispatcher together.
        UDPSelectorProviderFactory factory = new DefaultUDPSelectorProviderFactory(new LimeRUDPContext());
        UDPSelectorProvider.setDefaultProviderFactory(factory);
        UDP_SELECTOR_PROVIDER = UDPSelectorProvider.defaultProvider();
        UDP_MULTIPLEXOR = UDP_SELECTOR_PROVIDER.openSelector();
        SelectableChannel socketChannel = UDP_SELECTOR_PROVIDER.openSocketChannel();
        try {
            socketChannel.close();
        } catch(IOException ignored) {}
        NIODispatcher.instance().registerSelector(UDP_MULTIPLEXOR, socketChannel.getClass());
    }
    
    /**
     * A list of items that require running prior to shutting down LW.
     */
    private static final List<Thread> SHUTDOWN_ITEMS = 
        Collections.synchronizedList(new LinkedList<Thread>());

    /**
     * Variable for whether or not that backend threads have been started.
     * 0 - nothing started
     * 1 - pre/while gui tasks started
     * 2 - everything started
     * 3 - shutting down
     * 4 - shut down
     */
    private static enum StartStatus {NOTHING, PRE_GUI, STARTING, STARTED, SHUTTING, SHUT};
    private static volatile StartStatus _state = StartStatus.NOTHING;


	
	/**
	 * Whether or not we are running at full power.
	 */
    @InspectablePrimitive
	private static boolean _fullPower = true;
    
    /** The time when this finished starting. */
    @InspectablePrimitive
    public static long startTime;
	
	private static final byte [] MYGUID, MYBTGUID;
	static {
	    byte [] myguid=null;
	    try {
	        myguid = GUID.fromHexString(ApplicationSettings.CLIENT_ID.getValue());
	    }catch(IllegalArgumentException iae) {
	        myguid = GUID.makeGuid();
	        ApplicationSettings.CLIENT_ID.setValue((new GUID(myguid)).toHexString());
	    }
	    MYGUID=myguid;
	    
	    byte []mybtguid = new byte[20];
	    mybtguid[0] = 0x2D; // - 
	    mybtguid[1] = 0x4C; // L
	    mybtguid[2] = 0x57; // W
	    System.arraycopy(LimeWireUtils.BT_REVISION.getBytes(),0, mybtguid,3, 4);
        mybtguid[7] = 0x2D; // -
	    System.arraycopy(MYGUID,0,mybtguid,8,12);
	    MYBTGUID = mybtguid;
	}

	/**
	 * Creates a new <tt>RouterService</tt> instance.  This fully constructs 
	 * the backend.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use for
	 *  making callbacks
	 */
  	public RouterService(ActivityCallback callback) {
        this(callback, ProviderHacks.getMessageRouter());
    }

	/**
	 * Creates a new <tt>RouterService</tt> instance with special message
     * handling code.  Typically this constructor is only used for testing.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use for
	 *  making callbacks
     * @param router the <tt>MessageRouter</tt> instance to use for handling
     *  all messages
	 */
  	public RouterService(ActivityCallback callback, MessageRouter router) {
		RouterService.callback = callback;
        ProviderHacks.getFileManager().addFileEventListener(callback);
        RouterService.setMessageRouter(router);
        
        ProviderHacks.getConnectionManager().addEventListener(callback);
        ProviderHacks.getConnectionManager().addEventListener(ProviderHacks.getDHTManager());
        
  	}

    public static void setMessageRouter(MessageRouter messageRouter) {
        if(messageRouter != ProviderHacks.getMessageRouter())
            throw new IllegalStateException("fix your test!");
        
        //allow incoming RUDP messages to be forwarded correctly.
        LimeRUDPMessageHandler handler = new LimeRUDPMessageHandler(getUDPMultiplexor());
        handler.install(messageRouter);        
    }
    
    /**
  	 * Performs startup tasks that should happen while the GUI loads
  	 */
  	public static void asyncGuiInit() {
  		
  		synchronized(RouterService.class) {
  			if (_state != StartStatus.NOTHING) // already did this?
  				return;
  			else
  				_state = StartStatus.PRE_GUI;
  		}
  		
        ThreadExecutor.startThread(new Initializer(), "async gui initializer");
  	}
  	
  	/**
  	 * performs the tasks usually run while the gui is initializing synchronously
  	 * to be used for tests and when running only the core
  	 */
  	public void preGuiInit() {
  		
  		synchronized(RouterService.class) {
  			if (_state != StartStatus.NOTHING) // already did this?
  				return;
  			else
  				_state = StartStatus.PRE_GUI;
  		}
  		
  	    (new Initializer()).run();
  	}
  	
  	private static class Initializer implements Runnable {
  	    public void run() {
  	        //add more while-gui init tasks here
            ProviderHacks.getIpFilter().refreshHosts(new IPFilter.IPFilterCallback() {
                public void ipFiltersLoaded() {
                    adjustSpamFilters();
                }
            });
            SimppManager.instance().addListener(new SimppListener() {
                public void simppUpdated(int newVersion) {
                    reloadIPFilter();
                }
            });
  	        ProviderHacks.getAcceptor().init();
  	    }
  	}
  	
	/**
	 * Starts various threads and tasks once all core classes have
	 * been constructed.
	 */
	public void start() {
	    synchronized(RouterService.class) {
    	    LOG.trace("START RouterService");
    	    
    	    if ( isLoaded() )
    	        return;
    	        
            LimeCoreGlue.install();
            preGuiInit();
            _state = StartStatus.STARTING;
            
            HttpClientManager.initialize();
            
            LOG.trace("START SSL Test");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_SSL_TEST"));
            SSLEngineTest sslTester = new SSLEngineTest(SSLUtils.getTLSContext(), SSLUtils.getTLSCipherSuites(), NIODispatcher.instance().getBufferCache());
            if(!sslTester.go()) {
                Throwable t = sslTester.getLastFailureCause();
                SSLSettings.disableTLS(t);
                if(!SSLSettings.IGNORE_SSL_EXCEPTIONS.getValue() && !sslTester.isIgnorable(t))
                    ErrorService.error(t);
            }
            LOG.trace("END SSL Test");
    
    		// Now, link all the pieces together, starting the various threads.            
            LOG.trace("START ContentManager");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_CONTENT_MANAGER"));
            ProviderHacks.getContentManager().initialize();
            LOG.trace("STOP ContentManager");

            LOG.trace("START MessageRouter");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_MESSAGE_ROUTER"));
    		ProviderHacks.getMessageRouter().initialize();
    		LOG.trace("STOPMessageRouter");

            LOG.trace("START HTTPUploadManager");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_UPLOAD_MANAGER"));
            ProviderHacks.getUploadManager().start(ProviderHacks.getHTTPUploadAcceptor(), ProviderHacks.getFileManager(), callback, ProviderHacks.getMessageRouter()); 
            LOG.trace("STOP HTTPUploadManager");

            LOG.trace("START HTTPUploadAcceptor");
            ProviderHacks.getHTTPUploadAcceptor().start(ProviderHacks.getConnectionDispatcher()); 
            LOG.trace("STOP HTTPUploadAcceptor");

            LOG.trace("START Acceptor");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_ACCEPTOR"));
    		ProviderHacks.getAcceptor().start();
    		LOG.trace("STOP Acceptor");
    		
            LOG.trace("START loading StaticMessages");
            ProviderHacks.getStaticMessages().initialize();
            LOG.trace("END loading StaticMessages");
            
    		LOG.trace("START ConnectionManager");
    		callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_CONNECTION_MANAGER"));
            ProviderHacks.getConnectionManager().initialize();
    		LOG.trace("STOP ConnectionManager");
    		
    		LOG.trace("START DownloadManager");
    		callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_DOWNLOAD_MANAGER"));
    		ProviderHacks.getDownloadManager().initialize(); 
    		LOG.trace("STOP DownloadManager");
    		
    		LOG.trace("START NodeAssigner");
    		callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_NODE_ASSIGNER"));
    		ProviderHacks.getNodeAssigner().start();
    		LOG.trace("STOP NodeAssigner");
			
            LOG.trace("START HostCatcher.initialize");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_HOST_CATCHER"));
    		ProviderHacks.getHostCatcher().initialize();
    		LOG.trace("STOP HostCatcher.initialize");
    
    		if(ConnectionSettings.CONNECT_ON_STARTUP.getValue()) {
    			// Make sure connections come up ultra-fast (beyond default keepAlive)		
    			int outgoing = ConnectionSettings.NUM_CONNECTIONS.getValue();
    			if ( outgoing > 0 ) {
    			    LOG.trace("START connect");
    				connect();
                    LOG.trace("STOP connect");
                }
    		}
            // Asynchronously load files now that the GUI is up, notifying
            // callback.
            LOG.trace("START FileManager");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_FILE_MANAGER"));
            ProviderHacks.getFileManager().start();
            LOG.trace("STOP FileManager");
    
            LOG.trace("START TorrentManager");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_TORRENT_MANAGER"));
			ProviderHacks.getTorrentManager().initialize(ProviderHacks.getFileManager(), ProviderHacks.getConnectionDispatcher(), SimpleTimer.sharedTimer());
			LOG.trace("STOP TorrentManager");
            
            LOG.trace("START ControlRequestAcceptor");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_CONTROL_REQUEST_ACCEPTOR"));
			(new ControlRequestAcceptor()).register(ProviderHacks.getConnectionDispatcher());
			LOG.trace("STOP ControlRequestAcceptor");
			
            // Restore any downloads in progress.
            LOG.trace("START DownloadManager.postGuiInit");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_DOWNLOAD_MANAGER_POST_GUI"));
            ProviderHacks.getDownloadManager().postGuiInit();
            LOG.trace("STOP DownloadManager.postGuiInit");
            
            LOG.trace("START UpdateManager.instance");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_UPDATE_MANAGER"));
            ProviderHacks.getUpdateHandler();
            LOG.trace("STOP UpdateManager.instance");

            LOG.trace("START QueryUnicaster");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_QUERY_UNICASTER"));
    		ProviderHacks.getQueryUnicaster().start();
    		LOG.trace("STOP QueryUnicaster");
    		
    		LOG.trace("START HTTPAcceptor");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_HTTPACCEPTOR"));
            ProviderHacks.getHTTPAcceptor().start();
            LOG.trace("STOP HTTPAcceptor");
            
            LOG.trace("START Pinger");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_PINGER"));
            ProviderHacks.getPinger().start();
            LOG.trace("STOP Pinger");
            
            LOG.trace("START ConnectionWatchdog");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_CONNECTION_WATCHDOG"));
            ProviderHacks.getConnectionWatchdog().start();
            LOG.trace("STOP ConnectionWatchdog");
            
            LOG.trace("START SavedFileManager");
            callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_SAVED_FILE_MANAGER"));
            ProviderHacks.getSavedFileManager();
            LOG.trace("STOP SavedFileManager");
			
			LOG.trace("START loading spam data");
			callback.componentLoading(I18n.marktr("SPLASH_STATUS_COMPONENT_LOADING_SPAM"));
			ProviderHacks.getRatingTable();
			LOG.trace("START loading spam data");
            
            LOG.trace("START ChatManager");
            ProviderHacks.getChatManager().initialize();
            LOG.trace("END ChatManager");

            if(ApplicationSettings.AUTOMATIC_MANUAL_GC.getValue())
                startManualGCThread();
            
            LOG.trace("STOP RouterService.");
            _state = StartStatus.STARTED;
            
            startTime = System.currentTimeMillis();
        }
	}
	
	/**
	 * Starts a manual GC thread.
	 */
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
	                
	/**
     *  Returns the number of initialized messaging connections.
     */
    public static int getNumInitializedConnections() {
    	return ProviderHacks.getConnectionManager().getNumInitializedConnections();
    }

    /**
     * Returns whether there are any active internet (non-multicast) transfers
     * going at speed greater than 0.
     */
    public static boolean hasActiveUploads() {
        ProviderHacks.getUploadSlotManager().measureBandwidth();
        try {
            return ProviderHacks.getUploadSlotManager().getMeasuredBandwidth() > 0;
        } catch (InsufficientDataException ide) {
        }
        return false;
    }

    /**
     * @return the bandwidth for uploads in bytes per second
     */
    public static float getRequestedUploadSpeed() {
        // if the user chose not to limit his uploads
        // by setting the upload speed to unlimited
        // set the upload speed to 3.4E38 bytes per second.
        // This is de facto not limiting the uploads
        int uSpeed = UploadSettings.UPLOAD_SPEED.getValue();
        if (uSpeed == 100) {
            return Float.MAX_VALUE; 
        } else {
            // if the uploads are limited, take messageUpstream
            // for ultrapeers into account, - don't allow lower 
            // speeds than 1kb/s so uploads won't stall completely
            // if the user accidently sets his connection speed 
            // lower than his message upstream

            // connection speed is in kbits per second and upload speed is in percent
            float speed = ConnectionSettings.CONNECTION_SPEED.getValue() / 8f * uSpeed / 100f;
            
            // reduced upload speed if we are an ultrapeer
            speed -= ProviderHacks.getConnectionManager().getMeasuredUpstreamBandwidth();
            
            // we need bytes per second
            return Math.max(speed, 1f) * 1024f;
        }
    }

    /**
     * Used to determine whether or not the backend threads have been
     * started.
     *
     * @return <tt>true</tt> if the backend threads have been started,
     *  otherwise <tt>false</tt>
     */
    public static boolean isLoaded() {
        return isStarted() || _state == StartStatus.STARTING; 
    }
    
    public static boolean isStarted() {
    	return _state == StartStatus.STARTED || _state == StartStatus.SHUTTING ||
        _state == StartStatus.SHUT;
    }

    /**
     * Returns the <tt>ActivityCallback</tt> passed to this' constructor.
	 *
	 * @return the <tt>ActivityCallback</tt> passed to this' constructor --
	 *  this is one of the few accessors that can be <tt>null</tt> -- this 
	 *  will be <tt>null</tt> in the case where the <tt>RouterService</tt>
	 *  has not been constructed
     */ 
    public static ActivityCallback getCallback() {
        return RouterService.callback;
    }
    
    /**
     * Sets full power mode.
     */
    public static void setFullPower(boolean newValue) {
        if(_fullPower != newValue) {
            _fullPower = newValue;
            // FIXME implement throttle switching for uploads and downloads
            // NormalUploadState.setThrottleSwitching(!newValue);
            // HTTPDownloader.setThrottleSwitching(!newValue);
        }
    }

    /**
	 * Push uploads from firewalled clients.
	 */
	public static void acceptUpload(Socket socket, HTTPConnectionData data) {
	    ProviderHacks.getHTTPUploadAcceptor().acceptConnection(socket, data);
	}

	/** Gets the UDP Multiplexor. */
    public static UDPMultiplexor getUDPMultiplexor() {
        return UDP_MULTIPLEXOR;
    }
    
    /** Gets the SelectorProvider for UDPChannels */
    public static UDPSelectorProvider getUDPSelectorProvider() {
    	return UDP_SELECTOR_PROVIDER;
    }
    
    public static byte [] getMyGUID() {
	    return MYGUID;
	}
	
	public static byte [] getMyBTGUID() {
		return MYBTGUID;
	}

    /**
     * Schedules the given task for repeated fixed-delay execution on this's
     * backend thread.  <b>The task must not block for too long</b>, as 
     * a single thread is shared among all the backend.
     *
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     * @exception IllegalStateException this is cancelled
     * @exception IllegalArgumentException delay or period negative
     * @see org.limewire.concurrent.SimpleTimer#schedule(java.lang.Runnable,long,long)
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, long period) {
        return SimpleTimer.sharedTimer().scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS);
    }
    
    /**
     * @return an object that can be used as a <tt>getScheduledExecutorService</tt>
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
    	return SimpleTimer.sharedTimer();
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port.
     * Blocks until the connection established.  Throws IOException if
     * the connection failed.
     * @return a connection to the request host
     * @exception IOException the connection failed
     */
    public static ManagedConnection connectToHostBlocking(String hostname, int portnum, ConnectType type)
		throws IOException {
        return ProviderHacks.getConnectionManager().createConnectionBlocking(hostname, portnum, type);
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port. 
     * Returns immediately without blocking.  If hostname would connect
     * us to ourselves, returns immediately.
     */
    public static void connectToHostAsynchronously(String hostname, int portnum, ConnectType type) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.0.0.1" since
        //they are aliases for this machine.
		
        byte[] cIP = null;
        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
            cIP = addr.getAddress();
        } catch(UnknownHostException e) {
            return;
        }
        if ((cIP[0] == 127) && (portnum==ProviderHacks.getAcceptor().getPort(true)) &&
			ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
			return;
        } else {
            byte[] managerIP=ProviderHacks.getAcceptor().getAddress(true);
            if (Arrays.equals(cIP, managerIP)
                && portnum==ProviderHacks.getAcceptor().getPort(true))
                return;
        }

        if (!ProviderHacks.getAcceptor().isBannedIP(cIP)) {
            ProviderHacks.getConnectionManager().createConnectionAsynchronously(hostname, portnum, type);
		}
    }
    
    /**
     * Determines if you're connected to the given host.
     */
    public static boolean isConnectedTo(InetAddress addr) {
        // ideally we would check download sockets too, but
        // because of the way ManagedDownloader is built, it isn't
        // too practical.
        // TODO: rewrite ManagedDownloader
        
        String host = addr.getHostAddress();
        return ProviderHacks.getConnectionManager().isConnectedTo(host) ||
               UDP_MULTIPLEXOR.isConnectedTo(addr) ||
               ProviderHacks.getUploadManager().isConnectedTo(addr); // ||
               // dloadManager.isConnectedTo(addr);
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public static void connect() {
        adjustSpamFilters();
        
        //delegate to connection manager
        ProviderHacks.getConnectionManager().connect();
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public static void disconnect() {
		// Delegate to connection manager
        ProviderHacks.getConnectionManager().disconnect(false);
    }

    /**
     * Closes and removes the given connection.
     */
    public static void removeConnection(ManagedConnection c) {
        ProviderHacks.getConnectionManager().remove(c);
    }

    /**
     * Clears the hostcatcher.
     */
    public static void clearHostCatcher() {
        ProviderHacks.getHostCatcher().clear();
    }

    /**
     * Returns the number of downloads in progress.
     */
    public static int getNumDownloads() {
        return ProviderHacks.getDownloadManager().downloadsInProgress();
    }
    
    /**
     * Returns the number of active downloads.
     */
    public static int getNumActiveDownloads() {
        return ProviderHacks.getDownloadManager().getNumActiveDownloads();
    }
    
    /**
     * Returns the number of uploads in progress.
     */
    public static int getNumUploads() {
        return ProviderHacks.getUploadManager().uploadsInProgress() + ProviderHacks.getTorrentManager().getNumActiveTorrents();
    }

    /**
     * Returns the number of queued uploads.
     */
    public static int getNumQueuedUploads() {
        return ProviderHacks.getUploadManager().getNumQueuedUploads();
    }
    
    /**
     * Adds something that requires shutting down.
     *
     * TODO: Make this take a 'Service' or somesuch that
     *       has a shutdown method, and run the method in its
     *       own thread.
     */
    public static boolean addShutdownItem(Thread t) {
        if(isShuttingDown() || isShutdown())
            return false;

        SHUTDOWN_ITEMS.add(t);
        return true;
    }
    
    /**
     * Runs all shutdown items.
     */
    private static void runShutdownItems() {
        if(!isShuttingDown())
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
    
    /**
     * Determines if this is shutting down.
     */
    private static boolean isShuttingDown() {
        return _state == StartStatus.SHUTTING || _state == StartStatus.SHUT;
    }
    
    /**
     * Determines if this is shut down.
     */
    private static boolean isShutdown() {
        return _state == StartStatus.SHUT;
    }

    /**
     * Shuts down the backend and writes the gnutella.net file.
     *
     * TODO: Make all of these things Shutdown Items.
     */
    public static synchronized void shutdown() {
        try {
            if(!isLoaded())
                return;
                
            _state = StartStatus.SHUTTING;
            
            ProviderHacks.getNodeAssigner().stop();

            ProviderHacks.getDHTManager().stop();
            
            ProviderHacks.getAcceptor().shutdown();
            
            //clean-up connections and record connection uptime for this session
            ProviderHacks.getConnectionManager().disconnect(false);
            
            //Update fractional uptime statistics (before writing limewire.props)
            ProviderHacks.getStatistics().shutdown();
            
			// start closing all active torrents
			// torrentManager.shutdown();
			
            //Update firewalled status
            ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(ProviderHacks.getNetworkManager().acceptedIncomingConnection());

            //Write gnutella.net
            try {
                ProviderHacks.getHostCatcher().write();
            } catch (IOException e) {}
            
            // save limewire.props & other settings
            SettingsGroupManager.instance().save();
			
			ProviderHacks.getRatingTable().ageAndSave();
            
            cleanupPreviewFiles();
            
            cleanupTorrentMetadataFiles();
            
            ProviderHacks.getDownloadManager().writeSnapshot();
            
           // torrentManager.writeSnapshot();
            
            ProviderHacks.getFileManager().stop(); // Saves UrnCache and CreationTimeCache

            ProviderHacks.getTigerTreeCache().persistCache();

            LicenseFactory.persistCache();
            
            ProviderHacks.getContentManager().shutdown();
 
            
            runShutdownItems();
            
            _state = StartStatus.SHUT;
            
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }
    
    public static void shutdown(String toExecute) {
        shutdown();
        if (toExecute != null) {
            try {
                Runtime.getRuntime().exec(toExecute);
            } catch (IOException tooBad) {}
        }
    }
    
    /**
     * Deletes all preview files.
     */
    private static void cleanupPreviewFiles() {
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
    
    private static void cleanupTorrentMetadataFiles() {
        if(!ProviderHacks.getFileManager().isLoadFinished()) {
            return;
        }
        
        FileFilter filter = new FileFilter() {
            public boolean accept(File f) {
                return FileUtils.getFileExtension(f).equals("torrent");
            }
        };
        
        File[] file_list = FileManager.APPLICATION_SPECIAL_SHARE.listFiles(filter);
        if(file_list == null) {
            return;
        }
        long purgeLimit = System.currentTimeMillis() 
            - SharingSettings.TORRENT_METADATA_PURGE_TIME.getValue()*24L*60L*60L*1000L;
        File tFile;
        for(int i = 0; i < file_list.length; i++) {
            tFile = file_list[i];
            if(!ProviderHacks.getFileManager().isFileShared(tFile) &&
                    tFile.lastModified() < purgeLimit) {
                tFile.delete();
            }
        }
    }
    
    /**
     * Reloads the IP Filter data & adjusts spam filters when ready.
     */
    public static void reloadIPFilter() {
        ProviderHacks.getIpFilter().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                adjustSpamFilters();
            }
        });
        ProviderHacks.getHostileFilter().refreshHosts();
    }

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public static void adjustSpamFilters() {
        UDPReplyHandler.setPersonalFilter(SpamFilter.newPersonalFilter());
        
        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for(ManagedConnection c : ProviderHacks.getConnectionManager().getConnections()) {
            if(ProviderHacks.getIpFilter().allow(c)) {
                c.setPersonalFilter(SpamFilter.newPersonalFilter());
                c.setRouteFilter(SpamFilter.newRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }
        
        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }

    /**
     * Count up all the messages on active connections
     */
    public static int getActiveConnectionMessages() {
		return ProviderHacks.getConnectionManager().getActiveConnectionMessages();
    }

    /**
     * Count how many connections have already received N messages
     */
    public static int countConnectionsWithNMessages(int messageThreshold) {
		return ProviderHacks.getConnectionManager().countConnectionsWithNMessages(messageThreshold);
    }
    
    /** 
     * Returns a new GUID for passing to query.
     * This method is the central point of decision making for sending out OOB 
     * queries.
     */
    public static byte[] newQueryGUID() {
        byte []ret;
        // DPINJ: Use passed in NetworkManager!!!
        if (ProviderHacks.getNetworkManager().isOOBCapable() && OutOfBandThroughputStat.isOOBEffectiveForMe())
            ret = GUID.makeAddressEncodedGuid(ProviderHacks.getNetworkManager().getAddress(), ProviderHacks.getNetworkManager().getPort());
        else
            ret = GUID.makeGuid();
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(ret);
        return ret;
    }

    /**
     * Searches the network for files of the given type with the given
     * GUID, query string and minimum speed.  If type is null, any file type
     * is acceptable.<p>
     *
     * ActivityCallback is notified asynchronously of responses.  These
     * responses can be matched with requests by looking at their GUIDs.  (You
     * may want to wrap the bytes with a GUID object for simplicity.)  An
     * earlier version of this method returned the reply GUID instead of taking
     * it as an argument.  Unfortunately this caused a race condition where
     * replies were returned before the GUI was prepared to handle them.
     * 
     * @param guid the guid to use for the query.  MUST be a 16-byte
     *  value as returned by newQueryGUID.
     * @param query the query string to use
     * @param minSpeed the minimum desired result speed
     * @param type the desired type of result (e.g., audio, video), or
     *  null if you don't care 
     */
    public static void query(byte[] guid, String query, MediaType type) {
		query(guid, query, "", type);
	}

    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, MediaType)
     */
    public static void query(byte[] guid, String query) {
        query(guid, query, null);
    }

	/**
	 * Searches the network for files with the given metadata.
	 * 
	 * @param richQuery metadata query to insert between the nulls,
	 *  typically in XML format
	 * @see query(byte[], String, MediaType)
	 */
	public static void query(final byte[] guid, 
							 final String query, 
							 final String richQuery, 
							 final MediaType type) {

		try {
            QueryRequest qr = null;
            // DPINJ: Use a passed in networkManager!!!
            if (ProviderHacks.getNetworkManager().isIpPortValid() && (new GUID(guid)).addressesMatch(ProviderHacks.getNetworkManager().getAddress(), 
                    ProviderHacks.getNetworkManager().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = ProviderHacks.getQueryRequestFactory().createOutOfBandQuery(guid, query, richQuery,
                                                       type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = ProviderHacks.getQueryRequestFactory().createQuery(guid, query, richQuery, type);
            recordAndSendQuery(qr, type);
		} catch(Throwable t) {
			ErrorService.error(t);
		}
	}


	/**
	 * Sends a 'What Is New' query on the network.
	 */
	public static void queryWhatIsNew(final byte[] guid, final MediaType type) {
		try {
            QueryRequest qr = null;
            if (GUID.addressesMatch(guid, ProviderHacks.getNetworkManager().getAddress(), ProviderHacks.getNetworkManager().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = ProviderHacks.getQueryRequestFactory().createWhatIsNewOOBQuery(guid, (byte)2, type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = ProviderHacks.getQueryRequestFactory().createWhatIsNewQuery(guid, (byte)2, type);

            if(FilterSettings.FILTER_WHATS_NEW_ADULT.getValue())
                ProviderHacks.getMutableGUIDFilter().addGUID(guid);
    
            recordAndSendQuery(qr, type);
		} catch(Throwable t) {
			ErrorService.error(t);
		}
	}

    /** Just aggregates some common code in query() and queryWhatIsNew().
     */ 
    private static void recordAndSendQuery(final QueryRequest qr, 
                                           final MediaType type) {
        ProviderHacks.getQueryStats().recordQuery();
        ProviderHacks.getResponseVerifier().record(qr, type);
        ProviderHacks.getSearchResultHandler().addQuery(qr); // so we can leaf guide....
        ProviderHacks.getMessageRouter().sendDynamicQuery(qr);
    }

	/**
	 * Accessor for the last time a query was originated from this host.
	 *
	 * @return a <tt>long</tt> representing the number of milliseconds since
	 *  January 1, 1970, that the last query originated from this host
	 */
	public static long getLastQueryTime() {
		return ProviderHacks.getQueryStats().getLastQueryTime();
	}

    /** Purges the query from the QueryUnicaster (GUESS) and the ResultHandler
     *  (which maintains query stats for the purpose of leaf guidance).
     *  @param guid The GUID of the query you want to get rid of....
     */
    public static void stopQuery(GUID guid) {
        ProviderHacks.getQueryUnicaster().purgeQuery(guid);
        ProviderHacks.getSearchResultHandler().removeQuery(guid);
        ProviderHacks.getMessageRouter().queryKilled(guid);
        if(RouterService.isSupernode())
            ProviderHacks.getQueryDispatcher().addToRemove(guid);
        ProviderHacks.getMutableGUIDFilter().removeGUID(guid.bytes());
    }

    /** 
     * Returns true if the given response is of the same type as the the query
     * with the given guid.  Returns 100 if guid is not recognized.
     *
     * @param guid the value returned by query(..).  MUST be 16 bytes long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#matchesType(byte[], Response) 
     */
    public static boolean matchesType(byte[] guid, Response response) {
        return ProviderHacks.getResponseVerifier().matchesType(guid, response);
    }

    public static boolean matchesQuery(byte [] guid, Response response) {
        return ProviderHacks.getResponseVerifier().matchesQuery(guid, response);
    }
    /** 
     * Returns true if the given response for the query with the given guid is a
     * result of the Madragore worm (8KB files of form "x.exe").  Returns false
     * if guid is not recognized.  <i>Ideally this would be done by the normal
     * filtering mechanism, but it is not powerful enough without the query
     * string.</i>
     *
     * @param guid the value returned by query(..).  MUST be 16 byts long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#isMandragoreWorm(byte[], Response) 
     */
    public static boolean isMandragoreWorm(byte[] guid, Response response) {
        return ProviderHacks.getResponseVerifier().isMandragoreWorm(guid, response);
    }
    
    /**
     * Returns a collection of IpPorts, preferencing hosts with open slots.
     * If isUltrapeer is true, this preferences hosts with open ultrapeer slots,
     * otherwise it preferences hosts with open leaf slots.
     *
     * Preferences via locale, also.
     * 
     * @param num How many endpoints to try to get
     */
    public static Collection<IpPort> getPreferencedHosts(boolean isUltrapeer, String locale, int num) {
        
        Set<IpPort> hosts = new IpPortSet();
        
        if(isUltrapeer)
            hosts.addAll(ProviderHacks.getHostCatcher().getUltrapeersWithFreeUltrapeerSlots(locale,num));
        else
            hosts.addAll(ProviderHacks.getHostCatcher().getUltrapeersWithFreeLeafSlots(locale,num));
        
        // If we don't have enough hosts, add more.
        
        if(hosts.size() < num) {
            //we first try to get the connections that match the locale.
            for(IpPort ipp : ProviderHacks.getConnectionManager().getInitializedConnectionsMatchLocale(locale)) {
                if(hosts.size() >= num)
                    break;
                hosts.add(ipp);
            }
            
            //if we still don't have enough hosts, get them from the list
            //of all initialized connection
            if(hosts.size() < num) {
                for(IpPort ipp : ProviderHacks.getConnectionManager().getInitializedConnections()) {
                    if(hosts.size() >= num)
                        break;
                    hosts.add(ipp);
                }
            }
        }
        
        return hosts;
    }

    /**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isFullyConnected() {
		return ProviderHacks.getConnectionManager().isFullyConnected();
	}    

	/**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isConnected() {
		return ProviderHacks.getConnectionManager().isConnected();
	}
	
	/**
	 * Returns whether or not this client is attempting to connect.
	 */
	public static boolean isConnecting() {
	    return ProviderHacks.getConnectionManager().isConnecting();
	}

    /**
     * Returns the number of files being shared locally.
     */
    public static int getNumSharedFiles( ) {
        return( ProviderHacks.getFileManager().getNumFiles() );
    }
    
    /**
     * Returns the number of files which are awaiting sharing.
     */
    public static int getNumPendingShared() {
        return( ProviderHacks.getFileManager().getNumPendingFiles() );
    }

	/**
	 * Returns the size in bytes of shared files.
	 *
	 * @return the size in bytes of shared files on this host
	 */
	public static int getSharedFileSize() {
		return ProviderHacks.getFileManager().getSize();
	}
	
	/** 
	 * Returns a list of all incomplete shared file descriptors.
	 */
	public static FileDesc[] getIncompleteFileDescriptors() {
	    return ProviderHacks.getFileManager().getIncompleteFileDescriptors();
	}

    /**
     * Returns a list of all shared file descriptors in the given directory.
     * All the file descriptors returned have already been passed to the gui
     * via ActivityCallback.addSharedFile.  Note that if a file descriptor
     * is added to the given directory after this method completes, 
     * addSharedFile will be called for that file descriptor.<p>
     *
     * If directory is not a shared directory, returns null.
     */
    public static FileDesc[] getSharedFileDescriptors(File directory) {
		return ProviderHacks.getFileManager().getSharedFileDescriptors(directory);
    }
    
    /** 
     * Tries to "smart download" <b>any</b> [sic] of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * SaveLocationException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, SaveLocationException is thrown and no files are
     * modified.  If overwrite==true, the files may be overwritten.<p>
     * 
     * Otherwise returns a Downloader that allows you to stop and resume this
     * download.  The ActivityCallback will also be notified of this download,
     * so the return value can usually be ignored.  The download begins
     * immediately, unless it is queued.  It stops after any of the files
     * succeeds.  
     *
     * @param files a group of "similar" files to smart download
     * @param alts a List of secondary RFDs to use for other sources
     * @param queryGUID guid of the query that returned the results (i.e. files)
	 * @param overwrite true iff the download should proceedewithout
     *  checking if it's on disk
	 * @param saveDir can be null, then the save directory from the settings
	 * is used
	 * @param fileName can be null, then one of the filenames of the 
	 * <code>files</code> array is used
	 * array is used
     * @return the download object you can use to start and resume the download
     * @throws SaveLocationException if there is an error when setting the final
     * file location of the download 
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
	public static Downloader download(RemoteFileDesc[] files, 
	                                  List<? extends RemoteFileDesc> alts, GUID queryGUID,
                                      boolean overwrite, File saveDir,
									  String fileName)
		throws SaveLocationException {
		return ProviderHacks.getDownloadManager().download(files, alts, queryGUID, overwrite, saveDir,
								   fileName);
	}
	
	public static Downloader download(RemoteFileDesc[] files, 
									  List<? extends RemoteFileDesc> alts,
									  GUID queryGUID,
									  boolean overwrite)
		throws SaveLocationException {
		return download(files, alts, queryGUID, overwrite, null, null);
	}	
	
	/**
	 * Stub for calling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
	 * @throws SaveLocationException 
	 */
	public static Downloader download(RemoteFileDesc[] files,
                                      GUID queryGUID, 
                                      boolean overwrite, File saveDir, String fileName)
		throws SaveLocationException {
		return download(files, RemoteFileDesc.EMPTY_LIST, queryGUID,
				overwrite, saveDir, fileName);
	}
	
	public static Downloader download(RemoteFileDesc[] files,
									  boolean overwrite, GUID queryGUID) 
		throws SaveLocationException {
		return download(files, queryGUID, overwrite, null, null);
	}	
        
	/**
	 * Creates a downloader for a magnet.
	 * @param magnetprovides the information of the  file to download, must be
	 *  valid
	 * @param overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @return
	 * @throws SaveLocationException
	 * @throws IllegalArgumentException if the magnet is not 
	 * {@link MagnetOptions#isDownloadable() valid}.
	 */
	public static Downloader download(MagnetOptions magnet, boolean overwrite) 
		throws SaveLocationException {
		if (!magnet.isDownloadable()) {
			throw new IllegalArgumentException("invalid magnet: not have enough information for downloading");
		}
		return ProviderHacks.getDownloadManager().download(magnet, overwrite, null, magnet.getDisplayName());
	}

	/**
	 * Creates a downloader for a magnet using the given additional options.
	 *
	 * @param magnet provides the information of the  file to download, must be
	 *  valid
	 * @param overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @param saveDir can be null, then the save directory from the settings
	 * is used
	 * @param fileName the final filename of the download, can be
	 * <code>null</code>
	 * @return
	 * @throws SaveLocationException
	 * @throws IllegalArgumentException if the magnet is not
	 * {@link MagnetOptions#isDownloadable() downloadable}.
	 */
	public static Downloader download(MagnetOptions magnet, boolean overwrite,
			File saveDir, String fileName) throws SaveLocationException {
		return ProviderHacks.getDownloadManager().download(magnet, overwrite, saveDir, fileName);
	}

   /**
     * Starts a resume download for the given incomplete file.
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SaveLocationException 
     */ 
    public static Downloader download(File incompleteFile)
            throws CantResumeException, SaveLocationException {
        return ProviderHacks.getDownloadManager().download(incompleteFile);
    }

    
    /**
	 * Starts a torrent download for a given Inputstream to the .torrent file
	 * 
	 * @param is
	 *            the InputStream belonging to the .torrent file
	 * @throws IOException
	 *             in case there was a problem reading the file 
	 */
	public static Downloader downloadTorrent(BTMetaInfo info, boolean overwrite)
			throws SaveLocationException {
		return ProviderHacks.getDownloadManager().downloadTorrent(info, overwrite);
	}
    
	/**
	 * Creates and returns a new chat to the given host and port.
     * 
     * <p>{@link Chatter#start()} needs to be invoked to initiate the connection. 
	 */
	public static Chatter createChat(String host, int port) {
		Chatter chatter = ProviderHacks.getChatManager().request(host, port);
		return chatter;
	}
    
    /**
	 * Browses the passed host
     * @param host The host to browse
     * @param port The port at which to browse
     * @param guid The guid to be used for the query replies received 
     * while browsing host
     * @param serventID The guid of the client to browse from.  I need this in
     * case I need to push....
     * @param proxies the list of PushProxies we can use - may be null.
     * @param canDoFWTransfer true if the remote host supports fw transfer
	 */
	public static BrowseHostHandler doAsynchronousBrowseHost(
	  final Connectable host, GUID guid, GUID serventID, 
	  final Set<? extends IpPort> proxies, final boolean canDoFWTransfer) {
        final BrowseHostHandler handler = new BrowseHostHandler(callback, 
                                                          guid, serventID);
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                handler.browseHost(host, proxies, canDoFWTransfer);
            }
        }, "BrowseHoster" );
        
        return handler;
	}

    /**
     * Tells whether the node is a supernode or not.
     * NOTE: This will return true if this node is capable
     * of being a supernode but is not yet connected to 
     * the network as one (and is not a shielded leaf either).
     * 
     * @return true, if supernode, false otherwise
     */
    public static boolean isSupernode() {
        return ProviderHacks.getConnectionManager().isSupernode();
    }
    
    /**
     * Tells whether the node is currently connected to the network
     * as a supernode or not.
     * @return true, if active supernode, false otherwise
     */
    public static boolean isActiveSuperNode() {
        return ProviderHacks.getConnectionManager().isActiveSupernode();
    }
    
	/**
	 * Accessor for whether or not this node is a shielded leaf.
	 *
	 * @return <tt>true</tt> if this node is a shielded leaf, 
	 *  <tt>false</tt> otherwise
	 */
    public static boolean isShieldedLeaf() {
        return ProviderHacks.getConnectionManager().isShieldedLeaf();
    }    


    /**
     * @return the number of free leaf slots.
     */
    public static int getNumFreeLeafSlots() {
            return ProviderHacks.getConnectionManager().getNumFreeLeafSlots();
    }

    
    /**
     * @return the number of free non-leaf slots.
     */
    public static int getNumFreeNonLeafSlots() {
        return ProviderHacks.getConnectionManager().getNumFreeNonLeafSlots();
    }

    /**
     * @return the number of free leaf slots available for limewires.
     */
    public static int getNumFreeLimeWireLeafSlots() {
            return ProviderHacks.getConnectionManager().getNumFreeLimeWireLeafSlots();
    }

    
    /**
     * @return the number of free non-leaf slots available for limewires.
     */
    public static int getNumFreeLimeWireNonLeafSlots() {
        return ProviderHacks.getConnectionManager().getNumFreeLimeWireNonLeafSlots();
    }


    /**
     * Sets the flag for whether or not LimeWire is currently in the process of 
	 * shutting down.
	 *
     * @param flag the shutting down state to set
     */
    public static void setIsShuttingDown(boolean flag) {
		isShuttingDown = flag;
    }

	/**
	 * Returns whether or not LimeWire is currently in the shutting down state,
	 * meaning that a shutdown has been initiated but not completed.  This
	 * is most often the case when there are active file transfers and the
	 * application is set to shutdown after current file transfers are complete.
	 *
	 * @return <tt>true</tt> if the application is in the shutting down state,
	 *  <tt>false</tt> otherwise
	 */
    public static boolean getIsShuttingDown() {
		return isShuttingDown;
    }
}
