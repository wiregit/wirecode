package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.bootstrap.BootstrapServerManager;
import com.limegroup.gnutella.browser.HTTPAcceptor;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.downloader.AlreadyDownloadingException;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.FileExistsException;
import com.limegroup.gnutella.downloader.HTTPDownloader;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.MutableGUIDFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.Cookies;
import com.limegroup.gnutella.security.ServerAuthenticator;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SettingsHandler;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.udpconnect.UDPMultiplexor;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.upelection.PromotionManager;
import com.limegroup.gnutella.uploader.NormalUploadState;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.SimpleTimer;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.xml.MetaFileManager;

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
    
    private static final Log LOG = LogFactory.getLog(RouterService.class);

	/**
	 * <tt>FileManager</tt> instance that manages access to shared files.
	 */
    private static FileManager fileManager = new MetaFileManager();

    /**
     * For authenticating users.
     */
    private static final Authenticator authenticator = 
		new ServerAuthenticator();

	/**
	 * Timer similar to java.util.Timer, which was not available on 1.1.8.
	 */
    private static final SimpleTimer timer = new SimpleTimer(true);

	/**
	 * <tt>Acceptor</tt> instance for accepting new connections, HTTP
	 * requests, etc.
	 */
    private static final Acceptor acceptor = new Acceptor();

    /**
     * <tt>HTTPAcceptor</tt> instance for accepting magnet requests, etc.
     */
    private static HTTPAcceptor httpAcceptor;

	/**
	 * Initialize the class that manages all TCP connections.
	 */
    private static ConnectionManager manager =
		new ConnectionManager(authenticator);

	/**
	 * <tt>HostCatcher</tt> that handles Gnutella pongs.  Only not final
     * for tests.
	 */
    private static HostCatcher catcher = new HostCatcher();
	
	/**
	 * <tt>DownloadManager</tt> for handling HTTP downloading.
	 */
    private static DownloadManager downloader = new DownloadManager();

	/**
	 * <tt>UploadManager</tt> for handling HTTP uploading.
	 */
    private static UploadManager uploadManager = new UploadManager();
    
    /**
     * <tt>PushManager</tt> for handling push requests.
     */
    private static PushManager pushManager = new PushManager();
    
    /**
     * <tt>PromotionManager</tt> for handling promotions to Ultrapeer.
     */
    private static PromotionManager promotionManager = new PromotionManager();

	
    private static final ResponseVerifier verifier = new ResponseVerifier();

	/**
	 * <tt>Statistics</tt> class for managing statistics.
	 */
	private static final Statistics statistics = Statistics.instance();

	/**
	 * Constant for the <tt>UDPService</tt> instance that handles UDP 
	 * messages.
	 */
	private static final UDPService udpService = UDPService.instance();

	/**
	 * Constant for the <tt>SearchResultHandler</tt> class that processes
	 * search results sent back to this client.
	 */
	private static final SearchResultHandler RESULT_HANDLER =
		new SearchResultHandler();

    /**
     * isShuttingDown flag
     */
    private static boolean isShuttingDown;

	/**
	 * Variable for the <tt>ActivityCallback</tt> instance.
	 */
    private static ActivityCallback callback;

	/**
	 * Variable for the <tt>MessageRouter</tt> that routes Gnutella
	 * messages.
	 */
    private static MessageRouter router;

    /**
     * Variable for whether or not that backend threads have been started.
     * 0 - nothing started
     * 1 - pre/while gui tasks started
     * 2 - everything started
     * LOCKING: RouterService.class 
     */
    private static int _started;


	/**
	 * Long for the last time this host originated a query.
	 */
	private static long _lastQueryTime = 0L;
	
	/**
	 * Whether or not we are running at full power.
	 */
	private static boolean _fullPower = true;
	
	private static final byte [] MYGUID;
	static {
	    byte [] myguid=null;
	    try {
	        myguid = GUID.fromHexString(ApplicationSettings.CLIENT_ID.getValue());
	    }catch(IllegalArgumentException iae) {
	        myguid = GUID.makeGuid();
	        ApplicationSettings.CLIENT_ID.setValue((new GUID(myguid)).toHexString());
	    }
	    MYGUID=myguid;
	}

	/**
	 * Creates a new <tt>RouterService</tt> instance.  This fully constructs 
	 * the backend.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use for
	 *  making callbacks
	 */
  	public RouterService(ActivityCallback callback) {
        this(callback, new StandardMessageRouter());
    }

    /**
     * Constructor for the Peer Server.
     */ 
    public RouterService(ActivityCallback ac, MessageRouter mr, FileManager fm){
        this(ac,mr);
        RouterService.fileManager = fm;
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
  		RouterService.router = router;
  	}

  	/**
  	 * Performs startup tasks that should happen while the GUI loads
  	 */
  	public void asyncGuiInit() {
  		
  		synchronized(RouterService.class) {
  			if (_started > 0) // already did this?
  				return;
  			else
  				_started = 1;
  		}
  		
  	    Thread t = new ManagedThread(new Initializer());
  	    t.setName("async gui initializer");
  	    t.setDaemon(true);
  	    t.start();
  	}
  	
  	/**
  	 * performs the tasks usually run while the gui is initializing synchronously
  	 * to be used for tests and when running only the core
  	 */
  	public void preGuiInit() {
  		
  		synchronized(RouterService.class) {
  			if (_started > 0) // already did this?
  				return;
  			else
  				_started = 1;
  		}
  		
  	    (new Initializer()).run();
  	}
  	
  	private static class Initializer implements Runnable {
  	    public void run() {
  	        //add more while-gui init tasks here
  	        RouterService.getAcceptor().init();
  	    }
  	}
  	
	/**
	 * Starts various threads and tasks once all core classes have
	 * been constructed.
	 */
	public void start() {
	    synchronized(RouterService.class) {
    	    LOG.trace("START RouterService");
    	    
    	    if ( isStarted() ) return;
    	    	preGuiInit();
            _started = 2;
    
    		// Now, link all the pieces together, starting the various threads.

            //Note: SimppManager and SimppSettingsManager must go first to make
            //sure all the settings are created with the simpp values. Other
            //components may rely on the settings, so they must have the right
            //values when they are being initialized.
            LOG.trace("START SimppManager.instance");
            callback.componentLoading("SIMPP_MANAGER");
            SimppManager.instance();//initialize
            LOG.trace("STOP SimppManager.instance");
            
            LOG.trace("START SimppSettingsManager.instance");
            SimppSettingsManager.instance();
            LOG.trace("STOP SimppSettingsManager.instance");

            LOG.trace("START MessageRouter");
            callback.componentLoading("MESSAGE_ROUTER");
    		router.initialize();
    		LOG.trace("STOPMessageRouter");
    		
            LOG.trace("START Acceptor");
            callback.componentLoading("ACCEPTOR");
    		acceptor.start();
    		LOG.trace("STOP Acceptor");
    		
    		LOG.trace("START ConnectionManager");
    		callback.componentLoading("CONNECTION_MANAGER");
    		manager.initialize();
    		LOG.trace("STOP ConnectionManager");
    		
    		LOG.trace("START DownloadManager");
    		downloader.initialize(); 
    		LOG.trace("STOP DownloadManager");
    		
    		LOG.trace("START SupernodeAssigner");
    		SupernodeAssigner sa = new SupernodeAssigner(uploadManager, 
    													 downloader, 
    													 manager);
    		sa.start();
    		LOG.trace("STOP SupernodeAssigner");
    
            // THIS MUST BE BEFORE THE CONNECT (below)
            // OTHERWISE WE WILL ALWAYS CONNECT TO GWEBCACHES
            LOG.trace("START HostCatcher.initialize");
            callback.componentLoading("HOST_CATCHER");
    		catcher.initialize();
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
            callback.componentLoading("FILE_MANAGER");
            fileManager.start();
            LOG.trace("STOP FileManager");
    
            // Restore any downloads in progress.
            LOG.trace("START DownloadManager.postGuiInit");
            callback.componentLoading("DOWNLOAD_MANAGER_POST_GUI");
            downloader.postGuiInit();
            LOG.trace("STOP DownloadManager.postGuiInit");
            
            LOG.trace("START UpdateManager.instance");
            callback.componentLoading("UPDATE_MANAGER");
            UpdateManager.instance();//initialize
            LOG.trace("STOP UpdateManager.instance");

            LOG.trace("START QueryUnicaster");
            callback.componentLoading("QUERY_UNICASTER");
    		QueryUnicaster.instance().start();
    		LOG.trace("STOP QueryUnicaster");
    		
    		LOG.trace("START HTTPAcceptor");
            callback.componentLoading("HTTPACCEPTOR");
            httpAcceptor = new HTTPAcceptor();  
            httpAcceptor.start();
            LOG.trace("STOP HTTPAcceptor");
            
            LOG.trace("START Pinger");
            callback.componentLoading("PINGER");
            Pinger.instance().start();
            LOG.trace("STOP Pinger");
            
            LOG.trace("START ConnectionWatchdog");
            callback.componentLoading("CONNECTION_WATCHDOG");
            ConnectionWatchdog.instance().start();
            LOG.trace("STOP ConnectionWatchdog");
            
            LOG.trace("START SavedFileManager");
            callback.componentLoading("SAVED_FILE_MANAGER");
            SavedFileManager.instance();
            LOG.trace("STOP SavedFileManager");
            
            if(ApplicationSettings.AUTOMATIC_MANUAL_GC.getValue())
                startManualGCThread();
            
            LOG.trace("STOP RouterService.");
        }
	}
	
	/**
	 * Starts a manual GC thread.
	 */
	private void startManualGCThread() {
	    Thread t = new ManagedThread(new Runnable() {
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
     * Used to determine whether or not the backend threads have been
     * started.
     *
     * @return <tt>true</tt> if the backend threads have been started,
     *  otherwise <tt>false</tt>
     */
    public synchronized static boolean isStarted() {
        return _started == 2;
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
            NormalUploadState.setThrottleSwitching(!newValue);
            HTTPDownloader.setThrottleSwitching(!newValue);
        }
    }

	/**
	 * Accessor for the <tt>MessageRouter</tt> instance.
	 *
	 * @return the <tt>MessageRouter</tt> instance in use --
	 *  this is one of the few accessors that can be <tt>null</tt> -- this 
	 *  will be <tt>null</tt> in the case where the <tt>RouterService</tt>
	 *  has not been constructed
	 */
	public static MessageRouter getMessageRouter() {
		return router;
	}
    
	/**
	 * Accessor for the <tt>FileManager</tt> instance in use.
	 *
	 * @return the <tt>FileManager</tt> in use
	 */
    public static FileManager getFileManager(){
        return fileManager;
    }

    /** 
     * Accessor for the <tt>DownloadManager</tt> instance in use.
     *
     * @return the <tt>DownloadManager</tt> in use
     */
    public static DownloadManager getDownloadManager() {
        return downloader;
    }

	/**
	 * Accessor for the <tt>UDPService</tt> instance.
	 *
	 * @return the <tt>UDPService</tt> instance in use
	 */
	public static UDPService getUdpService() {
		return udpService;
	}
	
	/**
	 * Gets the UDPMultiplexor.
	 */
	public static UDPMultiplexor getUDPConnectionManager() {
	    return UDPMultiplexor.instance();
	}

	/**
	 * Accessor for the <tt>ConnectionManager</tt> instance.
	 *
	 * @return the <tt>ConnectionManager</tt> instance in use
	 */
	public static ConnectionManager getConnectionManager() {
		return manager;
	}
	
    /** 
     * Accessor for the <tt>UploadManager</tt> instance.
     *
     * @return the <tt>UploadManager</tt> in use
     */
	public static UploadManager getUploadManager() {
		return uploadManager;
	}
	
	/**
	 * Accessor for the <tt>PushManager</tt> instance.
	 *
	 * @return the <tt>PushManager</tt> in use
	 */
	public static PushManager getPushManager() {
	    return pushManager;
	}
	
    /** 
     * Accessor for the <tt>Acceptor</tt> instance.
     *
     * @return the <tt>Acceptor</tt> in use
     */
	public static Acceptor getAcceptor() {
		return acceptor;
	}

    /** 
     * Accessor for the <tt>Acceptor</tt> instance.
     *
     * @return the <tt>Acceptor</tt> in use
     */
    public static HTTPAcceptor getHTTPAcceptor() {
        return httpAcceptor;
    }

    /** 
     * Accessor for the <tt>HostCatcher</tt> instance.
     *
     * @return the <tt>HostCatcher</tt> in use
     */
	public static HostCatcher getHostCatcher() {
		return catcher;
	}

    /** 
     * Accessor for the <tt>SearchResultHandler</tt> instance.
     *
     * @return the <tt>SearchResultHandler</tt> in use
     */
	public static SearchResultHandler getSearchResultHandler() {
		return RESULT_HANDLER;
	}
	
	/**
	 * Accessor for the <tt>PromotionManager</tt> instance.
	 * @return the <tt>PromotionManager</tt> in use.
	 */
	public static PromotionManager getPromotionManager() {
		return promotionManager;
	}
	
	public static byte [] getMyGUID() {
	    return MYGUID;
	}

    /**
     * Schedules the given task for repeated fixed-delay execution on this'
     * backend thread.  <b>The task must not block for too long</b>, as 
     * a single thread is shared among all the backend.
     *
     * @param task the task to run repeatedly
     * @param delay the initial delay, in milliseconds
     * @param period the delay between executions, in milliseconds
     * @exception IllegalStateException this is cancelled
     * @exception IllegalArgumentException delay or period negative
     * @see com.limegroup.gnutella.util.SimpleTimer#schedule(java.lang.Runnable,long,long)
     */
    public static void schedule(Runnable task, long delay, long period) {
        timer.schedule(task, delay, period);
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port.
     * Blocks until the connection established.  Throws IOException if
     * the connection failed.
     * @return a connection to the request host
     * @exception IOException the connection failed
     */
    public static ManagedConnection connectToHostBlocking(String hostname, int portnum)
		throws IOException {
        return manager.createConnectionBlocking(hostname, portnum);
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port. 
     * Returns immediately without blocking.  If hostname would connect
     * us to ourselves, returns immediately.
     */
    public static void connectToHostAsynchronously(String hostname, int portnum) {
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
        if ((cIP[0] == 127) && (portnum==acceptor.getPort(true)) &&
			ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
			return;
        } else {
            byte[] managerIP=acceptor.getAddress(true);
            if (Arrays.equals(cIP, managerIP)
                && portnum==acceptor.getPort(true))
                return;
        }

        if (!acceptor.isBannedIP(cIP)) {
            manager.createConnectionAsynchronously(hostname, portnum);
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
        return manager.isConnectedTo(host) ||
               UDPMultiplexor.instance().isConnectedTo(addr) ||
               uploadManager.isConnectedTo(addr); // ||
               // dloadManager.isConnectedTo(addr);
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public static void connect() {
        adjustSpamFilters();
        
        //delegate to connection manager
        manager.connect();
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public static void disconnect() {
		// Delegate to connection manager
		manager.disconnect();
    }

    /**
     * Closes and removes the given connection.
     */
    public static void removeConnection(ManagedConnection c) {
        manager.remove(c);
    }

    /**
     * Clears the hostcatcher.
     */
    public static void clearHostCatcher() {
        catcher.clear();
    }

    /**
     * Returns the number of pongs in the host catcher.  <i>This method is
     * poorly named, but it's obsolescent, so I won't bother to rename it.</i>
     */
    public static int getRealNumHosts() {
        return(catcher.getNumHosts());
    }

    /**
     * Returns the number of downloads in progress.
     */
    public static int getNumDownloads() {
        return downloader.downloadsInProgress();
    }
    
    /**
     * Returns the number of active downloads.
     */
    public static int getNumActiveDownloads() {
        return downloader.getNumActiveDownloads();
    }
    
    /**
     * Returns the number of downloads waiting to be started.
     */
    public static int getNumWaitingDownloads() {
        return downloader.getNumWaitingDownloads();
    }
    
    /**
     * Returns the number of individual downloaders.
     */
    public static int getNumIndividualDownloaders() {
        return downloader.getNumIndividualDownloaders();
    }
    
    /**
     * Returns the number of uploads in progress.
     */
    public static int getNumUploads() {
        return uploadManager.uploadsInProgress();
    }

    /**
     * Returns the number of queued uploads.
     */
    public static int getNumQueuedUploads() {
        return uploadManager.getNumQueuedUploads();
    }
    
    /**
     * Returns the current uptime.
     */
    public static long getCurrentUptime() {
        return statistics.getUptime();
    }


    /**
     * Shuts down the backend and writes the gnutella.net file.
     */
    public static synchronized void shutdown() {
        try {
            if(!isStarted())
                return;
            
            getAcceptor().haltUPnP();
            
            //Update fractional uptime statistics (before writing limewire.props)
            Statistics.instance().shutdown();
            
            //Update firewalled status
            ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(
                acceptedIncomingConnection());

            //Write gnutella.net
            try {
                catcher.write();
            } catch (IOException e) {}
            finally {
                SettingsHandler.save();
            }
            //Cleanup any preview files.  Note that these will not be deleted if
            //your previewer is still open.
            File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
            if (incompleteDir == null) { return; } // if we could not get the incomplete directory, simply return.
            
            String[] files=incompleteDir.list();
            
            if(files == null) return;
            
            for (int i=0; i<files.length; i++) {
                if (files[i].startsWith(IncompleteFileManager.PREVIEW_PREFIX)) {
                    File file=new File(incompleteDir, files[i]);
                    file.delete();  //May or may not work; ignore return code.
                }
            }
            
            //Write download state
            downloader.writeSnapshot();
            
            //save cookies
            Cookies.instance().save();
            
            //persist urn cache
            UrnCache.instance().persistCache();
            CreationTimeCache.instance().persistCache();
            TigerTreeCache.instance().persistCache();
            
        } catch(Throwable t) {
            ErrorService.error(t);
        }
    }

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public static void adjustSpamFilters() {
        IPFilter.refreshIPFilter();

        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for (Iterator iter=manager.getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            c.setPersonalFilter(SpamFilter.newPersonalFilter());
            c.setRouteFilter(SpamFilter.newRouteFilter());
        }
        
        UDPReplyHandler.setPersonalFilter(SpamFilter.newPersonalFilter());
    }

    /**
     * Sets the port on which to listen for incoming connections.
     * If that fails, this is <i>not</i> modified and IOException is thrown.
     * If port==0, tells this to stop listening to incoming connections.
     */
    public static void setListeningPort(int port) throws IOException {
        acceptor.setListeningPort(port);
    }

    /** 
     * Returns true if this has accepted an incoming connection, and hence
     * probably isn't firewalled.  (This is useful for colorizing search
     * results in the GUI.)
     */
    public static boolean acceptedIncomingConnection() {
		return acceptor.acceptedIncoming();
    }

    /**
     * Count up all the messages on active connections
     */
    public static int getActiveConnectionMessages() {
		int count = 0;

        // Count the messages on initialized connections
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            count += c.getNumMessagesSent();
            count += c.getNumMessagesReceived();
        }
		return count;
    }

    /**
     * Count how many connections have already received N messages
     */
    public static int countConnectionsWithNMessages(int messageThreshold) {
		int count = 0;
		int msgs; 

        // Count the messages on initialized connections
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            msgs = c.getNumMessagesSent();
            msgs += c.getNumMessagesReceived();
			if ( msgs > messageThreshold )
				count++;
        }
		return count;
    }

    /**
     *  Returns the number of good hosts in my horizon.
     */
    public static long getNumHosts() {
        return HorizonCounter.instance().getNumHosts();
    }

    /**
     * Returns the number of files in my horizon.
     */
    public static long getNumFiles() {
        return HorizonCounter.instance().getNumFiles();
    }

    /**
     * Returns the size of all files in my horizon, in kilobytes.
     */
    public static long getTotalFileSize() {
        return HorizonCounter.instance().getTotalFileSize();
    }

    /**
     * Prints out the information about current initialied connections
     */
    public static void dumpConnections() {
        //dump ultrapeer connections
        System.out.println("UltraPeer connections");
        dumpConnections(manager.getInitializedConnections());
        //dump leaf connections
        System.out.println("Leaf connections");
        dumpConnections(manager.getInitializedClientConnections());
    }
    
    /**
     * Prints out the passed collection of connections
     * @param connections The collection(of Connection) 
     * of connections to be printed
     */
    private static void dumpConnections(Collection connections)
    {
        for(Iterator iterator = connections.iterator(); iterator.hasNext();) {
            System.out.println(iterator.next().toString());
        }
    }
    
    /**
     * Updates the horizon statistics.  This should called at least every five
     * minutes or so to prevent the reported numbers from growing too large.
     * You can safely call it more often.  Note that it does not modify the
     * network; horizon stats are calculated by passively looking at messages.
     *
     * @modifies this (values returned by getNumFiles, getTotalFileSize, and
     *  getNumHosts) 
     */
    public static void updateHorizon() {
        HorizonCounter.instance().refresh();
    }

    /** 
     * Returns a new GUID for passing to query.
     * This method is the central point of decision making for sending out OOB 
     * queries.
     */
    public static byte[] newQueryGUID() {
        if (isOOBCapable() && OutOfBandThroughputStat.isOOBEffectiveForMe())
            return GUID.makeAddressEncodedGuid(getAddress(), getPort());
        else
            return GUID.makeGuid();
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
            if (isIpPortValid() && (new GUID(guid)).addressesMatch(getAddress(), 
                                                                   getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = QueryRequest.createOutOfBandQuery(guid, query, richQuery,
                                                       type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = QueryRequest.createQuery(guid, query, richQuery, type);
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
            if (GUID.addressesMatch(guid, getAddress(), getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = QueryRequest.createWhatIsNewOOBQuery(guid, (byte)2, type);
                OutOfBandThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = QueryRequest.createWhatIsNewQuery(guid, (byte)2, type);

            if(FilterSettings.FILTER_WHATS_NEW_ADULT.getValue())
                MutableGUIDFilter.instance().addGUID(guid);
    
            recordAndSendQuery(qr, type);
		} catch(Throwable t) {
			ErrorService.error(t);
		}
	}

    /** Just aggregates some common code in query() and queryWhatIsNew().
     */ 
    private static void recordAndSendQuery(final QueryRequest qr, 
                                           final MediaType type) {
        _lastQueryTime = System.currentTimeMillis();
        verifier.record(qr, type);
        RESULT_HANDLER.addQuery(qr); // so we can leaf guide....
        router.sendDynamicQuery(qr);
    }

	/**
	 * Accessor for the last time a query was originated from this host.
	 *
	 * @return a <tt>long</tt> representing the number of milliseconds since
	 *  January 1, 1970, that the last query originated from this host
	 */
	public static long getLastQueryTime() {
		return _lastQueryTime;
	}

    /** Purges the query from the QueryUnicaster (GUESS) and the ResultHandler
     *  (which maintains query stats for the purpose of leaf guidance).
     *  @param guid The GUID of the query you want to get rid of....
     */
    public static void stopQuery(GUID guid) {
        QueryUnicaster.instance().purgeQuery(guid);
        RESULT_HANDLER.removeQuery(guid);
        router.queryKilled(guid);
        if(RouterService.isSupernode())
            QueryDispatcher.instance().addToRemove(guid);
        MutableGUIDFilter.instance().removeGUID(guid.bytes());
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
        return verifier.matchesType(guid, response);
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
        return verifier.isMandragoreWorm(guid, response);
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
    public static Collection getPreferencedHosts(boolean isUltrapeer, String locale, int num) {
        
        // note that we need to use a TreeSet because the objects returned
        // from the various adding calls below will be different types,
        // and hashCode & equals won't be respected.
        Set hosts = new TreeSet(IpPort.COMPARATOR);
        
        if(isUltrapeer)
            hosts.addAll(catcher.getUltrapeersWithFreeUltrapeerSlots(locale,num));
        else
            hosts.addAll(catcher.getUltrapeersWithFreeLeafSlots(locale,num));
        
        // If we don't have enough hosts, add more.
        
        if(hosts.size() < num) {
            //we first try to get the connections that match the locale.
            List conns = manager.getInitializedConnectionsMatchLocale(locale);
            for(Iterator i = conns.iterator(); i.hasNext() && hosts.size() < num;)
                hosts.add(i.next());
            
            //if we still don't have enough hosts, get them from the list
            //of all initialized connection
            if(hosts.size() < num) {
                //list returned is unmmodifiable
                conns = manager.getInitializedConnections();
                for(Iterator i = conns.iterator(); i.hasNext() && hosts.size() < num;)
                    hosts.add(i.next());
            }
        }
        
        return hosts;
    }


    /**
     *  Returns the number of messaging connections.
     */
    public static int getNumConnections() {
		return manager.getNumConnections();
    }

    /**
     *  Returns the number of initialized messaging connections.
     */
    public static int getNumInitializedConnections() {
		return manager.getNumInitializedConnections();
    }
    
    /**
     * Returns the number of active ultrapeer -> leaf connections.
     */
    public static int getNumUltrapeerToLeafConnections() {
        return manager.getNumInitializedClientConnections();
    }
    
    /**
     * Returns the number of leaf -> ultrapeer connections.
     */
    public static int getNumLeafToUltrapeerConnections() {
        return manager.getNumClientSupernodeConnections();
    }
    
    /**
     * Returns the number of ultrapeer -> ultrapeer connections.
     */
    public static int getNumUltrapeerToUltrapeerConnections() {
        return manager.getNumUltrapeerConnections();
    }
    
    /**
     * Returns the number of old unrouted connections.
     */
    public static int getNumOldConnections() {
        return manager.getNumOldConnections();
    }
    
	/**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isFullyConnected() {
		return manager.isFullyConnected();
	}    

	/**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public static boolean isConnected() {
		return manager.isConnected();
	}
	
	/**
	 * Returns whether or not this client is attempting to connect.
	 */
	public static boolean isConnecting() {
	    return manager.isConnecting();
	}
	
	/**
	 * Returns whether or not this client is currently fetching
	 * endpoints from a GWebCache.
	 *
	 * @return <tt>true</tt> if the client is fetching endpoints.
	 */
	public static boolean isFetchingEndpoints() {
	    return BootstrapServerManager.instance().isEndpointFetchInProgress();
    }

    /**
     * Returns the number of files being shared locally.
     */
    public static int getNumSharedFiles( ) {
        return( fileManager.getNumFiles() );
    }
    
    /**
     * Returns the number of files which are awaiting sharing.
     */
    public static int getNumPendingShared() {
        return( fileManager.getNumPendingFiles() );
    }

	/**
	 * Returns the size in bytes of shared files.
	 *
	 * @return the size in bytes of shared files on this host
	 */
	public static int getSharedFileSize() {
		return fileManager.getSize();
	}
	
	/** 
	 * Returns a list of all incomplete shared file descriptors.
	 */
	public static FileDesc[] getIncompleteFileDescriptors() {
	    return fileManager.getIncompleteFileDescriptors();
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
        if( directory == null )
            return fileManager.getAllSharedFileDescriptors();
        else
            return fileManager.getSharedFileDescriptors(directory);
    }
    
    /** 
     * Tries to "smart download" <b>any</b> [sic] of the given files.<p>  
     *
     * If any of the files already being downloaded (or queued for downloaded)
     * has the same temporary name as any of the files in 'files', throws
     * AlreadyDownloadingException.  Note, however, that this doesn't guarantee
     * that a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==false, then if any of the files already exists in the
     * download directory, FileExistsException is thrown and no files are
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
     * @param overwrite true iff the download should proceded without
     *  checking if it's on disk
     * @param the guid of the query that returned the results (i.e. files)
     * @return the download object you can use to start and resume the download
     * @exception AlreadyDownloadingException the file is already being 
     *  downloaded.
     * @exception FileExistsException the file already exists in the library
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
	public static Downloader download(RemoteFileDesc[] files, 
	                                  List alts,
	                                  boolean overwrite,
                                      GUID queryGUID)
		throws FileExistsException, AlreadyDownloadingException, 
  			   java.io.FileNotFoundException {
		return downloader.download(files, alts, overwrite, queryGUID);
	}
	
	/**
	 * Stub for calling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
	 */
	public static Downloader download(RemoteFileDesc[] files,
                                      boolean overwrite, GUID queryGUID)
		throws FileExistsException, AlreadyDownloadingException, 
  			   java.io.FileNotFoundException {
		return download(files, Collections.EMPTY_LIST, overwrite, queryGUID);
	}
        

    /*
     * Creates a new MAGNET downloader.  Immediately tries to download from
     * <tt>defaultURL</tt>, if specified.  If that fails, or if defaultURL does
     * not provide alternate locations, issues a requery with <tt>textQuery</tt>
     * and </tt>urn</tt>, as provided.  (At least one must be non-null.)  If
     * <tt>filename</tt> is specified, it will be used as the name of the
     * complete file; otherwise it will be taken from any search results or
     * guessed from <tt>defaultURL</tt>.
     *
     * @param urn the hash of the file (exact topic), or null if unknown
     * @param textQuery requery keywords (keyword topic), or null if unknown
     * @param filename the final file name, or null if unknown
     * @param defaultURLs the initial locations to try (exact source), or null 
     *  if unknown
     *
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception IllegalArgumentException both urn and textQuery are null 
     */
    public static synchronized Downloader download(URN urn, String textQuery,
            String filename, String [] defaultURL, boolean overwrite) 
            throws IllegalArgumentException, AlreadyDownloadingException, 
                                                          FileExistsException { 
        return downloader.download(urn,textQuery,filename,defaultURL,overwrite);
    }

   /**
     * Starts a resume download for the given incomplete file.
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     */ 
    public static Downloader download(File incompleteFile)
            throws AlreadyDownloadingException, CantResumeException {
        return downloader.download(incompleteFile);
    }

	/**
	 * Creates and returns a new chat to the given host and port.
	 */
	public static Chatter createChat(String host, int port) {
		Chatter chatter = ChatManager.instance().request(host, port);
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
	  final String host, final int port, GUID guid, GUID serventID, 
	  final Set proxies, final boolean canDoFWTransfer) {
        final BrowseHostHandler handler = new BrowseHostHandler(callback, 
                                                          guid, serventID);
        Thread asynch = new ManagedThread( new Runnable() {
            public void run() {
                try {
                    handler.browseHost(host, port, proxies, canDoFWTransfer);
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        }, "BrowseHoster" );
        asynch.setDaemon(true);
        asynch.start();
        
        return handler;
	}

    /**
     * Tells whether the node is a supernode or not
     * @return true, if supernode, false otherwise
     */
    public static boolean isSupernode() {
        return manager.isSupernode();
    }

	/**
	 * Accessor for whether or not this node is a shielded leaf.
	 *
	 * @return <tt>true</tt> if this node is a shielded leaf, 
	 *  <tt>false</tt> otherwise
	 */
    public static boolean isShieldedLeaf() {
        return manager.isShieldedLeaf();
    }    


    /**
     * @return the number of free leaf slots.
     */
    public static int getNumFreeLeafSlots() {
            return manager.getNumFreeLeafSlots();
    }

    
    /**
     * @return the number of free non-leaf slots.
     */
    public static int getNumFreeNonLeafSlots() {
        return manager.getNumFreeNonLeafSlots();
    }

    /**
     * @return the number of free leaf slots available for limewires.
     */
    public static int getNumFreeLimeWireLeafSlots() {
            return manager.getNumFreeLimeWireLeafSlots();
    }

    
    /**
     * @return the number of free non-leaf slots available for limewires.
     */
    public static int getNumFreeLimeWireNonLeafSlots() {
        return manager.getNumFreeLimeWireNonLeafSlots();
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
    
    /**
     * Notifies components that this' IP address has changed.
     */
    public static boolean addressChanged() {
        // Only continue if the current address/port is valid & not private.
        byte addr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isValidAddress(addr))
            return false;
        if(NetworkUtils.isPrivateAddress(addr))
            return false;            
        if(!NetworkUtils.isValidPort(port))
            return false;

        FileDesc[] fds = fileManager.getAllSharedFileDescriptors();
        for(int i = 0; i < fds.length; i++)
            fds[i].addUrnsForSelf();

        // reset the last connect back time so the next time the TCP/UDP
        // validators run they try to connect back.
        if (acceptor != null)
        	acceptor.resetLastConnectBackTime();
        if (udpService != null)
        	udpService.resetLastConnectBackTime();
        
        if (manager != null) {
        	Properties props = new Properties();
        	props.put(HeaderNames.LISTEN_IP,NetworkUtils.ip2string(addr)+":"+port);
        	HeaderUpdateVendorMessage huvm = new HeaderUpdateVendorMessage(props);
        	
        	for (Iterator iter = manager.getInitializedConnections().iterator();iter.hasNext();) {
        		ManagedConnection c = (ManagedConnection)iter.next();
        		if (c.remoteHostSupportsHeaderUpdate() >= huvm.VERSION)
        			c.send(huvm);
        	}
        	
        	for (Iterator iter = manager.getInitializedClientConnections().iterator();iter.hasNext();) {
        		ManagedConnection c = (ManagedConnection)iter.next();
        		if (c.remoteHostSupportsHeaderUpdate() >= huvm.VERSION)
        			c.send(huvm);
        	}
        }
        return true;
    }
    
    /**
     * Returns the external IP address for this host.
     */
    public static byte[] getExternalAddress() {
        return acceptor.getExternalAddress();
    }

	/**
	 * Returns the raw IP address for this host.
	 *
	 * @return the raw IP address for this host
	 */
	public static byte[] getAddress() {
		return acceptor.getAddress(true);
	}
	
	/**
	 * Returns the Non-Forced IP address for this host.
	 *
	 * @return the non-forced IP address for this host
	 */
	public static byte[] getNonForcedAddress() {
	    return acceptor.getAddress(false);
	}
	

    /**
     * Returns the port used for downloads and messaging connections.
     * Used to fill out the My-Address header in ManagedConnection.
     * @see Acceptor#getPort
     */    
	public static int getPort() {
		return acceptor.getPort(true);
	}
	
    /**
	 * Returns the Non-Forced port for this host.
	 *
	 * @return the non-forced port for this host
	 */
	public static int getNonForcedPort() {
	    return acceptor.getPort(false);
	}

	/**
	 * Returns whether or not this node is capable of sending its own
	 * GUESS queries.  This would not be the case only if this node
	 * has not successfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is capable of running its own
	 *  GUESS queries, <tt>false</tt> otherwise
	 */
	public static boolean isGUESSCapable() {
		return udpService.isGUESSCapable();
	}


    /** 
     * Returns whether or not this node is capable of performing OOB queries.
     */
    public static boolean isOOBCapable() {
        return isGUESSCapable() && OutOfBandThroughputStat.isSuccessRateGood()&&
               !NetworkUtils.isPrivate() &&
               SearchSettings.OOB_ENABLED.getValue() &&
               acceptor.isAddressExternal() && isIpPortValid();
    }


    public static GUID getUDPConnectBackGUID() {
        return udpService.getConnectBackGUID();
    }

    
    /** @return true if your IP and port information is valid.
     */
    public static boolean isIpPortValid() {
        return (NetworkUtils.isValidAddress(getAddress()) &&
                NetworkUtils.isValidPort(getPort()));
    }
    
    public static boolean canReceiveSolicited() {
    	return udpService.canReceiveSolicited();
    }
    
    public static boolean canReceiveUnsolicited() {
    	return udpService.canReceiveUnsolicited();
    }
    
    public static boolean canDoFWT() {
        return udpService.canDoFWT();
    }
}
