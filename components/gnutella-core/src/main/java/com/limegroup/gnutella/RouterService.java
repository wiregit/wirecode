pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.io.IOException;
import jbva.net.InetAddress;
import jbva.net.UnknownHostException;
import jbva.util.Arrays;
import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Properties;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.altlocs.AltLocManager;
import com.limegroup.gnutellb.bootstrap.BootstrapServerManager;
import com.limegroup.gnutellb.browser.HTTPAcceptor;
import com.limegroup.gnutellb.browser.MagnetOptions;
import com.limegroup.gnutellb.chat.ChatManager;
import com.limegroup.gnutellb.chat.Chatter;
import com.limegroup.gnutellb.downloader.CantResumeException;
import com.limegroup.gnutellb.downloader.HTTPDownloader;
import com.limegroup.gnutellb.downloader.IncompleteFileManager;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.filters.MutableGUIDFilter;
import com.limegroup.gnutellb.filters.SpamFilter;
import com.limegroup.gnutellb.handshaking.HeaderNames;
import com.limegroup.gnutellb.licenses.LicenseFactory;
import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutellb.search.QueryDispatcher;
import com.limegroup.gnutellb.search.SearchResultHandler;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.FilterSettings;
import com.limegroup.gnutellb.settings.SearchSettings;
import com.limegroup.gnutellb.settings.SettingsHandler;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.settings.SimppSettingsManager;
import com.limegroup.gnutellb.simpp.SimppManager;
import com.limegroup.gnutellb.statistics.OutOfBandThroughputStat;
import com.limegroup.gnutellb.tigertree.TigerTreeCache;
import com.limegroup.gnutellb.udpconnect.UDPMultiplexor;
import com.limegroup.gnutellb.updates.UpdateManager;
import com.limegroup.gnutellb.upelection.PromotionManager;
import com.limegroup.gnutellb.uploader.NormalUploadState;
import com.limegroup.gnutellb.util.IpPortSet;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.SimpleTimer;
import com.limegroup.gnutellb.version.UpdateHandler;
import com.limegroup.gnutellb.xml.MetaFileManager;


/**
 * A fbcade for the entire LimeWire backend.  This is the GUI's primary way of
 * communicbting with the backend.  RouterService constructs the backend 
 * components.  Typicbl use is as follows:
 *
 * <pre>
 * RouterService rs = new RouterService(ActivityCbllback);
 * rs.stbrt();
 * rs.query(...);
 * rs.downlobd(...);
 * rs.shutdown();
 * </pre>
 *
 * The methods of this clbss are numerous, but they tend to fall into one of the
 * following cbtegories:
 *
 * <ul> 
 * <li><b>Connecting bnd disconnecting</b>: connect, disconnect,
 *     connectToHostBlocking, connectToHostAsynchronously, 
 *     connectToGroup, removeConnection, getNumConnections
 * <li><b>Sebrching and downloading</b>: query, browse, score, matchesType,
 *     isMbndragoreWorm, download
 * <li><b>Notificbtion of SettingsManager changes</b>:
 *     setKeepAlive, setListeningPort, bdjustSpamFilters, refreshBannedIPs
 * <li><b>HostCbtcher and horizon</b>: clearHostCatcher, getHosts, removeHost,
 *     getNumHosts, getNumFiles, getTotblFileSize, setAlwaysNotifyKnownHost,
 *     updbteHorizon.  <i>(HostCatcher has changed dramatically on
 *     pong-cbching-branch and query-routing3-branch of CVS, so these methods
 *     will probbbly be obsolete in the future.)</i>
 * <li><b>Stbtistics</b>: getNumLocalSearches, getNumSharedFiles, 
 *      getTotblMessages, getTotalDroppedMessages, getTotalRouteErrors,
 *      getNumPendingShbred
 * </ul> 
 */
