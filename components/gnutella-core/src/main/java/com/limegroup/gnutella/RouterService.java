package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.Cookies;
import com.limegroup.gnutella.util.*;

/**
 * A facade for the entire LimeWire backend.  This is the GUI's primary way of
 * communicating with the backend.  RouterService plays a key role in
 * constructing the backend components.  Typical use is as follows:
 *
 * <pre>
 * RouterService rs=new RouterService(callback, router);
 * rs.initialize();
 * ... //construct GUI
 * rs.postGuiInit();
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
 *      getTotalMessages, getTotalDroppedMessages, getTotalRouteErrors
 * </ul> 
 */
public class RouterService
{
    private ActivityCallback callback;
    private HostCatcher catcher;
    private MessageRouter router;
    private Acceptor acceptor;
    private ConnectionManager manager;
    private ResponseVerifier verifier = new ResponseVerifier();
    private DownloadManager downloader;
    private UploadManager uploadManager;
    private FileManager fileManager;
    private ChatManager chatManager;//keep the reference around...prevent class GC
    private SimpleTimer timer;

    
    /**
     * For authenticating users
     */
    private Authenticator authenticator;

    /**
     * isShuttingDown flag
     */
    private boolean isShuttingDown;

    private static RouterService me = null;
    /* @return May return null, be careful....
     */
    public static RouterService instance() {
        return me;
    }

	/**
	 * Creates a unitialized RouterService.  No work is done until
     * initialize() is called.
     * @param activityCallback the object to be notified of backend changes
     * @param router the algorithm to use for routing messages.  
     * @param fManager FileManager instance for all file system related duties
     * @param authenticator Authenticator instance for authenticating users
	 */
  	public RouterService(ActivityCallback activityCallback,
  						 MessageRouter router,
                         FileManager fManager,
                         Authenticator authenticator) {
  		this.callback = activityCallback;
  		this.router = router;
        this.fileManager = fManager;
        this.authenticator = authenticator;
        this.timer = new SimpleTimer(true, activityCallback);
		Assert.setCallback(this.callback);
        
        me = this;
  	}

	/**
     * Initializes the key backend components.  Some tasks are postponed
     * until postGuiInit().
	 */
  	public void initialize() {
		SettingsManager settings = SettingsManager.instance();
  		int port = settings.getPort();
  		this.acceptor = new Acceptor(port, callback);
  		this.manager = createConnectionManager();
  		this.catcher = createHostCatcher();
  		this.downloader = new DownloadManager();
  		this.uploadManager = new UploadManager(this.callback, this.router, 
											   this.fileManager);

        this.chatManager = ChatManager.instance();

		// Now, link all the pieces together, starting the various threads.
		this.catcher.initialize(acceptor, manager, this,
								SettingsManager.instance().getHostList());
		this.router.initialize(acceptor, manager, catcher, uploadManager);
		this.manager.initialize(router, catcher);		
		//this.uploadManager.initialize(callback, router, acceptor,fileManager);
		this.acceptor.initialize(manager, router, downloader, uploadManager);
        this.chatManager.setActivityCallback(callback);

		//We used to call the following code here:
        //  		if(settings.getConnectOnStartup()) {
        //  			this.catcher.connectToRouter();
        //  		}
        //
        //But that isn't needed; the call to connect() below calls
        //ConnnectionManager.connect(), which in turns HostCatcher.expire(),
        //which in turn calls HostCatcher.connectToRouter().
        //
        //If the code above were called (like in the old days)
        //HostCatcher.expire() would instead call Thread.interrupt, causing
        //HostCatcher.connectUntilPong to be restarted.       

		this.downloader.initialize(callback, router, acceptor, fileManager);
		
        //Ensure statistcs have started (by loading class).
        Statistics.instance();

		SupernodeAssigner sa=new SupernodeAssigner(uploadManager, 
                                                   downloader, 
                                                   manager);
		sa.start(this);

		if(settings.getConnectOnStartup()) {
			// Make sure connections come up ultra-fast (beyond default keepAlive)		
			int outgoing = settings.getKeepAlive();
			if ( outgoing > 0 ) 
				connect();
		}
  	}

