package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.security.ServerAuthenticator;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.Cookies;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.updates.*;
import com.limegroup.gnutella.browser.*;

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
public final class RouterService {

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
	 * Initialize the class that manages all TCP connections.
	 */
    private static final ConnectionManager manager =
		new ConnectionManager(authenticator);

	/**
	 * <tt>HostCatcher</tt> that handles Gnutella pongs.
	 */
    private static final HostCatcher catcher = new HostCatcher();
	
	/**
	 * <tt>DownloadManager</tt> for handling HTTP downloading.
	 */
    private static final DownloadManager downloader = new DownloadManager();

	/**
	 * <tt>UploadManager</tt> for handling HTTP uploading.
	 */
    private static UploadManager uploadManager = new UploadManager();

	
    private static final ResponseVerifier verifier = new ResponseVerifier();

	//keep the reference around...prevent class GC
	/**
	 * <tt>ChatManager</tt> for managing all chat sessions.
	 */
    private static final ChatManager chatManager = ChatManager.instance();

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
	 * Constant for the <tt>SettingsManager</tt>.
	 */
	private static final SettingsManager SETTINGS = 
		SettingsManager.instance();

	/**
	 * Creates a new <tt>RouterService</tt> instance.  This fully constructs 
	 * the backend.
	 *
	 * @param callback the <tt>ActivityCallback</tt> instance to use for
	 *  making callbacks
	 */
  	public RouterService(ActivityCallback callback) {
        this(callback, 
		    new MetaEnabledMessageRouter(callback, fileManager));
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
        //TODO: expand parameter list to allow for easy testing with stubs for
        //FileManager, Acceptor, etc.
		RouterService.callback = callback;
  		RouterService.router = router;

		// Now, link all the pieces together, starting the various threads.
		router.initialize();
		manager.initialize();	   
		downloader.initialize(); 
	    new HTTPAcceptor(callback);	
		SupernodeAssigner sa = new SupernodeAssigner(uploadManager, 
													 downloader, 
													 manager);
		sa.start();

		if(SETTINGS.getConnectOnStartup()) {
			// Make sure connections come up ultra-fast (beyond default keepAlive)		
			int outgoing = SETTINGS.getKeepAlive();
			if ( outgoing > 0 ) 
				connect();
		}
  	}