public clbss RouterService {
    
    privbte static final Log LOG = LogFactory.getLog(RouterService.class);

	/**
	 * <tt>FileMbnager</tt> instance that manages access to shared files.
	 */
    privbte static FileManager fileManager = new MetaFileManager();

	/**
	 * Timer similbr to java.util.Timer, which was not available on 1.1.8.
	 */
    privbte static final SimpleTimer timer = new SimpleTimer(true);

	/**
	 * <tt>Acceptor</tt> instbnce for accepting new connections, HTTP
	 * requests, etc.
	 */
    privbte static final Acceptor acceptor = new Acceptor();

    /**
     * <tt>HTTPAcceptor</tt> instbnce for accepting magnet requests, etc.
     */
    privbte static HTTPAcceptor httpAcceptor;

	/**
	 * Initiblize the class that manages all TCP connections.
	 */
    privbte static ConnectionManager manager = new ConnectionManager();

	/**
	 * <tt>HostCbtcher</tt> that handles Gnutella pongs.  Only not final
     * for tests.
	 */
    privbte static HostCatcher catcher = new HostCatcher();
	
	/**
	 * <tt>DownlobdManager</tt> for handling HTTP downloading.
	 */
    privbte static DownloadManager downloader = new DownloadManager();

	/**
	 * <tt>UplobdManager</tt> for handling HTTP uploading.
	 */
    privbte static UploadManager uploadManager = new UploadManager();
    
    /**
     * <tt>PushMbnager</tt> for handling push requests.
     */
    privbte static PushManager pushManager = new PushManager();
    
    /**
     * <tt>PromotionMbnager</tt> for handling promotions to Ultrapeer.
     */
    privbte static PromotionManager promotionManager = new PromotionManager();

	
    privbte static final ResponseVerifier VERIFIER = new ResponseVerifier();

	/**
	 * <tt>Stbtistics</tt> class for managing statistics.
	 */
	privbte static final Statistics STATISTICS = Statistics.instance();

	/**
	 * Constbnt for the <tt>UDPService</tt> instance that handles UDP 
	 * messbges.
	 */
	privbte static final UDPService UDPSERVICE = UDPService.instance();

	/**
	 * Constbnt for the <tt>SearchResultHandler</tt> class that processes
	 * sebrch results sent back to this client.
	 */
	privbte static final SearchResultHandler RESULT_HANDLER =
		new SebrchResultHandler();

    /**
     * The mbnager of altlocs
     */
    privbte static AltLocManager altManager = AltLocManager.instance();
    
    /**
     * isShuttingDown flbg
     */
    privbte static boolean isShuttingDown;

	/**
	 * Vbriable for the <tt>ActivityCallback</tt> instance.
	 */
    privbte static ActivityCallback callback;

	/**
	 * Vbriable for the <tt>MessageRouter</tt> that routes Gnutella
	 * messbges.
	 */
    privbte static MessageRouter router;
    
    /**
     * A list of items thbt require running prior to shutting down LW.
     */
    privbte static final List SHUTDOWN_ITEMS = 
        Collections.synchronizedList(new LinkedList());

    /**
     * Vbriable for whether or not that backend threads have been started.
     * 0 - nothing stbrted
     * 1 - pre/while gui tbsks started
     * 2 - everything stbrted
     * 3 - shutting down
     * 4 - shut down
     */
    privbte static volatile int _state;


	/**
	 * Long for the lbst time this host originated a query.
	 */
	privbte static long _lastQueryTime = 0L;
	
	/**
	 * Whether or not we bre running at full power.
	 */
	privbte static boolean _fullPower = true;
	
	privbte static final byte [] MYGUID;
	stbtic {
	    byte [] myguid=null;
	    try {
	        myguid = GUID.fromHexString(ApplicbtionSettings.CLIENT_ID.getValue());
	    }cbtch(IllegalArgumentException iae) {
	        myguid = GUID.mbkeGuid();
	        ApplicbtionSettings.CLIENT_ID.setValue((new GUID(myguid)).toHexString());
	    }
	    MYGUID=myguid;
	}

	/**
	 * Crebtes a new <tt>RouterService</tt> instance.  This fully constructs 
	 * the bbckend.
	 *
	 * @pbram callback the <tt>ActivityCallback</tt> instance to use for
	 *  mbking callbacks
	 */
  	public RouterService(ActivityCbllback callback) {
        this(cbllback, new StandardMessageRouter());
    }

    /**
     * Constructor for the Peer Server.
     */ 
    public RouterService(ActivityCbllback ac, MessageRouter mr, FileManager fm){
        this(bc,mr);
        RouterService.fileMbnager = fm;
    }

	/**
	 * Crebtes a new <tt>RouterService</tt> instance with special message
     * hbndling code.  Typically this constructor is only used for testing.
	 *
	 * @pbram callback the <tt>ActivityCallback</tt> instance to use for
	 *  mbking callbacks
     * @pbram router the <tt>MessageRouter</tt> instance to use for handling
     *  bll messages
	 */
  	public RouterService(ActivityCbllback callback, MessageRouter router) {
		RouterService.cbllback = callback;
        fileMbnager.registerFileManagerEventListener(callback);
  		RouterService.router = router;
  	}

  	/**
  	 * Performs stbrtup tasks that should happen while the GUI loads
  	 */
  	public stbtic void asyncGuiInit() {
  		
  		synchronized(RouterService.clbss) {
  			if (_stbte > 0) // already did this?
  				return;
  			else
  				_stbte = 1;
  		}
  		
  	    Threbd t = new ManagedThread(new Initializer());
  	    t.setNbme("async gui initializer");
  	    t.setDbemon(true);
  	    t.stbrt();
  	}
  	
  	/**
  	 * performs the tbsks usually run while the gui is initializing synchronously
  	 * to be used for tests bnd when running only the core
  	 */
  	public stbtic void preGuiInit() {
  		
  		synchronized(RouterService.clbss) {
  			if (_stbte > 0) // already did this?
  				return;
  			else
  				_stbte = 1;
  		}
  		
  	    (new Initiblizer()).run();
  	}
  	
  	privbte static class Initializer implements Runnable {
  	    public void run() {
  	        //bdd more while-gui init tasks here
  	        RouterService.getAcceptor().init();
  	    }
  	}
  	
	/**
	 * Stbrts various threads and tasks once all core classes have
	 * been constructed.
	 */
	public void stbrt() {
	    synchronized(RouterService.clbss) {
    	    LOG.trbce("START RouterService");
    	    
    	    if ( isStbrted() )
    	        return;
    	        
            preGuiInit();
            _stbte = 2;
    
    		// Now, link bll the pieces together, starting the various threads.

            //Note: SimppMbnager and SimppSettingsManager must go first to make
            //sure bll the settings are created with the simpp values. Other
            //components mby rely on the settings, so they must have the right
            //vblues when they are being initialized.
            LOG.trbce("START SimppManager.instance");
            cbllback.componentLoading("SIMPP_MANAGER");
            SimppMbnager.instance();//initialize
            LOG.trbce("STOP SimppManager.instance");
            
            LOG.trbce("START SimppSettingsManager.instance");
            SimppSettingsMbnager.instance();
            LOG.trbce("STOP SimppSettingsManager.instance");

            LOG.trbce("START MessageRouter");
            cbllback.componentLoading("MESSAGE_ROUTER");
    		router.initiblize();
    		LOG.trbce("STOPMessageRouter");
    		
            LOG.trbce("START Acceptor");
            cbllback.componentLoading("ACCEPTOR");
    		bcceptor.start();
    		LOG.trbce("STOP Acceptor");
    		
    		LOG.trbce("START ConnectionManager");
    		cbllback.componentLoading("CONNECTION_MANAGER");
    		mbnager.initialize();
    		LOG.trbce("STOP ConnectionManager");
    		
    		LOG.trbce("START DownloadManager");
    		downlobder.initialize(); 
    		LOG.trbce("STOP DownloadManager");
    		
    		LOG.trbce("START SupernodeAssigner");
    		SupernodeAssigner sb = new SupernodeAssigner(uploadManager, 
    													 downlobder, 
    													 mbnager);
    		sb.start();
    		LOG.trbce("STOP SupernodeAssigner");
    
            // THIS MUST BE BEFORE THE CONNECT (below)
            // OTHERWISE WE WILL ALWAYS CONNECT TO GWEBCACHES
            LOG.trbce("START HostCatcher.initialize");
            cbllback.componentLoading("HOST_CATCHER");
    		cbtcher.initialize();
    		LOG.trbce("STOP HostCatcher.initialize");
    
    		if(ConnectionSettings.CONNECT_ON_STARTUP.getVblue()) {
    			// Mbke sure connections come up ultra-fast (beyond default keepAlive)		
    			int outgoing = ConnectionSettings.NUM_CONNECTIONS.getVblue();
    			if ( outgoing > 0 ) {
    			    LOG.trbce("START connect");
    				connect();
                    LOG.trbce("STOP connect");
                }
    		}
            // Asynchronously lobd files now that the GUI is up, notifying
            // cbllback.
            LOG.trbce("START FileManager");
            cbllback.componentLoading("FILE_MANAGER");
            fileMbnager.start();
            LOG.trbce("STOP FileManager");
    
            // Restore bny downloads in progress.
            LOG.trbce("START DownloadManager.postGuiInit");
            cbllback.componentLoading("DOWNLOAD_MANAGER_POST_GUI");
            downlobder.postGuiInit();
            LOG.trbce("STOP DownloadManager.postGuiInit");
            
            LOG.trbce("START UpdateManager.instance");
            cbllback.componentLoading("UPDATE_MANAGER");
            UpdbteManager.instance();
            UpdbteHandler.instance();
            LOG.trbce("STOP UpdateManager.instance");

            LOG.trbce("START QueryUnicaster");
            cbllback.componentLoading("QUERY_UNICASTER");
    		QueryUnicbster.instance().start();
    		LOG.trbce("STOP QueryUnicaster");
    		
    		LOG.trbce("START HTTPAcceptor");
            cbllback.componentLoading("HTTPACCEPTOR");
            httpAcceptor = new HTTPAcceptor();  
            httpAcceptor.stbrt();
            LOG.trbce("STOP HTTPAcceptor");
            
            LOG.trbce("START Pinger");
            cbllback.componentLoading("PINGER");
            Pinger.instbnce().start();
            LOG.trbce("STOP Pinger");
            
            LOG.trbce("START ConnectionWatchdog");
            cbllback.componentLoading("CONNECTION_WATCHDOG");
            ConnectionWbtchdog.instance().start();
            LOG.trbce("STOP ConnectionWatchdog");
            
            LOG.trbce("START SavedFileManager");
            cbllback.componentLoading("SAVED_FILE_MANAGER");
            SbvedFileManager.instance();
            LOG.trbce("STOP SavedFileManager");
            
            if(ApplicbtionSettings.AUTOMATIC_MANUAL_GC.getValue())
                stbrtManualGCThread();
            
            LOG.trbce("STOP RouterService.");
        }
	}
	
	/**
	 * Stbrts a manual GC thread.
	 */
	privbte void startManualGCThread() {
	    Threbd t = new ManagedThread(new Runnable() {
	        public void run() {
	            while(true) {
	                try {
	                    Threbd.sleep(5 * 60 * 1000);
	                } cbtch(InterruptedException ignored) {}
	                LOG.trbce("Running GC");
	                System.gc();
	                LOG.trbce("GC finished, running finalizers");
	                System.runFinblization();
	                LOG.trbce("Finalizers finished.");
                }
            }
        }, "MbnualGC");
        t.setDbemon(true);
        t.stbrt();
        LOG.trbce("Started manual GC thread.");
    }
	                

    /**
     * Used to determine whether or not the bbckend threads have been
     * stbrted.
     *
     * @return <tt>true</tt> if the bbckend threads have been started,
     *  otherwise <tt>fblse</tt>
     */
    public stbtic boolean isStarted() {
        return _stbte >= 2;
    }

    /**
     * Returns the <tt>ActivityCbllback</tt> passed to this' constructor.
	 *
	 * @return the <tt>ActivityCbllback</tt> passed to this' constructor --
	 *  this is one of the few bccessors that can be <tt>null</tt> -- this 
	 *  will be <tt>null</tt> in the cbse where the <tt>RouterService</tt>
	 *  hbs not been constructed
     */ 
    public stbtic ActivityCallback getCallback() {
        return RouterService.cbllback;
    }
    
    /**
     * Sets full power mode.
     */
    public stbtic void setFullPower(boolean newValue) {
        if(_fullPower != newVblue) {
            _fullPower = newVblue;
            NormblUploadState.setThrottleSwitching(!newValue);
            HTTPDownlobder.setThrottleSwitching(!newValue);
        }
    }

	/**
	 * Accessor for the <tt>MessbgeRouter</tt> instance.
	 *
	 * @return the <tt>MessbgeRouter</tt> instance in use --
	 *  this is one of the few bccessors that can be <tt>null</tt> -- this 
	 *  will be <tt>null</tt> in the cbse where the <tt>RouterService</tt>
	 *  hbs not been constructed
	 */
	public stbtic MessageRouter getMessageRouter() {
		return router;
	}
    
	/**
	 * Accessor for the <tt>FileMbnager</tt> instance in use.
	 *
	 * @return the <tt>FileMbnager</tt> in use
	 */
    public stbtic FileManager getFileManager(){
        return fileMbnager;
    }

    /** 
     * Accessor for the <tt>DownlobdManager</tt> instance in use.
     *
     * @return the <tt>DownlobdManager</tt> in use
     */
    public stbtic DownloadManager getDownloadManager() {
        return downlobder;
    }

    public stbtic AltLocManager getAltlocManager() {
        return bltManager;
    }
    
	/**
	 * Accessor for the <tt>UDPService</tt> instbnce.
	 *
	 * @return the <tt>UDPService</tt> instbnce in use
	 */
	public stbtic UDPService getUdpService() {
		return UDPSERVICE;
	}
	
	/**
	 * Gets the UDPMultiplexor.
	 */
	public stbtic UDPMultiplexor getUDPConnectionManager() {
	    return UDPMultiplexor.instbnce();
	}

	/**
	 * Accessor for the <tt>ConnectionMbnager</tt> instance.
	 *
	 * @return the <tt>ConnectionMbnager</tt> instance in use
	 */
	public stbtic ConnectionManager getConnectionManager() {
		return mbnager;
	}
	
    /** 
     * Accessor for the <tt>UplobdManager</tt> instance.
     *
     * @return the <tt>UplobdManager</tt> in use
     */
	public stbtic UploadManager getUploadManager() {
		return uplobdManager;
	}
	
	/**
	 * Accessor for the <tt>PushMbnager</tt> instance.
	 *
	 * @return the <tt>PushMbnager</tt> in use
	 */
	public stbtic PushManager getPushManager() {
	    return pushMbnager;
	}
	
    /** 
     * Accessor for the <tt>Acceptor</tt> instbnce.
     *
     * @return the <tt>Acceptor</tt> in use
     */
	public stbtic Acceptor getAcceptor() {
		return bcceptor;
	}

    /** 
     * Accessor for the <tt>Acceptor</tt> instbnce.
     *
     * @return the <tt>Acceptor</tt> in use
     */
    public stbtic HTTPAcceptor getHTTPAcceptor() {
        return httpAcceptor;
    }

    /** 
     * Accessor for the <tt>HostCbtcher</tt> instance.
     *
     * @return the <tt>HostCbtcher</tt> in use
     */
	public stbtic HostCatcher getHostCatcher() {
		return cbtcher;
	}

    /** 
     * Accessor for the <tt>SebrchResultHandler</tt> instance.
     *
     * @return the <tt>SebrchResultHandler</tt> in use
     */
	public stbtic SearchResultHandler getSearchResultHandler() {
		return RESULT_HANDLER;
	}
	
	/**
	 * Accessor for the <tt>PromotionMbnager</tt> instance.
	 * @return the <tt>PromotionMbnager</tt> in use.
	 */
	public stbtic PromotionManager getPromotionManager() {
		return promotionMbnager;
	}
	
	public stbtic byte [] getMyGUID() {
	    return MYGUID;
	}

    /**
     * Schedules the given tbsk for repeated fixed-delay execution on this's
     * bbckend thread.  <b>The task must not block for too long</b>, as 
     * b single thread is shared among all the backend.
     *
     * @pbram task the task to run repeatedly
     * @pbram delay the initial delay, in milliseconds
     * @pbram period the delay between executions, in milliseconds
     * @exception IllegblStateException this is cancelled
     * @exception IllegblArgumentException delay or period negative
     * @see com.limegroup.gnutellb.util.SimpleTimer#schedule(java.lang.Runnable,long,long)
     */
    public stbtic void schedule(Runnable task, long delay, long period) {
        timer.schedule(tbsk, delay, period);
    }

    /**
     * Crebtes a new outgoing messaging connection to the given host and port.
     * Blocks until the connection estbblished.  Throws IOException if
     * the connection fbiled.
     * @return b connection to the request host
     * @exception IOException the connection fbiled
     */
    public stbtic ManagedConnection connectToHostBlocking(String hostname, int portnum)
		throws IOException {
        return mbnager.createConnectionBlocking(hostname, portnum);
    }

    /**
     * Crebtes a new outgoing messaging connection to the given host and port. 
     * Returns immedibtely without blocking.  If hostname would connect
     * us to ourselves, returns immedibtely.
     */
    public stbtic void connectToHostAsynchronously(String hostname, int portnum) {
        //Don't bllow connections to yourself.  We have to special
        //cbse connections to "localhost" or "127.0.0.1" since
        //they bre aliases for this machine.
		
        byte[] cIP = null;
        InetAddress bddr;
        try {
            bddr = InetAddress.getByName(hostname);
            cIP = bddr.getAddress();
        } cbtch(UnknownHostException e) {
            return;
        }
        if ((cIP[0] == 127) && (portnum==bcceptor.getPort(true)) &&
			ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) {
			return;
        } else {
            byte[] mbnagerIP=acceptor.getAddress(true);
            if (Arrbys.equals(cIP, managerIP)
                && portnum==bcceptor.getPort(true))
                return;
        }

        if (!bcceptor.isBannedIP(cIP)) {
            mbnager.createConnectionAsynchronously(hostname, portnum);
		}
    }
    
    /**
     * Determines if you're connected to the given host.
     */
    public stbtic boolean isConnectedTo(InetAddress addr) {
        // ideblly we would check download sockets too, but
        // becbuse of the way ManagedDownloader is built, it isn't
        // too prbctical.
        // TODO: rewrite MbnagedDownloader
        
        String host = bddr.getHostAddress();
        return mbnager.isConnectedTo(host) ||
               UDPMultiplexor.instbnce().isConnectedTo(addr) ||
               uplobdManager.isConnectedTo(addr); // ||
               // dlobdManager.isConnectedTo(addr);
    }

    /**
     * Connects to the network.  Ensures the number of messbging connections
     * (keep-blive) is non-zero and recontacts the pong server as needed.  
     */
    public stbtic void connect() {
        bdjustSpamFilters();
        
        //delegbte to connection manager
        mbnager.connect();
    }

    /**
     * Disconnects from the network.  Closes bll connections and sets
     * the number of connections to zero.
     */
    public stbtic void disconnect() {
		// Delegbte to connection manager
		mbnager.disconnect();
    }

    /**
     * Closes bnd removes the given connection.
     */
    public stbtic void removeConnection(ManagedConnection c) {
        mbnager.remove(c);
    }

    /**
     * Clebrs the hostcatcher.
     */
    public stbtic void clearHostCatcher() {
        cbtcher.clear();
    }

    /**
     * Returns the number of pongs in the host cbtcher.  <i>This method is
     * poorly nbmed, but it's obsolescent, so I won't bother to rename it.</i>
     */
    public stbtic int getRealNumHosts() {
        return(cbtcher.getNumHosts());
    }

    /**
     * Returns the number of downlobds in progress.
     */
    public stbtic int getNumDownloads() {
        return downlobder.downloadsInProgress();
    }
    
    /**
     * Returns the number of bctive downloads.
     */
    public stbtic int getNumActiveDownloads() {
        return downlobder.getNumActiveDownloads();
    }
    
    /**
     * Returns the number of downlobds waiting to be started.
     */
    public stbtic int getNumWaitingDownloads() {
        return downlobder.getNumWaitingDownloads();
    }
    
    /**
     * Returns the number of individubl downloaders.
     */
    public stbtic int getNumIndividualDownloaders() {
        return downlobder.getNumIndividualDownloaders();
    }
    
    /**
     * Returns the number of uplobds in progress.
     */
    public stbtic int getNumUploads() {
        return uplobdManager.uploadsInProgress();
    }

    /**
     * Returns the number of queued uplobds.
     */
    public stbtic int getNumQueuedUploads() {
        return uplobdManager.getNumQueuedUploads();
    }
    
    /**
     * Returns the current uptime.
     */
    public stbtic long getCurrentUptime() {
        return STATISTICS.getUptime();
    }
    
    /**
     * Adds something thbt requires shutting down.
     *
     * TODO: Mbke this take a 'Service' or somesuch that
     *       hbs a shutdown method, and run the method in its
     *       own threbd.
     */
    public stbtic boolean addShutdownItem(Thread t) {
        if(isShuttingDown() || isShutdown())
            return fblse;

        SHUTDOWN_ITEMS.bdd(t);
        return true;
    }
    
    /**
     * Runs bll shutdown items.
     */
    privbte static void runShutdownItems() {
        if(!isShuttingDown())
            return;
        
        // Stbrt each shutdown item.
        for(Iterbtor i = SHUTDOWN_ITEMS.iterator(); i.hasNext(); ) {
            Threbd t = (Thread)i.next();
            t.stbrt();
        }
        
        // Now thbt we started them all, iterate back and wait for each one to finish.
        for(Iterbtor i = SHUTDOWN_ITEMS.iterator(); i.hasNext(); ) {
            Threbd t = (Thread)i.next();
            try {
                t.join();
            } cbtch(InterruptedException ie) {}
        }
    }
    
    /**
     * Determines if this is shutting down.
     */
    privbte static boolean isShuttingDown() {
        return _stbte >= 3;
    }
    
    /**
     * Determines if this is shut down.
     */
    privbte static boolean isShutdown() {
        return _stbte >= 4;
    }

    /**
     * Shuts down the bbckend and writes the gnutella.net file.
     *
     * TODO: Mbke all of these things Shutdown Items.
     */
    public stbtic synchronized void shutdown() {
        try {
            if(!isStbrted())
                return;
                
            _stbte = 3;
            
            getAcceptor().shutdown();
            
            //Updbte fractional uptime statistics (before writing limewire.props)
            Stbtistics.instance().shutdown();
            
            //Updbte firewalled status
            ConnectionSettings.EVER_ACCEPTED_INCOMING.setVblue(acceptedIncomingConnection());

            //Write gnutellb.net
            try {
                cbtcher.write();
            } cbtch (IOException e) {}
            
            // sbve limewire.props & other settings
            SettingsHbndler.save();            
            
            clebnupPreviewFiles();
            
            downlobder.writeSnapshot();
            
            fileMbnager.stop();
            
            TigerTreeCbche.instance().persistCache();

            LicenseFbctory.persistCache();
            
            runShutdownItems();
            
            _stbte = 4;
            
        } cbtch(Throwable t) {
            ErrorService.error(t);
        }
    }
    
    public stbtic void shutdown(String toExecute) {
        shutdown();
        if (toExecute != null) {
            try {
                Runtime.getRuntime().exec(toExecute);
            } cbtch (IOException tooBad) {}
        }
    }
    
    /**
     * Deletes bll preview files.
     */
    privbte static void cleanupPreviewFiles() {
        //Clebnup any preview files.  Note that these will not be deleted if
        //your previewer is still open.
        File incompleteDir = ShbringSettings.INCOMPLETE_DIRECTORY.getValue();
        if (incompleteDir == null)
            return; // if we could not get the incomplete directory, simply return.
        
        
        File[] files = incompleteDir.listFiles();
        if(files == null)
            return;
        
        for (int i=0; i<files.length; i++) {
            String nbme = files[i].getName();
            if (nbme.startsWith(IncompleteFileManager.PREVIEW_PREFIX))
                files[i].delete();  //Mby or may not work; ignore return code.
        }
    }

    /**
     * Notifies the bbckend that spam filters settings have changed, and that
     * extrb work must be done.
     */
    public stbtic void adjustSpamFilters() {
        IPFilter.refreshIPFilter();

        //Just replbce the spam filters.  No need to do anything
        //fbncy like incrementally updating them.
        for (Iterbtor iter=manager.getConnections().iterator();
             iter.hbsNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            c.setPersonblFilter(SpamFilter.newPersonalFilter());
            c.setRouteFilter(SpbmFilter.newRouteFilter());
        }
        
        UDPReplyHbndler.setPersonalFilter(SpamFilter.newPersonalFilter());
    }

    /**
     * Sets the port on which to listen for incoming connections.
     * If thbt fails, this is <i>not</i> modified and IOException is thrown.
     * If port==0, tells this to stop listening to incoming connections.
     */
    public stbtic void setListeningPort(int port) throws IOException {
        bcceptor.setListeningPort(port);
    }

    /** 
     * Returns true if this hbs accepted an incoming connection, and hence
     * probbbly isn't firewalled.  (This is useful for colorizing search
     * results in the GUI.)
     */
    public stbtic boolean acceptedIncomingConnection() {
		return bcceptor.acceptedIncoming();
    }

    /**
     * Count up bll the messages on active connections
     */
    public stbtic int getActiveConnectionMessages() {
		int count = 0;

        // Count the messbges on initialized connections
        for (Iterbtor iter=manager.getInitializedConnections().iterator();
             iter.hbsNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            count += c.getNumMessbgesSent();
            count += c.getNumMessbgesReceived();
        }
		return count;
    }

    /**
     * Count how mbny connections have already received N messages
     */
    public stbtic int countConnectionsWithNMessages(int messageThreshold) {
		int count = 0;
		int msgs; 

        // Count the messbges on initialized connections
        for (Iterbtor iter=manager.getInitializedConnections().iterator();
             iter.hbsNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            msgs = c.getNumMessbgesSent();
            msgs += c.getNumMessbgesReceived();
			if ( msgs > messbgeThreshold )
				count++;
        }
		return count;
    }

    /**
     * Prints out the informbtion about current initialied connections
     */
    public stbtic void dumpConnections() {
        //dump ultrbpeer connections
        System.out.println("UltrbPeer connections");
        dumpConnections(mbnager.getInitializedConnections());
        //dump lebf connections
        System.out.println("Lebf connections");
        dumpConnections(mbnager.getInitializedClientConnections());
    }
    
    /**
     * Prints out the pbssed collection of connections
     * @pbram connections The collection(of Connection) 
     * of connections to be printed
     */
    privbte static void dumpConnections(Collection connections)
    {
        for(Iterbtor iterator = connections.iterator(); iterator.hasNext();) {
            System.out.println(iterbtor.next().toString());
        }
    }
    
    /** 
     * Returns b new GUID for passing to query.
     * This method is the centrbl point of decision making for sending out OOB 
     * queries.
     */
    public stbtic byte[] newQueryGUID() {
        if (isOOBCbpable() && OutOfBandThroughputStat.isOOBEffectiveForMe())
            return GUID.mbkeAddressEncodedGuid(getAddress(), getPort());
        else
            return GUID.mbkeGuid();
    }

    /**
     * Sebrches the network for files of the given type with the given
     * GUID, query string bnd minimum speed.  If type is null, any file type
     * is bcceptable.<p>
     *
     * ActivityCbllback is notified asynchronously of responses.  These
     * responses cbn be matched with requests by looking at their GUIDs.  (You
     * mby want to wrap the bytes with a GUID object for simplicity.)  An
     * ebrlier version of this method returned the reply GUID instead of taking
     * it bs an argument.  Unfortunately this caused a race condition where
     * replies were returned before the GUI wbs prepared to handle them.
     * 
     * @pbram guid the guid to use for the query.  MUST be a 16-byte
     *  vblue as returned by newQueryGUID.
     * @pbram query the query string to use
     * @pbram minSpeed the minimum desired result speed
     * @pbram type the desired type of result (e.g., audio, video), or
     *  null if you don't cbre 
     */
    public stbtic void query(byte[] guid, String query, MediaType type) {
		query(guid, query, "", type);
	}

    /** 
     * Sebrches the network for files with the given query string and 
     * minimum speed, i.e., sbme as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, MedibType)
     */
    public stbtic void query(byte[] guid, String query) {
        query(guid, query, null);
    }

	/**
	 * Sebrches the network for files with the given metadata.
	 * 
	 * @pbram richQuery metadata query to insert between the nulls,
	 *  typicblly in XML format
	 * @see query(byte[], String, MedibType)
	 */
	public stbtic void query(final byte[] guid, 
							 finbl String query, 
							 finbl String richQuery, 
							 finbl MediaType type) {

		try {
            QueryRequest qr = null;
            if (isIpPortVblid() && (new GUID(guid)).addressesMatch(getAddress(), 
                                                                   getPort())) {
                // if the guid is encoded with my bddress, mark it as needing out
                // of bbnd support.  note that there is a VERY small chance that
                // the guid will be bddress encoded but not meant for out of band
                // delivery of results.  bbd things may happen in this case but 
                // it seems tremendously unlikely, even over the course of b 
                // VERY long lived client
                qr = QueryRequest.crebteOutOfBandQuery(guid, query, richQuery,
                                                       type);
                OutOfBbndThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = QueryRequest.crebteQuery(guid, query, richQuery, type);
            recordAndSendQuery(qr, type);
		} cbtch(Throwable t) {
			ErrorService.error(t);
		}
	}


	/**
	 * Sends b 'What Is New' query on the network.
	 */
	public stbtic void queryWhatIsNew(final byte[] guid, final MediaType type) {
		try {
            QueryRequest qr = null;
            if (GUID.bddressesMatch(guid, getAddress(), getPort())) {
                // if the guid is encoded with my bddress, mark it as needing out
                // of bbnd support.  note that there is a VERY small chance that
                // the guid will be bddress encoded but not meant for out of band
                // delivery of results.  bbd things may happen in this case but 
                // it seems tremendously unlikely, even over the course of b 
                // VERY long lived client
                qr = QueryRequest.crebteWhatIsNewOOBQuery(guid, (byte)2, type);
                OutOfBbndThroughputStat.OOB_QUERIES_SENT.incrementStat();
            }
            else
                qr = QueryRequest.crebteWhatIsNewQuery(guid, (byte)2, type);

            if(FilterSettings.FILTER_WHATS_NEW_ADULT.getVblue())
                MutbbleGUIDFilter.instance().addGUID(guid);
    
            recordAndSendQuery(qr, type);
		} cbtch(Throwable t) {
			ErrorService.error(t);
		}
	}

    /** Just bggregates some common code in query() and queryWhatIsNew().
     */ 
    privbte static void recordAndSendQuery(final QueryRequest qr, 
                                           finbl MediaType type) {
        _lbstQueryTime = System.currentTimeMillis();
        VERIFIER.record(qr, type);
        RESULT_HANDLER.bddQuery(qr); // so we can leaf guide....
        router.sendDynbmicQuery(qr);
    }

	/**
	 * Accessor for the lbst time a query was originated from this host.
	 *
	 * @return b <tt>long</tt> representing the number of milliseconds since
	 *  Jbnuary 1, 1970, that the last query originated from this host
	 */
	public stbtic long getLastQueryTime() {
		return _lbstQueryTime;
	}

    /** Purges the query from the QueryUnicbster (GUESS) and the ResultHandler
     *  (which mbintains query stats for the purpose of leaf guidance).
     *  @pbram guid The GUID of the query you want to get rid of....
     */
    public stbtic void stopQuery(GUID guid) {
        QueryUnicbster.instance().purgeQuery(guid);
        RESULT_HANDLER.removeQuery(guid);
        router.queryKilled(guid);
        if(RouterService.isSupernode())
            QueryDispbtcher.instance().addToRemove(guid);
        MutbbleGUIDFilter.instance().removeGUID(guid.bytes());
    }

    /** 
     * Returns true if the given response is of the sbme type as the the query
     * with the given guid.  Returns 100 if guid is not recognized.
     *
     * @pbram guid the value returned by query(..).  MUST be 16 bytes long.
     * @pbram resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#mbtchesType(byte[], Response) 
     */
    public stbtic boolean matchesType(byte[] guid, Response response) {
        return VERIFIER.mbtchesType(guid, response);
    }

    public stbtic boolean matchesQuery(byte [] guid, Response response) {
        return VERIFIER.mbtchesQuery(guid, response);
    }
    /** 
     * Returns true if the given response for the query with the given guid is b
     * result of the Mbdragore worm (8KB files of form "x.exe").  Returns false
     * if guid is not recognized.  <i>Ideblly this would be done by the normal
     * filtering mechbnism, but it is not powerful enough without the query
     * string.</i>
     *
     * @pbram guid the value returned by query(..).  MUST be 16 byts long.
     * @pbram resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#isMbndragoreWorm(byte[], Response) 
     */
    public stbtic boolean isMandragoreWorm(byte[] guid, Response response) {
        return VERIFIER.isMbndragoreWorm(guid, response);
    }
    
    /**
     * Returns b collection of IpPorts, preferencing hosts with open slots.
     * If isUltrbpeer is true, this preferences hosts with open ultrapeer slots,
     * otherwise it preferences hosts with open lebf slots.
     *
     * Preferences vib locale, also.
     * 
     * @pbram num How many endpoints to try to get
     */
    public stbtic Collection getPreferencedHosts(boolean isUltrapeer, String locale, int num) {
        
        Set hosts = new IpPortSet();
        
        if(isUltrbpeer)
            hosts.bddAll(catcher.getUltrapeersWithFreeUltrapeerSlots(locale,num));
        else
            hosts.bddAll(catcher.getUltrapeersWithFreeLeafSlots(locale,num));
        
        // If we don't hbve enough hosts, add more.
        
        if(hosts.size() < num) {
            //we first try to get the connections thbt match the locale.
            List conns = mbnager.getInitializedConnectionsMatchLocale(locale);
            for(Iterbtor i = conns.iterator(); i.hasNext() && hosts.size() < num;)
                hosts.bdd(i.next());
            
            //if we still don't hbve enough hosts, get them from the list
            //of bll initialized connection
            if(hosts.size() < num) {
                //list returned is unmmodifibble
                conns = mbnager.getInitializedConnections();
                for(Iterbtor i = conns.iterator(); i.hasNext() && hosts.size() < num;)
                    hosts.bdd(i.next());
            }
        }
        
        return hosts;
    }
    
    /**
     *  Returns the number of messbging connections.
     */
    public stbtic int getNumConnections() {
		return mbnager.getNumConnections();
    }

    /**
     *  Returns the number of initiblized messaging connections.
     */
    public stbtic int getNumInitializedConnections() {
		return mbnager.getNumInitializedConnections();
    }
    
    /**
     * Returns the number of bctive ultrapeer -> leaf connections.
     */
    public stbtic int getNumUltrapeerToLeafConnections() {
        return mbnager.getNumInitializedClientConnections();
    }
    
    /**
     * Returns the number of lebf -> ultrapeer connections.
     */
    public stbtic int getNumLeafToUltrapeerConnections() {
        return mbnager.getNumClientSupernodeConnections();
    }
    
    /**
     * Returns the number of ultrbpeer -> ultrapeer connections.
     */
    public stbtic int getNumUltrapeerToUltrapeerConnections() {
        return mbnager.getNumUltrapeerConnections();
    }
    
    /**
     * Returns the number of old unrouted connections.
     */
    public stbtic int getNumOldConnections() {
        return mbnager.getNumOldConnections();
    }
    
	/**
	 * Returns whether or not this client currently hbs any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does hbve initialized connections,
	 *  <tt>fblse</tt> otherwise
	 */
	public stbtic boolean isFullyConnected() {
		return mbnager.isFullyConnected();
	}    

	/**
	 * Returns whether or not this client currently hbs any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does hbve initialized connections,
	 *  <tt>fblse</tt> otherwise
	 */
	public stbtic boolean isConnected() {
		return mbnager.isConnected();
	}
	
	/**
	 * Returns whether or not this client is bttempting to connect.
	 */
	public stbtic boolean isConnecting() {
	    return mbnager.isConnecting();
	}
	
	/**
	 * Returns whether or not this client is currently fetching
	 * endpoints from b GWebCache.
	 *
	 * @return <tt>true</tt> if the client is fetching endpoints.
	 */
	public stbtic boolean isFetchingEndpoints() {
	    return BootstrbpServerManager.instance().isEndpointFetchInProgress();
    }

    /**
     * Returns the number of files being shbred locally.
     */
    public stbtic int getNumSharedFiles( ) {
        return( fileMbnager.getNumFiles() );
    }
    
    /**
     * Returns the number of files which bre awaiting sharing.
     */
    public stbtic int getNumPendingShared() {
        return( fileMbnager.getNumPendingFiles() );
    }

	/**
	 * Returns the size in bytes of shbred files.
	 *
	 * @return the size in bytes of shbred files on this host
	 */
	public stbtic int getSharedFileSize() {
		return fileMbnager.getSize();
	}
	
	/** 
	 * Returns b list of all incomplete shared file descriptors.
	 */
	public stbtic FileDesc[] getIncompleteFileDescriptors() {
	    return fileMbnager.getIncompleteFileDescriptors();
	}

    /**
     * Returns b list of all shared file descriptors in the given directory.
     * All the file descriptors returned hbve already been passed to the gui
     * vib ActivityCallback.addSharedFile.  Note that if a file descriptor
     * is bdded to the given directory after this method completes, 
     * bddSharedFile will be called for that file descriptor.<p>
     *
     * If directory is not b shared directory, returns null.
     */
    public stbtic FileDesc[] getSharedFileDescriptors(File directory) {
		return fileMbnager.getSharedFileDescriptors(directory);
    }
    
    /** 
     * Tries to "smbrt download" <b>any</b> [sic] of the given files.<p>  
     *
     * If bny of the files already being downloaded (or queued for downloaded)
     * hbs the same temporary name as any of the files in 'files', throws
     * SbveLocationException.  Note, however, that this doesn't guarantee
     * thbt a successfully downloaded file can be moved to the library.<p>
     *
     * If overwrite==fblse, then if any of the files already exists in the
     * downlobd directory, SaveLocationException is thrown and no files are
     * modified.  If overwrite==true, the files mby be overwritten.<p>
     * 
     * Otherwise returns b Downloader that allows you to stop and resume this
     * downlobd.  The ActivityCallback will also be notified of this download,
     * so the return vblue can usually be ignored.  The download begins
     * immedibtely, unless it is queued.  It stops after any of the files
     * succeeds.  
     *
     * @pbram files a group of "similar" files to smart download
     * @pbram alts a List of secondary RFDs to use for other sources
     * @pbram queryGUID guid of the query that returned the results (i.e. files)
	 * @pbram overwrite true iff the download should proceedewithout
     *  checking if it's on disk
	 * @pbram saveDir can be null, then the save directory from the settings
	 * is used
	 * @pbram fileName can be null, then one of the filenames of the 
	 * <code>files</code> brray is used
	 * brray is used
     * @return the downlobd object you can use to start and resume the download
     * @throws SbveLocationException if there is an error when setting the final
     * file locbtion of the download 
     * @see DownlobdManager#getFiles(RemoteFileDesc[], boolean)
     */
	public stbtic Downloader download(RemoteFileDesc[] files, 
	                                  List blts, GUID queryGUID,
                                      boolebn overwrite, File saveDir,
									  String fileNbme)
		throws SbveLocationException {
		return downlobder.download(files, alts, queryGUID, overwrite, saveDir,
								   fileNbme);
	}
	
	public stbtic Downloader download(RemoteFileDesc[] files, 
									  List blts,
									  GUID queryGUID,
									  boolebn overwrite)
		throws SbveLocationException {
		return downlobd(files, alts, queryGUID, overwrite, null, null);
	}	
	
	/**
	 * Stub for cblling download(RemoteFileDesc[], DataUtils.EMPTY_LIST, boolean)
	 * @throws SbveLocationException 
	 */
	public stbtic Downloader download(RemoteFileDesc[] files,
                                      GUID queryGUID, 
                                      boolebn overwrite, File saveDir, String fileName)
		throws SbveLocationException {
		return downlobd(files, Collections.EMPTY_LIST, queryGUID,
				overwrite, sbveDir, fileName);
	}
	
	public stbtic Downloader download(RemoteFileDesc[] files,
									  boolebn overwrite, GUID queryGUID) 
		throws SbveLocationException {
		return downlobd(files, queryGUID, overwrite, null, null);
	}	
        
	/**
	 * Crebtes a downloader for a magnet.
	 * @pbram magnetprovides the information of the  file to download, must be
	 *  vblid
	 * @pbram overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @return
	 * @throws SbveLocationException
	 * @throws IllegblArgumentException if the magnet is not 
	 * {@link MbgnetOptions#isDownloadable() valid}.
	 */
	public stbtic Downloader download(MagnetOptions magnet, boolean overwrite) 
		throws SbveLocationException {
		if (!mbgnet.isDownloadable()) {
			throw new IllegblArgumentException("invalid magnet: not have enough information for downloading");
		}
		return downlobder.download(magnet, overwrite, null, magnet.getDisplayName());
	}

	/**
	 * Crebtes a downloader for a magnet using the given additional options.
	 *
	 * @pbram magnet provides the information of the  file to download, must be
	 *  vblid
	 * @pbram overwrite whether an existing file a the final file location 
	 * should be overwritten
	 * @pbram saveDir can be null, then the save directory from the settings
	 * is used
	 * @pbram fileName the final filename of the download, can be
	 * <code>null</code>
	 * @return
	 * @throws SbveLocationException
	 * @throws IllegblArgumentException if the magnet is not
	 * {@link MbgnetOptions#isDownloadable() downloadable}.
	 */
	public stbtic Downloader download(MagnetOptions magnet, boolean overwrite,
			File sbveDir, String fileName) throws SaveLocationException {
		return downlobder.download(magnet, overwrite, saveDir, fileName);
	}

   /**
     * Stbrts a resume download for the given incomplete file.
     * @exception CbntResumeException incompleteFile is not a valid 
     *  incomplete file
     * @throws SbveLocationException 
     */ 
    public stbtic Downloader download(File incompleteFile)
            throws CbntResumeException, SaveLocationException {
        return downlobder.download(incompleteFile);
    }

	/**
	 * Crebtes and returns a new chat to the given host and port.
	 */
	public stbtic Chatter createChat(String host, int port) {
		Chbtter chatter = ChatManager.instance().request(host, port);
		return chbtter;
	}
    
    /**
	 * Browses the pbssed host
     * @pbram host The host to browse
     * @pbram port The port at which to browse
     * @pbram guid The guid to be used for the query replies received 
     * while browsing host
     * @pbram serventID The guid of the client to browse from.  I need this in
     * cbse I need to push....
     * @pbram proxies the list of PushProxies we can use - may be null.
     * @pbram canDoFWTransfer true if the remote host supports fw transfer
	 */
	public stbtic BrowseHostHandler doAsynchronousBrowseHost(
	  finbl String host, final int port, GUID guid, GUID serventID, 
	  finbl Set proxies, final boolean canDoFWTransfer) {
        finbl BrowseHostHandler handler = new BrowseHostHandler(callback, 
                                                          guid, serventID);
        Threbd asynch = new ManagedThread( new Runnable() {
            public void run() {
                try {
                    hbndler.browseHost(host, port, proxies, canDoFWTransfer);
                } cbtch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        }, "BrowseHoster" );
        bsynch.setDaemon(true);
        bsynch.start();
        
        return hbndler;
	}

    /**
     * Tells whether the node is b supernode or not
     * @return true, if supernode, fblse otherwise
     */
    public stbtic boolean isSupernode() {
        return mbnager.isSupernode();
    }

	/**
	 * Accessor for whether or not this node is b shielded leaf.
	 *
	 * @return <tt>true</tt> if this node is b shielded leaf, 
	 *  <tt>fblse</tt> otherwise
	 */
    public stbtic boolean isShieldedLeaf() {
        return mbnager.isShieldedLeaf();
    }    


    /**
     * @return the number of free lebf slots.
     */
    public stbtic int getNumFreeLeafSlots() {
            return mbnager.getNumFreeLeafSlots();
    }

    
    /**
     * @return the number of free non-lebf slots.
     */
    public stbtic int getNumFreeNonLeafSlots() {
        return mbnager.getNumFreeNonLeafSlots();
    }

    /**
     * @return the number of free lebf slots available for limewires.
     */
    public stbtic int getNumFreeLimeWireLeafSlots() {
            return mbnager.getNumFreeLimeWireLeafSlots();
    }

    
    /**
     * @return the number of free non-lebf slots available for limewires.
     */
    public stbtic int getNumFreeLimeWireNonLeafSlots() {
        return mbnager.getNumFreeLimeWireNonLeafSlots();
    }


    /**
     * Sets the flbg for whether or not LimeWire is currently in the process of 
	 * shutting down.
	 *
     * @pbram flag the shutting down state to set
     */
    public stbtic void setIsShuttingDown(boolean flag) {
		isShuttingDown = flbg;
    }

	/**
	 * Returns whether or not LimeWire is currently in the shutting down stbte,
	 * mebning that a shutdown has been initiated but not completed.  This
	 * is most often the cbse when there are active file transfers and the
	 * bpplication is set to shutdown after current file transfers are complete.
	 *
	 * @return <tt>true</tt> if the bpplication is in the shutting down state,
	 *  <tt>fblse</tt> otherwise
	 */
    public stbtic boolean getIsShuttingDown() {
		return isShuttingDown;
    }
    
    /**
     * Notifies components thbt this' IP address has changed.
     */
    public stbtic boolean addressChanged() {
        if(cbllback != null)
            cbllback.addressStateChanged();        
        
        // Only continue if the current bddress/port is valid & not private.
        byte bddr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isVblidAddress(addr))
            return fblse;
        if(NetworkUtils.isPrivbteAddress(addr))
            return fblse;            
        if(!NetworkUtils.isVblidPort(port))
            return fblse;

        // reset the lbst connect back time so the next time the TCP/UDP
        // vblidators run they try to connect back.
        if (bcceptor != null)
        	bcceptor.resetLastConnectBackTime();
        if (UDPSERVICE != null)
        	UDPSERVICE.resetLbstConnectBackTime();
        
        if (mbnager != null) {
        	Properties props = new Properties();
        	props.put(HebderNames.LISTEN_IP,NetworkUtils.ip2string(addr)+":"+port);
        	HebderUpdateVendorMessage huvm = new HeaderUpdateVendorMessage(props);
        	
        	for (Iterbtor iter = manager.getInitializedConnections().iterator();iter.hasNext();) {
        		MbnagedConnection c = (ManagedConnection)iter.next();
        		if (c.remoteHostSupportsHebderUpdate() >= HeaderUpdateVendorMessage.VERSION)
        			c.send(huvm);
        	}
        	
        	for (Iterbtor iter = manager.getInitializedClientConnections().iterator();iter.hasNext();) {
        		MbnagedConnection c = (ManagedConnection)iter.next();
        		if (c.remoteHostSupportsHebderUpdate() >= HeaderUpdateVendorMessage.VERSION)
        			c.send(huvm);
        	}
        }
        return true;
    }
    
    /**
     * Notificbtion that we've either just set or unset acceptedIncoming.
     */
    public stbtic boolean incomingStatusChanged() {
        if(cbllback != null)
            cbllback.addressStateChanged();
            
        // Only continue if the current bddress/port is valid & not private.
        byte bddr[] = getAddress();
        int port = getPort();
        if(!NetworkUtils.isVblidAddress(addr))
            return fblse;
        if(NetworkUtils.isPrivbteAddress(addr))
            return fblse;            
        if(!NetworkUtils.isVblidPort(port))
            return fblse;
            
        return true;
    }
    
    /**
     * Returns the externbl IP address for this host.
     */
    public stbtic byte[] getExternalAddress() {
        return bcceptor.getExternalAddress();
    }

	/**
	 * Returns the rbw IP address for this host.
	 *
	 * @return the rbw IP address for this host
	 */
	public stbtic byte[] getAddress() {
		return bcceptor.getAddress(true);
	}
	
	/**
	 * Returns the Non-Forced IP bddress for this host.
	 *
	 * @return the non-forced IP bddress for this host
	 */
	public stbtic byte[] getNonForcedAddress() {
	    return bcceptor.getAddress(false);
	}
	

    /**
     * Returns the port used for downlobds and messaging connections.
     * Used to fill out the My-Address hebder in ManagedConnection.
     * @see Acceptor#getPort
     */    
	public stbtic int getPort() {
		return bcceptor.getPort(true);
	}
	
    /**
	 * Returns the Non-Forced port for this host.
	 *
	 * @return the non-forced port for this host
	 */
	public stbtic int getNonForcedPort() {
	    return bcceptor.getPort(false);
	}

	/**
	 * Returns whether or not this node is cbpable of sending its own
	 * GUESS queries.  This would not be the cbse only if this node
	 * hbs not successfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is cbpable of running its own
	 *  GUESS queries, <tt>fblse</tt> otherwise
	 */
	public stbtic boolean isGUESSCapable() {
		return UDPSERVICE.isGUESSCbpable();
	}


    /** 
     * Returns whether or not this node is cbpable of performing OOB queries.
     */
    public stbtic boolean isOOBCapable() {
        return isGUESSCbpable() && OutOfBandThroughputStat.isSuccessRateGood()&&
               !NetworkUtils.isPrivbte() &&
               SebrchSettings.OOB_ENABLED.getValue() &&
               bcceptor.isAddressExternal() && isIpPortValid();
    }


    public stbtic GUID getUDPConnectBackGUID() {
        return UDPSERVICE.getConnectBbckGUID();
    }

    
    /** @return true if your IP bnd port information is valid.
     */
    public stbtic boolean isIpPortValid() {
        return (NetworkUtils.isVblidAddress(getAddress()) &&
                NetworkUtils.isVblidPort(getPort()));
    }
    
    public stbtic boolean canReceiveSolicited() {
    	return UDPSERVICE.cbnReceiveSolicited();
    }
    
    public stbtic boolean canReceiveUnsolicited() {
    	return UDPSERVICE.cbnReceiveUnsolicited();
    }
    
    public stbtic boolean canDoFWT() {
        return UDPSERVICE.cbnDoFWT();
    }
}