    /**
     * Returns the ActivityCallback passed to this' constructor.
     */ 
    public ActivityCallback getActivityCallback() {
        return callback;
    }

    /**
     * Returns a new instance of RouterService. Its a Factory Method.
     */
    protected ConnectionManager createConnectionManager() {
        return new ConnectionManager(callback, authenticator);
    }
    
    /**
     * Returns a new instance of HostCatcher. Its a Factory Method.
     */
    protected HostCatcher createHostCatcher() {
        return new HostCatcher(callback);
    }
    
	/**
	 * Accessor for the <tt>FileManager</tt> instance in use.
	 *
	 * @return the <tt>FileManager</tt> in use
	 */
    public FileManager getFileManager(){
        return fileManager;
    }

    /** 
     * Accessor for the <tt>DownloadManager</tt> instance in use.
     *
     * @return the <tt>DownloadManager</tt> in use
     */
    public DownloadManager getDownloadManager() {
        return downloader;
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
    void schedule(Runnable task, long delay, long period) {
        timer.schedule(task, delay, period);
    }

    /** Kicks off expensive backend tasks (like file loading) that should
     *  only be done after GUI is loaded. */
    public void postGuiInit() {
        // Asynchronously load files now that the GUI is up, notifying
        // callback.
        fileManager.initialize(callback);
        // Restore any downloads in progress.
        downloader.postGuiInit(this);
    }

    private static final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0,
                                           (byte)1};

    /**
     * Creates a new outgoing messaging connection to the given host and port.
     * Blocks until the connection established.  Throws IOException if
     * the connection failed.
     * @return a connection to the request host
     * @exception IOException the connection failed
     */
    public ManagedConnection connectToHostBlocking(String hostname, int portnum)
            throws IOException {
        return manager.createConnectionBlocking(hostname, portnum);
    }

    /**
     * Creates a new outgoing messaging connection to the given host and port. 
     * Returns immediately without blocking.  If hostname would connect
     * us to ourselves, returns immediately.
     */
    public void connectToHostAsynchronously(String hostname, int portnum) {
        //Don't allow connections to yourself.  We have to special
        //case connections to "localhost" or "127.0.0.1" since
        //they are aliases for this machine.
        byte[] cIP = null;
        try {
            cIP=InetAddress.getByName(hostname).getAddress();
        } catch(UnknownHostException e) {
            return;
        }
        if ((Arrays.equals(cIP, LOCALHOST)) &&
            (portnum==acceptor.getPort())) {
                return;
        } else {
            byte[] managerIP=acceptor.getAddress();
            if (Arrays.equals(cIP, managerIP)
                && portnum==acceptor.getPort())
                return;
        }

        if (!acceptor.isBannedIP(hostname))
            manager.createConnectionAsynchronously(hostname, portnum);
    }


    /**
     * Attempts to connect to the given group.  Removes your current
     * connections and blocks until the group server has been contacted.
     * If the group server is not reachable, restores connection settings
     * and silently fails.
     */
    //public void connectToGroup(String group) {
	// groupConnect(group);
    //}

    /**
     * Connect to remote host (establish outgoing connection).
     * Blocks until connection established but send a GroupPingRequest
     */
    /**private ManagedConnection groupConnectToHostBlocking(
      String hostname, int portnum, String group)
            throws IOException {

        SettingsManager settings=SettingsManager.instance();
        group += ":"+settings.getConnectionSpeed();

        GroupPingRequest pingRequest =
          router.createGroupPingRequest(group);

        return manager.createGroupConnectionBlocking(hostname, portnum,
          pingRequest);
    }
	*/