	/**
	 * Starts various threads and tasks once all core classes have
	 * been constructed.
	 */
	public void start() {
		// start up the UDP server thread
		catcher.initialize();
		acceptor.initialize();
		
        // Asynchronously load files now that the GUI is up, notifying
        // callback.
        fileManager.initialize();

        // Restore any downloads in progress.
        downloader.postGuiInit();
        
        UpdateManager updater = UpdateManager.instance();//initialize
        updater.postGuiInit(callback);
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
     * Accessor for the <tt>Acceptor</tt> instance.
     *
     * @return the <tt>Acceptor</tt> in use
     */
	public static Acceptor getAcceptor() {
		return acceptor;
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
        try {
            cIP=InetAddress.getByName(hostname).getAddress();
        } catch(UnknownHostException e) {
            return;
        }
        if ((cIP[0] == 127) && (portnum==acceptor.getPort())) {
			return;
        } else {
            byte[] managerIP=acceptor.getAddress();
            if (Arrays.equals(cIP, managerIP)
                && portnum==acceptor.getPort())
                return;
        }

        if (!acceptor.isBannedIP(hostname)) {
            manager.createConnectionAsynchronously(hostname, portnum);
		}
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public static void connect() {
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
     * Returns the number of uploads in progress.
     */
    public static int getNumUploads() {
        return uploadManager.uploadsInProgress();
    }


    /**
     * Shuts down the backend and writes the gnutella.net file.
     */
    public static void shutdown() {
        try {
            //Update fractional uptime statistics (before writing limewire.props)
            Statistics.instance().shutdown();
            
            //Write gnutella.net
            try {
                catcher.write(SETTINGS.getHostList());
            } catch (IOException e) {}
            finally {
                SETTINGS.writeProperties();
            }
            //Cleanup any preview files.  Note that these will not be deleted if
            //your previewer is still open.
            File incompleteDir=null;
            try {
                incompleteDir=SETTINGS.getIncompleteDirectory();
            } catch(java.io.FileNotFoundException fnfe) {
                // if we could not get the incomplete directory, simply return.
                return;
            }
            
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
        } catch(Throwable t) {
            RouterService.error(t);
        }
    }

    /**
     * Forces the backend to try to establish newKeep connections by kicking
     * off connection fetchers as needed.  Does not affect the KEEP_ALIVE
     * property.
     * @param newKeep the desired total number of messaging connections
     */
    public static void forceKeepAlive(int newKeep) {
        //no validation done
        
        //set the new keep alive
        manager.setKeepAlive(newKeep);
    }
    
    /**
     * Validates the passed new keep alive, and sets the backend to 
     * try to establish newKeep connections by kicking
     * off connection fetchers as needed.  Does not affect the KEEP_ALIVE
     * property.
     * @param newKeep the desired total number of messaging connections
     * @exception if the suggested keep alive value is not suitable
     */
    public static void setKeepAlive(int newKeep) throws BadConnectionSettingException {
        
        //validate the keep alive value

        //Negative keep alive is invalid
        if(newKeep < 0)
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                SETTINGS.getKeepAlive());
        
        //TODO: we may want to re-enable this...with a higher limit.
        ////The request for increasing keep alive if we are leaf node is invalid
        //if ((newKeep > 1) && hasClientSupernodeConnection())
        //    throw new BadConnectionSettingException(
        //       BadConnectionSettingException.TOO_HIGH_FOR_LEAF, 1);

        //max connections for this connection speed
        int max = SETTINGS.maxConnections();
        if (manager.hasSupernodeClientConnection()) {
            //Also the request to decrease the keep alive below a minimum
            //level is invalid, if we are an Ultrapeer with leaves
            if (newKeep < manager.MIN_CONNECTIONS_FOR_SUPERNODE)
                throw new BadConnectionSettingException(
                BadConnectionSettingException.TOO_LOW_FOR_ULTRAPEER,
                manager.MIN_CONNECTIONS_FOR_SUPERNODE);
            else if (newKeep > manager.MIN_CONNECTIONS_FOR_SUPERNODE 
                && newKeep > max)
                throw new BadConnectionSettingException(
                BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                manager.MIN_CONNECTIONS_FOR_SUPERNODE > max ?
                    manager.MIN_CONNECTIONS_FOR_SUPERNODE : max);
        } else if (newKeep > max)
            //cant have too many connections based upon node's speed
            throw new BadConnectionSettingException(
                BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                max);

        //set the new keep alive.  To allow connections to bootstrap servers, we
        //expire the HostCatcher if the keep alive was zero.  This is similar to
        //calling connect(), except that it does not get the keep alive from
        //SettingsManager.
        if (manager.getKeepAlive()==0)
            catcher.expire();
        forceKeepAlive(newKeep);
    }

    /**
     * Sets the max number of incoming Gnutella connections allowed by the
     * connection manager.  This does not affect the permanent
     * MAX_INCOMING_CONNECTIONS property.  
     */
    //public static void setMaxIncomingConnections(int max) {
	//manager.setMaxIncomingConnections(max);
    //}

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public static void adjustSpamFilters() {
        acceptor.refreshBannedIPs();

        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for (Iterator iter=manager.getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            c.setPersonalFilter(SpamFilter.newPersonalFilter());
            c.setRouteFilter(SpamFilter.newRouteFilter());
        }
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
     * Sets the host catcher's flag for always notifing ActivityCallback on a 
     * known host added to the catcher.
     */
    public static void setAlwaysNotifyKnownHost(boolean notify) {
        catcher.setAlwaysNotifyKnownHost(notify);
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
     *  Returns the total number of messages sent and received.
     */
//      public static int getTotalMessages() {
//          return( router.getNumMessages() );
//      }

    /**
     *  Returns the total number of dropped messages.
     */
//      public static int getTotalDroppedMessages() {
//          return( router.getNumDroppedMessages() );
//      }

    /**
     *  Returns the total number of misrouted messages.
     */
//      public static int getTotalRouteErrors() {
//          return( router.getNumRouteErrors() );
//      }


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
		long ret=0;
		for (Iterator iter=manager.getInitializedConnections().iterator();
			 iter.hasNext() ; )
			ret+=((ManagedConnection)iter.next()).getNumHosts();
		return ret;
    }

    /**
     * Returns the number of files in my horizon.
     */
    public static long getNumFiles() {
		long ret=0;
		for (Iterator iter=manager.getInitializedConnections().iterator();
			 iter.hasNext() ; )
			ret+=((ManagedConnection)iter.next()).getNumFiles();
		return ret;
    }

    /**
     * Returns the size of all files in my horizon, in kilobytes.
     */
    public static long getTotalFileSize() {
		long ret=0;
		for (Iterator iter=manager.getInitializedConnections().iterator();
			 iter.hasNext() ; )
			ret+=((ManagedConnection)iter.next()).getTotalFileSize();
		return ret;
    }

    /**
     * Prints out the information about current initialied connections
     */
    public static void dumpConnections() {
        //dump ultrapeer connections
        System.out.println("UltraPeer connections");
        dumpConnections(manager.getInitializedConnections2());
        //dump leaf connections
        System.out.println("Leaf connections");
        dumpConnections(manager.getInitializedClientConnections2());
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
        for (Iterator iter=manager.getInitializedConnections().iterator();
			 iter.hasNext() ; )
			((ManagedConnection)iter.next()).refreshHorizonStats();
    }

    /** 
     * Returns a new GUID for passing to query.
     */
    public static byte[] newQueryGUID() {
        return QueryRequest.newQueryGUID(false);
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
    public static void query(byte[] guid, String query, int minSpeed, MediaType type) {
		query(guid, query, "", minSpeed, type);
	}

	/**
	 * Searches the network for files with the given metadata.
	 * 
	 * @param richQuery metadata query to insert between the nulls,
	 *  typically in XML format
	 * @see query(byte[], String, int, MediaType)
	 */
	public static void query(final byte[] guid, 
							 final String query, 
							 final String richQuery, 
							 final int minSpeed, 
							 final MediaType type) {

		Thread searcherThread = new Thread() {
			public void run() {
                try {
                    // per HUGE v0.94, ask for URNs on responses
                    Set reqUrns = new HashSet();
                    reqUrns.add(UrnType.ANY_TYPE);
                    
                    QueryRequest qr = 
                        new QueryRequest(guid, SETTINGS.getTTL(), minSpeed, 
                                         query, richQuery, false, reqUrns, null,
                                         !acceptedIncomingConnection());
                    verifier.record(qr, type);
                    router.broadcastQueryRequest(qr);
                } catch(Throwable t) {
                    RouterService.error(t);
                }
			}
		};
		searcherThread.setDaemon(true);
		searcherThread.start();
    }


    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, int, MediaType)
     */
    public static void query(byte[] guid, String query, int minSpeed) {
        query(guid, query, minSpeed, null);
    }


    /** Will make all attempts to stop a query from executing.  Really only 
     *  applicable to GUESS queries...
     *  @param guid The GUID of the query you want to get rid of....
     */
    public static void stopQuery(GUID guid) {
        QueryUnicaster.instance().purgeQuery(guid);
    }

    /** 
     * Returns the percentage of keywords in the query with the given guid that
     * match the given response.  Returns 100 if guid is not recognized.
     *
     * @param guid the value returned by query(..)  MUST be 16 bytes long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#score(byte[], Response) 
     */
    public static int score(byte[] guid, Response resp){
        return verifier.score(guid,resp);
    }

    /**
     * Returns true if the search related to the corresponding guid was a
     * 'specific' xml search, ie if it had two or more XML fields specified.
     */
    public static boolean isSpecificXMLSearch(byte[] guid) {
        return verifier.isSpecificXMLSearch(guid);
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
     * Returns an iterator of the hosts in the host catcher, each
     * an Endpoint.
     */
    public static Iterator getHosts() {
        return catcher.getHosts();
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
     *  Returns the number searches made to the local database.
     */
//      public static int getNumLocalSearches() {
//          return router.getNumQueryRequests();
//      }

    /**
     *  Ensures that the given host:port pair is not in the host catcher.
     */
    public static void removeHost(String host, int port) {
        catcher.removeHost(host, port);
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
     * Returns a list of all shared file descriptors in the given directory.
     * All the file descriptors returned have already been passed to the gui
     * via ActivityCallback.addSharedFile.  Note that if a file descriptor
     * is added to the given directory after this method completes, 
     * addSharedFile will be called for that file descriptor.<p>
     *
     * If directory is not a shared directory, returns null.
     */
    public static FileDesc[] getSharedFileDescriptors(File directory) {
        return fileManager.getSharedFileDescriptors(directory);
    }
    
    /**
     * Returns a list of all shared file in the given directory.
     * All the file returned have already been passed to the gui
     * via ActivityCallback.addSharedFile.  Note that if a file 
     * is added to the given directory after this method completes, 
     * addSharedFile will be called for that file.<p>
     *
     * If directory is not a shared directory, returns null.
     */
    public static File[] getSharedFiles(File directory) {
        return fileManager.getSharedFiles(directory);
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
     * @param overwrite true iff the download should proceded without
     *  checking if it's on disk
     * @return the download object you can use to start and resume the download
     * @exception AlreadyDownloadingException the file is already being 
     *  downloaded.
     * @exception FileExistsException the file already exists in the library
     * @see DownloadManager#getFiles(RemoteFileDesc[], boolean)
     */
	public static Downloader download(RemoteFileDesc[] files, boolean overwrite)
		throws FileExistsException, AlreadyDownloadingException, 
  			   java.io.FileNotFoundException {
		return downloader.download(files, overwrite);
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
    public static synchronized Downloader download(
            URN urn, String textQuery, String filename, String [] defaultURL) 
            throws IllegalArgumentException, AlreadyDownloadingException { 
        return downloader.download(urn, textQuery, filename, defaultURL);
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
     * Starts a "requery download", aka, a "wishlist download".  A "requery
     * download" should be started when the user has not received any results
     * for her query, and wants LimeWire to spawn a specialized Downloader that
     * requeries the network until a 'appropriate' file is found.
     * 
     * @param query The original query string.
     * @param richQuery The original richQuery string.
     * @param guid The guid associated with this query request.
     * @param type The mediatype associated with this search.
     */
    public static Downloader download(String query, String richQuery,
                               byte[] guid, MediaType type) 
        throws AlreadyDownloadingException {
        return downloader.download(query, richQuery, guid, type);
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
	 */
	public static void doBrowseHost(String host, int port, 
                             GUID guid, GUID serventID) {
        BrowseHostHandler handler = new BrowseHostHandler(callback, router,
                                                          acceptor, guid,
                                                          serventID);
        handler.browseHost(host, port);
	}

    /**
     * Tells whether the node is a supernode or not
     * @return true, if supernode, false otherwise
     */
    public static boolean isSupernode() {
        return manager.isSupernode();
    }

    public static boolean hasClientSupernodeConnection() {
        return manager.hasClientSupernodeConnection();
    }
    
    public static boolean hasSupernodeClientConnection() {
        return manager.hasSupernodeClientConnection();
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
	 * Returns the raw IP address for this host.
	 *
	 * @return the raw IP address for this host
	 */
	public static byte[] getAddress() {
		return acceptor.getAddress();
	}

    /**
     * Returns the port used for downloads and messaging connections.
     * Used to fill out the My-Address header in ManagedConnection.
     * @see Acceptor#getPort
     */    
	public static int getPort() {
		return acceptor.getPort();
	}

	/**
	 * Notify the callback that an error of the specified error code has
	 * occurred.
	 *
	 * @param errorCode the code for the error
	 */
	public static void error(int errorCode) { 
		callback.error(errorCode);
	}

	/**
	 * Notify the callback that an error has occurred with the given 
	 * <tt>Throwable</tt>.
	 *
	 * @param trace the <tt>Throwable</tt> instance containing the stack
	 *  trace of the error
	 */
	public static void error(Throwable trace) {
		trace.printStackTrace();
		callback.error(trace);
	}

	/**
	 * Notify the callback that an error of the specified error code has
	 * occurred with the given <tt>Throwable</tt>.
	 *
	 * @param errorCode the code for the error
	 * @param trace the <tt>Throwable</tt> instance containing the stack
	 *  trace of the error
	 */
	public static void error(int errorCode, Throwable trace) {
		trace.printStackTrace();
		callback.error(errorCode, trace);
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
		return udpService.isGUESSCapable() && SETTINGS.getGuessEnabled();
	}
}
