package com.limegroup.gnutella;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.xml.*;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.Cookies;

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
 *     connectToGroup, reduceConnections, removeConnection,
 *     getNumConnections
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
    
    /**
     * For authenticating users
     */
    private Authenticator authenticator;

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
  		this.uploadManager = new UploadManager();

        this.chatManager = ChatManager.instance();

		// Now, link all the pieces together, starting the various threads.
		this.catcher.initialize(acceptor, manager,
								SettingsManager.instance().getHostList());
		this.router.initialize(acceptor, manager, catcher, uploadManager);
		this.manager.initialize(router, catcher);		
		this.uploadManager.initialize(callback, router, acceptor,fileManager);
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

		this.downloader.initialize(callback, router, acceptor,
                                   fileManager);
		
		Thread supernodeThread = 
		    new Thread(new SupernodeAssigner(uploadManager, downloader, manager));
		supernodeThread.setDaemon(true);
		supernodeThread.start();

		if(settings.getConnectOnStartup()) {
			// Make sure connections come up ultra-fast (beyond default keepAlive)		
			int outgoing = settings.getKeepAlive();
			if ( outgoing > 0 ) 
				connect();
		}
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


    /** Kicks off expensive backend tasks (like file loading) that should
     *  only be done after GUI is loaded. */
    public void postGuiInit() {
        // Asynchronously load files now that the GUI is up, notifying
        // callback.
        fileManager.initialize(callback);
        // Restore any downloads in progress.
        downloader.readSnapshot();
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
	 * Lowers the number of connections for "low-power" mode.
	 */
	//public void reduceConnections() {
	//manager.reduceConnections();
	//}

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
        catcher.silentClear();
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
        
        //save cookies
        Cookies.instance().save();
    }

    /**
     * Forces the backend to try to establish newKeep connections by kicking
     * off connection fetchers as needed.  Does not affect the KEEP_ALIVE
     * property.
     *
     * @param newKeep the desired total number of messaging connections
     */
    public void setKeepAlive(int newKeep) {
        manager.setKeepAlive(newKeep);
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
     * Searches the network for files of the given type with the given
     * query string and minimum speed.  If type is null, any file type
     * is acceptable.  Returns the GUID of the query request sent as a
     * 16 byte array.<p>
     *
     * ActivityCallback is notified asynchronously of responses.
     * These responses can be matched with requests by looking at
     * their GUIDs.  (You may want to wrap the bytes with a GUID
     * object for simplicity.) 
     * 
     * @param query the query string to use
     * @param minSpeed the minimum desired result speed
     * @param type the desired type of result (e.g., audio, video), or
     *  null if you don't care
     * @return the guid of the underlying search.  Used to match up
     *  query results.  Guaranteed to be 16 bytes long.
     */
    public byte[] query(String query, int minSpeed, MediaType type) {
        //anu modified
        //TODO revert changes, or place somewhere else. I guess sumeet will
        //figure out
        //anu creating rich query (by duplicating the normal query into
        //both plain and rich areas
        QueryRequest qr=new QueryRequest(SettingsManager.instance().getTTL(),
                                         minSpeed, query);
        verifier.record(qr, type);
        router.broadcastQueryRequest(qr);
        return qr.getGUID();
    }

    /**
     * @see query(String, int, MediaType)
     * same as the method above, except, the type is set to "any" and
     * the it creates a rich Query. 
     * @return the guid of the search, used to match up query with 
     * replies. 
     * @param type the desired type of result (e.g., audio, video), or
     *  null if you don't care
     */
    public byte[] query(String query, String richQuery, 
                        int minSpeed, MediaType type, String schemaURI) {
        //System.out.println("Sumeet rich query coming...");
        Assert.that(schemaURI!=null,"rich query without schema");
        QueryRequest qr=new QueryRequest(SettingsManager.instance().getTTL(),
                                         minSpeed, query, richQuery);
        verifier.record(qr, type);
        router.broadcastQueryRequest(qr);
        //Rich query? Check if there are special servers to send this query to
        //Then spawn the RichConnectionThread and send the rich query out w/ it
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
        return qr.getGUID();
    }


    /** 
     * Searches the network for files with the given query string and 
     * minimum speed, i.e., same as query(query, minSpeed, null). 
     *
     * @see query(String, int, MediaType)
     */
    public byte[] query(String query, int minSpeed) {
        return query(query, minSpeed, null);
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
     * Searches the given host for all its files.  Results are given to the GUI
     * via handleQuery.  Returns the guid of the query, or null if the host
     * couldn't be reached.  Blocks until the connection is established and the
     * query is sent.  
     *
     * @return the guid of the underlying search.  Used to match up
     *  query results.  Guaranteed to be 16 bytes long.
     */
    public byte[] browse(String host, int port) {
        ManagedConnection c=null;

        //1. See if we're connected....
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; ) {
            ManagedConnection c2=(ManagedConnection)iter.next();
            //Get the IP address of c2 in dotted decimal form.  Note
            //that c2.getOrigHost is no good as it may return a
            //symbolic name.
            String ip=c2.getInetAddress().getHostAddress();
            if (ip.equals(host) && c2.getOrigPort()==port) {
            c=c2;
            break;
            }
        }
        //...if not, establish a new one.
        if (c==null) {
            try {
                c = connectToHostBlocking(host, port);
            } catch (IOException e) {
                return null;
            }
        }

        //2. Send a query for "*.*" with a TTL of 1.
        QueryRequest qr=new QueryRequest((byte)1, 0, FileManager.BROWSE_QUERY);
        router.sendQueryRequest(qr, c);
        try {
            c.flush();
        } catch (IOException e) {
            return null;
        }

        //3. Remove a lesser connection if necessary.  Current heuristic:
        //drop the connection other than c with least number of files.
        //
        //TODO: this should go in ConnectionManager, but that requires
        //us to add MAX_KEEP_ALIVE property.  Besides being the logical
        //place for this functionality, it would make the network
        //hill climbing a snap to implement.  It would also allow us to
        //synchronize properly to prevent race conditions.
        if (manager.getNumConnections()>manager.getKeepAlive()) {
            ManagedConnection worst=null;
            long files=Long.MAX_VALUE;
            for (Iterator iter=manager.getConnections().iterator();
                 iter.hasNext(); ) {
                ManagedConnection c2=(ManagedConnection)iter.next();
                //Don't remove the connection to the host we are browsing.
                if (c2==c)
                    continue;
                long n=c2.getNumFiles();
                if (n<files) {
                    worst=c2;
                    files=n;
                }
            }
            if (worst!=null)
                manager.remove(worst);
        }

        return qr.getGUID();
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
     * Returns a list of all shared files in the given directory.
     * All the files returned have already been passed to the gui
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
		return downloader.getFiles(files, overwrite);
	}

    /** 
      * Notifies the backend that the BLACKLISTED_IP property has changed,
      * forcing the acceptor to reload the property. This method was added
      * to solve bug 62001.  
      */
    public void refreshBannedIPs() {
        acceptor.refreshBannedIPs();
    }


	/**
	 * Creates and returns a new chat to the given host and port.
	 */
	public Chatter createChat(String host, int port) {
		Chatter chatter = ChatManager.instance().request(host, port);
		return chatter;
	}

    /**
     * Tells whether the node is a supernode or not
     * @return true, if supernode, false otherwise
     */
    public boolean isSupernode()
    {
        return manager.isSupernode();
    }

    public boolean hasClientSupernodeConnection() {
        return manager.hasClientSupernodeConnection();
    }

    public void sendQRToDownloadManager(QueryReply qr) {
        if (downloader != null)
            downloader.handleQueryReply(qr);
    }
    


}