    /**
     * Connects to router and sends a GroupPingRequest.
     * Block until connected.
     */
    /**private void groupConnect(String group) {
        SettingsManager settings=SettingsManager.instance();

        // Store the quick connect value.
        boolean useQuickConnect = settings.getUseQuickConnect();
        settings.setUseQuickConnect(false);

        // Ensure the keep alive is at least 1.
        if (settings.getKeepAlive()<1)
            settings.setKeepAlive(settings.DEFAULT_KEEP_ALIVE);
        int oldKeepAlive = settings.getKeepAlive();

        // Build an endpoint of the group server
        String host= "router.limewire.com:6349";
        Endpoint e;
        try {
            e=new Endpoint(host);
        } catch (IllegalArgumentException exc) {
            return;
        }

        // Disconnect from current connections.
        disconnect();

        // Clear host catcher.
        catcher.silentClear();

        // Kickoff the Group Connect fetch of PingReplies
        try {
            groupConnectToHostBlocking(e.getHostname(), e.getPort(), group);
        } catch (IOException exc) {
            settings.setUseQuickConnect(useQuickConnect);
            return;
        }

        // Reset the KeepAlive to greater than 1
        //oldKeepAlive;

        //Ensure settings are positive
        int outgoing=settings.getKeepAlive();
        if (outgoing<1) {
            outgoing = settings.DEFAULT_KEEP_ALIVE;
            settings.setKeepAlive(outgoing);
        }
        //int incoming=settings.getMaxIncomingConnections();
        ///if (incoming<1 && outgoing!=0) {
		// incoming = outgoing/2;
		//  settings.setMaxIncomingConnections(incoming);
        //}

		//  Adjust up keepAlive for initial ultrafast connect
		if ( outgoing < 10 ) {
			outgoing = 10;
			//manager.activateUltraFastConnectShutdown();
		}
        setKeepAlive(outgoing);
        settings.setUseQuickConnect(useQuickConnect);
    }
	*/

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public void connect() {
        //delegate to connection manager
        manager.connect();
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public void disconnect() {
		// Delegate to connection manager
		manager.disconnect();
    }

    /**
     * Closes and removes the given connection.
     */
    public void removeConnection(ManagedConnection c) {
        manager.remove(c);
    }

    /**
     * Clears the hostcatcher.
     */
    public void clearHostCatcher() {
        catcher.clear();
    }

    /**
     * Returns the number of pongs in the host catcher.  <i>This method is
     * poorly named, but it's obsolescent, so I won't bother to rename it.</i>
     */
    public int getRealNumHosts() {
        return(catcher.getNumHosts());
    }

    /**
     * Returns the number of downloads in progress.
     */
    public int getNumDownloads() {
        return downloader.downloadsInProgress();
    }
    
    /**
     * Returns the number of uploads in progress.
     */
    public int getNumUploads() {
        return uploadManager.uploadsInProgress();
    }


    /**
     * Shuts down the backend and writes the gnutella.net file.
     */
    public void shutdown() {
        //Update fractional uptime statistics (before writing limewire.props)
        Statistics.instance().shutdown();

        //Write gnutella.net
        try {
            catcher.write(SettingsManager.instance().getHostList());
        } catch (IOException e) {}
		finally {
			SettingsManager.instance().writeProperties();
		}
        //Cleanup any preview files.  Note that these will not be deleted if
        //your previewer is still open.
        File incompleteDir=null;
		try {
			incompleteDir=SettingsManager.instance().getIncompleteDirectory();
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
    }

    /**
     * Forces the backend to try to establish newKeep connections by kicking
     * off connection fetchers as needed.  Does not affect the KEEP_ALIVE
     * property.
     * @param newKeep the desired total number of messaging connections
     */
    public void forceKeepAlive(int newKeep) {
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
    public void setKeepAlive(int newKeep) throws BadConnectionSettingException {
        
        //validate the keep alive value

        //Negative keep alive is invalid
        if(newKeep < 0)
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                SettingsManager.instance().getKeepAlive());
        
        //TODO: we may want to re-enable this...with a higher limit.
        ////The request for increasing keep alive if we are leaf node is invalid
        //if ((newKeep > 1) && hasClientSupernodeConnection())
        //    throw new BadConnectionSettingException(
        //       BadConnectionSettingException.TOO_HIGH_FOR_LEAF, 1);

        //max connections for this connection speed
        int max = SettingsManager.instance().maxConnections();
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
    //public void setMaxIncomingConnections(int max) {
	//manager.setMaxIncomingConnections(max);
    //}

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public void adjustSpamFilters() {
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
    public void setListeningPort(int port) throws IOException {
        acceptor.setListeningPort(port);
    }

    /**
     * Sets the host catcher's flag for always notifing ActivityCallback on a 
     * known host added to the catcher.
     */
    public void setAlwaysNotifyKnownHost(boolean notify) {
        catcher.setAlwaysNotifyKnownHost(notify);
    }

    /** 
     * Returns true if this has accepted an incoming connection, and hence
     * probably isn't firewalled.  (This is useful for colorizing search
     * results in the GUI.)
     */
    public boolean acceptedIncomingConnection() {
            return acceptor.acceptedIncoming();
    }


    /**
     *  Returns the total number of messages sent and received.
     */
    public int getTotalMessages() {
        return( router.getNumMessages() );
    }

    /**
     *  Returns the total number of dropped messages.
     */
    public int getTotalDroppedMessages() {
        return( router.getNumDroppedMessages() );
    }

    /**
     *  Returns the total number of misrouted messages.
     */
    public int getTotalRouteErrors() {
        return( router.getNumRouteErrors() );
    }

    /**
     *  Returns the number of good hosts in my horizon.
     */
    public long getNumHosts() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getNumHosts();
        return ret;
    }

    /**
     * Returns the number of files in my horizon.
     */
    public long getNumFiles() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getNumFiles();
        return ret;
    }

    /**
     * Returns the size of all files in my horizon, in kilobytes.
     */
    public long getTotalFileSize() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getTotalFileSize();
        return ret;
    }

    /**
     * Prints out the information about current initialied connections
     */
    public void dumpConnections() {
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
    private void dumpConnections(Collection connections)
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
    public void updateHorizon() {        
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
    public void query(byte[] guid, String query, int minSpeed, MediaType type) {  
		// as specified in HUGE v0.93, ask for any available URNs on responses
		Set reqUrns = new HashSet();
		reqUrns.add(UrnType.ANY_TYPE);

		QueryRequest qr=new QueryRequest(guid,SettingsManager.instance().getTTL(),
										 minSpeed, query, "", false, reqUrns, null);
		verifier.record(qr, type);
		router.broadcastQueryRequest(qr);
	}

	/**
	 * Searches the network for files with the given metadata.
	 * 
	 * @param richQuery metadata query to insert between the nulls,
	 *  typically in XML format
	 * @see query(byte[], String, int, MediaType)
	 */
	public void query(byte[] guid, String query, String richQuery, 
					  int minSpeed, MediaType type) {
                            
		// per HUGE v0.93, ask for URNs on responses
		Set reqUrns = new HashSet();
		reqUrns.add(UrnType.ANY_TYPE);

		QueryRequest qr=new QueryRequest(guid, SettingsManager.instance().getTTL(),
                                         minSpeed, query, richQuery, false, reqUrns, null);
		verifier.record(qr, type);
		router.broadcastQueryRequest(qr);

		/* 
		 * We don't really use this. for now
         //Rich query?
         //Check if there are special servers to send this query to
         //Then spawn the RichConnectionThread and send the rich query 
         //out w/ it
         try{
         XMLHostCache xhc = new XMLHostCache();
         String[] ips = xhc.getCachedHostsForURI(schemaURI);
         if(ips!=null){
         for(int i=0;i<ips.length;i++){//usually just  1 iteration
         Thread rcThread=new 
         RichConnectionThread(ips[i],qr,callback);
         rcThread.start();
         }
         }
         }catch(Exception e){
         //do nothing
         }
        */
    }


    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(guid, query, minSpeed, null). 
     *
     * @see query(byte[], String, int, MediaType)
     */
    public void query(byte[] guid, String query, int minSpeed) {
        query(guid, query, minSpeed, null);
    }

    /** 
     * Returns the percentage of keywords in the query with the given guid that
     * match the given response.  Returns 100 if guid is not recognized.
     *
     * @param guid the value returned by query(..)  MUST be 16 bytes long.
     * @param resp a response delivered by ActivityCallback.handleQueryReply
     * @see ResponseVerifier#score(byte[], Response) 
     */
    public int score(byte[] guid, Response resp){
        return verifier.score(guid,resp);
    }

    /**
     * Returns true if the search related to the corresponding guid was a
     * 'specific' xml search, ie if it had two or more XML fields specified.
     */
    public boolean isSpecificXMLSearch(byte[] guid) {
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
    public boolean matchesType(byte[] guid, Response response) {
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
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        return verifier.isMandragoreWorm(guid, response);
    }

    /**
     * Returns an iterator of the hosts in the host catcher, each
     * an Endpoint.
     */
    public Iterator getHosts() {
        return catcher.getHosts();
    }

    /**
     *  Returns the number of messaging connections.
     */
    public int getNumConnections() {
		return manager.getNumConnections();
    }

	/**
	 * Returns whether or not this client currently has any initialized 
	 * connections.
	 *
	 * @return <tt>true</tt> if the client does have initialized connections,
	 *  <tt>false</tt> otherwise
	 */
	public boolean isConnected() {
		return manager.isConnected();
	}

    /**
     *  Returns the number searches made to the local database.
     */
    public int getNumLocalSearches() {
        return router.getNumQueryRequests();
    }

    /**
     *  Ensures that the given host:port pair is not in the host catcher.
     */
    public void removeHost(String host, int port) {
        catcher.removeHost(host, port);
    }

    /**
     * Returns the number of files being shared locally.
     */
    public int getNumSharedFiles( ) {
        return( fileManager.getNumFiles() );
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
    public FileDesc[] getSharedFileDescriptors(File directory) {
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
    public File[] getSharedFiles(File directory) {
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
	public Downloader download(RemoteFileDesc[] files, boolean overwrite)
		throws FileExistsException, AlreadyDownloadingException, 
  			   java.io.FileNotFoundException {
		return downloader.download(files, overwrite);
	}

   /**
     * Starts a resume download for the given incomplete file.
     * @exception AlreadyDownloadingException couldn't download because the
     *  another downloader is getting the file
     * @exception CantResumeException incompleteFile is not a valid 
     *  incomplete file
     */ 
    public Downloader download(File incompleteFile)
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
    public Downloader download(String query, String richQuery,
                               byte[] guid, MediaType type) 
        throws AlreadyDownloadingException {
        return downloader.download(query, richQuery, guid, type);
    }

	/**
	 * Creates and returns a new chat to the given host and port.
	 */
	public Chatter createChat(String host, int port) {
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
	public void doBrowseHost(String host, int port, 
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
    public boolean isSupernode() {
        return manager.isSupernode();
    }

    public boolean hasClientSupernodeConnection() {
        return manager.hasClientSupernodeConnection();
    }
    
    public boolean hasSupernodeClientConnection() {
        return manager.hasSupernodeClientConnection();
    }

    /**
     * Sets the flag for whether or not LimeWire is currently in the process of 
	 * shutting down.
	 *
     * @param flag the shutting down state to set
     */
    public void setIsShuttingDown(boolean flag) {
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
    public boolean getIsShuttingDown() {
		return isShuttingDown;
    }

}
