package com.limegroup.gnutella;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.downloader.*;

/**
 * The External interface into the router world.
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

    /**
     * Create a RouterService accepting connections on the default port
     */
    public RouterService(ActivityCallback activityCallback,
                         MessageRouter router) {
        this(SettingsManager.instance().getPort(),
             activityCallback,
             router);
    }

    /**
     * Create a RouterService accepting connections on the specified port
     */
    public RouterService(int port,
                         ActivityCallback activityCallback,
                         MessageRouter router) {
        callback = activityCallback;

        // First, construct all the pieces
        this.acceptor = new Acceptor(port, callback);
        this.manager = new ConnectionManager(callback);
        this.router = router;
        this.catcher = new HostCatcher(callback);
        downloader = new DownloadManager();

        // Now, link all the pieces together, starting the various threads.
        this.catcher.initialize(acceptor, manager,
                                SettingsManager.instance().getHostList());
        this.router.initialize(acceptor, manager, catcher);
        this.manager.initialize(router, catcher);
        this.acceptor.initialize(manager, router, downloader);
        this.downloader.initialize(callback, router, acceptor, FileManager.instance());

		// Make sure connections come up ultra-fast (beyond default keepAlive)
        SettingsManager settings=SettingsManager.instance();
        int outgoing=settings.getKeepAlive();
		if ( outgoing > 0 )
		    connect();
    }

    /**
     * Dump the ping and query routing tables
     */
    public void dumpRouteTable() {
        System.out.println(router.getPingRouteTableDump());
        System.out.println(router.getQueryRouteTableDump());
    }

    /**
     * Dump the push routing table
     */
    public void dumpPushRouteTable() {
        System.out.println(router.getPushRouteTableDump());
    }

    /**
     * Dump the list of connections
     */
    public void dumpConnections() {
        Iterator iter=manager.getConnections().iterator();
        while (iter.hasNext())
            System.out.println(iter.next().toString());
    }

    private static final byte[] LOCALHOST={(byte)127, (byte)0, (byte)0,
                                           (byte)1};

    /**
     * Connect to remote host (establish outgoing connection).
     * Blocks until connection established.
     */
    public ManagedConnection connectToHostBlocking(String hostname, int portnum)
            throws IOException {
        return manager.createConnectionBlocking(hostname, portnum);
    }

    /**
     * Connect to remote host (establish outgoing connection) on a separate
     * thread.
     * If establishing c would connect us to the listening socket,
     * the connection is not established.
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

        manager.createConnectionAsynchronously(hostname, portnum);
    }


    /**
     * Attempts to connect to the given group.  Removes your current
     * connections and blocks until the group server has been contacted.
     * If the group server is not reachable, restores connection settings
     * and silently fails.
     */
    public void connectToGroup(String group) {
        groupConnect(group);
    }

    /**
     * Connect to remote host (establish outgoing connection).
     * Blocks until connection established but send a GroupPingRequest
     */
    private ManagedConnection groupConnectToHostBlocking(
      String hostname, int portnum, String group)
            throws IOException {

        SettingsManager settings=SettingsManager.instance();
        group += ":"+settings.getConnectionSpeed();

        GroupPingRequest pingRequest =
          router.createGroupPingRequest(group);

        return manager.createGroupConnectionBlocking(hostname, portnum,
          pingRequest);
    }

    /**
     * Connects to router and sends a GroupPingRequest.
     * Block until connected.
     */
    private void groupConnect(String group) {
        SettingsManager settings=SettingsManager.instance();

        // Store the quick connect value.
        boolean useQuickConnect = settings.getUseQuickConnect();
        settings.setUseQuickConnect(false);

        // Ensure the keep alive is at least 1.
        if (settings.getKeepAlive()<1)
            settings.setKeepAlive(SettingsInterface.DEFAULT_KEEP_ALIVE);
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
        setKeepAlive(oldKeepAlive);
        settings.setUseQuickConnect(useQuickConnect);
    }

    /**
     * @modifies this
     * @effects ensures the keep-alive is non-zero and recontacts the
     *  pong server as needed.
     */
    public void connect() {
        //HACK. People used to complain to that the connect button wasn't
        //working when the host catcher was empty and USE_QUICK_CONNECT=false.
        //This is not a bug; LimeWire isn't supposed to connect to the pong
        //server in this case.  But this IS admittedly confusing.  So we force a
        //connection to the pong server in this case by disconnecting and
        //temporarily setting USE_QUICK_CONNECT to true.  But we have to
        //sleep(..) a little bit before setting USE_QUICK_CONNECT back to false
        //to give the connection fetchers time to do their thing.  Ugh.  A
        //Thread.yield() may work here too, but that's less dependable.  And I
        //do not want to bother with wait/notify's just for this obscure case.
        SettingsManager settings=SettingsManager.instance();
        boolean useHack=
            (!settings.getUseQuickConnect())
                && catcher.getNumHosts()==0;
        if (useHack) {
            settings.setUseQuickConnect(true);
            disconnect();
        }

        //Force reconnect to pong server.
        catcher.expire();

        //Ensure outgoing connections is positive.
        int outgoing=settings.getKeepAlive();
        if (outgoing<1) {
            outgoing = settings.DEFAULT_KEEP_ALIVE;
            settings.setKeepAlive(outgoing);
        }
        //Actually notify the backend.

		//  Adjust up keepAlive for initial ultrafast connect
		if ( outgoing < 10 ) {
			outgoing = 10;
			manager.activateUltraFastConnectShutdown();
		}
        setKeepAlive(outgoing);

        //int incoming=settings.getKeepAlive();
        //setMaxIncomingConnections(incoming);

        //See note above.
        if (useHack) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
            SettingsManager.instance().setUseQuickConnect(false);
        }
    }

    /**
     * @modifies this
     * @effects removes all connections.
     * @effects deactivates extra connection watchdog check
     */
    public void disconnect() {
		// Deactivate checking for Ultra Fast Shutdown
		manager.deactivateUltraFastConnectShutdown(); 

        SettingsManager settings=SettingsManager.instance();
        int oldKeepAlive=settings.getKeepAlive();

        //1. Prevent any new threads from starting.  Note that this does not
        //   affect the permanent settings.
        setKeepAlive(0);
        //setMaxIncomingConnections(0);
        //2. Remove all connections.
        for (Iterator iter=manager.getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            removeConnection(c);
        }
    }

    /**
     * Remove a connection based on the host/port
     */
    public void removeConnection(ManagedConnection c) {
        manager.remove(c);
    }

    /**
     * Clear the hostcatcher if requested
     */
    public void clearHostCatcher() {
        catcher.silentClear();
    }

    /**
     * Get the real number of hosts from the host catcher
     */
    public int getRealNumHosts() {
        return(catcher.getNumHosts());
    }


    /**
     * Shut stuff down and write the gnutella.net file
     */
    public void shutdown() {
        try {
            catcher.write(SettingsManager.instance().getHostList());
        } catch (IOException e) {}
    }

    /**
     *  Reset how many connections you want and start kicking more off
     *  if required.  Does not affect the KEEP_ALIVE property.
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
     * Notify the backend that spam filters settings have changed, and that
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
     * @modifies this
     * @effects sets the port on which to listen for incoming connections.
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     */
    public void setListeningPort(int port) throws IOException {
        acceptor.setListeningPort(port);
    }

    /**
     *  Return the total number of messages sent and received
     */
    public int getTotalMessages() {
        return( router.getNumMessages() );
    }

    /**
     *  Return the total number of dropped messages
     */
    public int getTotalDroppedMessages() {
        return( router.getNumDroppedMessages() );
    }

    /**
     *  Return the total number of misrouted messages
     */
    public int getTotalRouteErrors() {
        return( router.getNumRouteErrors() );
    }

    /**
     *  Return the number of good hosts in my horizon.
     */
    public long getNumHosts() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getNumHosts();
        return ret;
    }

    /**
     * Return the number of files in my horizon.
     */
    public long getNumFiles() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getNumFiles();
        return ret;
    }

    /**
     * Return the size of all files in my horizon, in kilobytes.
     */
    public long getTotalFileSize() {
        long ret=0;
        for (Iterator iter=manager.getInitializedConnections().iterator();
             iter.hasNext() ; )
            ret+=((ManagedConnection)iter.next()).getTotalFileSize();
        return ret;
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
     * Searches Gnutellanet files of the given type with the given
     * query string and minimum speed.  If type is null, any file type
     * is acceptable.  Returns the GUID of the query request sent as a
     * 16 byte array, or null if there was a network error.
     * ActivityCallback is notified asynchronously of responses.
     * These responses can be matched with requests by looking at
     * their GUIDs.  (You may want to wrap the bytes with a GUID
     * object for simplicity.)  */
    public byte[] query(String query, int minSpeed, MediaType type) {
        QueryRequest qr=new QueryRequest(SettingsManager.instance().getTTL(),
                                         minSpeed, query);
        verifier.record(qr, type);
        router.broadcastQueryRequest(qr);
        return qr.getGUID();
    }

    /** Same as query(query, minSpeed, null), i.e.,
      * searches for files of any type. */
    public byte[] query(String query, int minSpeed) {
        return query(query, minSpeed, null);
    }

    /** Same as ResponseVerifier.score. */
    public int score(byte[] Guid, Response resp){
        return verifier.score(Guid,resp);
    }

    /** Same as ResponseVerifier.matchesType. */
    public boolean matchesType(byte[] guid, Response response) {
        return verifier.matchesType(guid, response);
    }

    /**
     * Searches the given host for all its files.  Results are given
     * to the GUI via handleQuery.  Returns null if the host couldn't
     * be reached.  Blocks until the connection is established and
     * the query is sent.
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
     *  Return an iterator on the hostcatcher hosts
     */
    public Iterator getHosts() {
        return catcher.getHosts();
    }

    /**
     *  Return the number of good hosts
     */
    public int getNumConnections() {
        return manager.getNumConnections();
    }

    /**
     *  Return the number searches made locally ( QReq )
     */
    public int getNumLocalSearches() {
        return router.getNumQueryRequests();
    }

    /**
     *  Remove unwanted or used entries from host catcher
     */
    public void removeHost(String host, int port) {
        catcher.removeHost(host, port);
    }

    /**
     * Returns an instance of a SettingsInterface
     */
    public SettingsInterface getSettings() {
        return SettingsManager.instance();
    }


    /**
     * Return how many files are being shared
     */
    public int getNumSharedFiles( ) {
        return( FileManager.instance().getNumFiles() );
    }

    /** 
     * Tries to "smart download" any of the given files.<p>  
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
     *     @modifies this, disk 
     */
    public Downloader download(RemoteFileDesc[] files, boolean overwrite) 
        throws FileExistsException, AlreadyDownloadingException {
        return downloader.getFiles(files, overwrite);
    }
}
