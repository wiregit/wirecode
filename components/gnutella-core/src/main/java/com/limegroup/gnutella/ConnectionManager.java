package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.Properties;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * The list of all Gnutella connections.  Because this is the only list of all
 * connections, it has an important role in message broadcasting.  Provides
 * factory methods for creating outgoing connnections (createConnectionX), a
 * method to accept incoming connections from the Acceptor (acceptConnection),
 * and threads to automatically fetch new connections.  The number of
 * connections is controlled by the setKeepAlive method, which is typically but
 * not necessarily the same as the KEEP_ALIVE property.<p>
 *
 * This particular version of ConnectionManager fetches new and old connections
 * independently, though both are ultimately added to the same broadcast list.
 * Here, "new connection" means one supporting query routing.  This enables you
 * to preference new connections while ensuring connections to the old
 * network.<p>
 * 
 * This class is thread-safe.  
 */
public class ConnectionManager {
    private MessageRouter _router;
    private HostCatcher _catcher;
    private ActivityCallback _callback;
	private SettingsManager _settings;

    /* List of all connections.  This is implemented with two data structures:
     * a list for fast iteration, and a set for quickly telling what we're
     * connected to.
     *
     * INVARIANT: "connections" contains no duplicates, and "endpoints" contains
     * exactly those endpoints that could be made from the elements of
     * "connections".
     *
     * INVARIANT: numFetchers = max(0, keepAlive - getNumConnections())
     *            Number of fetchers equals number of connections needed, unless
     *            that number is less than zero.
     *
     * LOCKING: connections and endpoints must NOT BE MUTATED.  Instead they
     *          should be replaced as necessary with new copies.  Before
     *          replacing the structures, obtain this' monitor.
     *          *** All six of the following members should only be modified
     *              from threads that have this' monitor ***
     */
    private volatile List /* of ManagedConnection */ _initializedConnections =
        new ArrayList();
    private volatile List /* of ManagedConnection */ _connections =
        new ArrayList();
    private volatile Set /* of Endpoint */ _endpoints = new HashSet();
	private ConnectionWatchdog _watchdog;


    /***********************************************************************
     * The remaining data structures come in pairs: one for new connections
     * and one for old connections.
     ***********************************************************************/

    private static final int NEW=0;
    private static final int OLD=1;
    /** 
     * The number of connections to keep up.  Initially we will try
     * _keepAlive outgoing connections.  At the same time we will accept up to
     * _keepAlive incoming connections.  As outgoing connections fail, we will
     * not accept any more incoming connections.  Hence we will converge on
     * exactly _keepAlive incoming connections.  
     */
    private volatile int[] _keepAlive=new int[2];
    /** The number of incoming connections.  Used to avoid the cost of
     * scanning through _initializedConnections when deciding whether to accept
     * incoming..
     *
     * INVARIANT: _incomingConnections>=the number of incoming connections in
     * _connections.  In the "steady state", i.e., when no incoming connections
     * are being initialized, this value is exactly equal to the number of
     * incoming connections.
     *
     * LOCKING: obtain _incomingConnectionLock 
     */
    private volatile int[] _incomingConnections=new int[2];
    private List[] /* of ConnectionFetcher */ _fetchers=new List[2];
    private List[] /* of ManagedConnection */ _initializingFetchedConnections
        = new List[2];
	private Runnable _ultraFastCheck;
    /** The lock for the number of incoming connnections. */
    private Object _incomingConnectionsLock=new Object();



    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     */
    public ConnectionManager(ActivityCallback callback) {
        _callback = callback;		
		_settings = SettingsManager.instance(); 
        _fetchers[OLD]=new ArrayList();
        _fetchers[NEW]=new ArrayList();
        _initializingFetchedConnections[OLD]=new ArrayList();  
        _initializingFetchedConnections[NEW]=new ArrayList();
    }

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initialize(MessageRouter router, HostCatcher catcher) {
        _router = router;
        _catcher = catcher;

        // Start a thread to police connections.
        // Perhaps this should use a low priority?
        _watchdog = new ConnectionWatchdog(this, _router);
        Thread watchdog=new Thread(_watchdog);
        watchdog.setDaemon(true);
        watchdog.start();

        setKeepAlive(_settings.getKeepAlive(), true);
        setKeepAlive(_settings.getKeepAliveOld(), false);
    }


    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.  The new connection will support
     * query routing and pong caching if possible, but speak plain Gnutella 0.4
     * otherwise.
     */
    public ManagedConnection createConnectionBlocking(
            String hostname, int portnum) throws IOException {
        ManagedConnection c = new ManagedConnection(
            hostname, portnum, _router, this, false,
            ManagedConnection.PROTOCOL_BEST);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        new OutgoingConnectionThread(c, false);

        return c;
    }

    /**
     * Create a new connection, allowing it to initialize and loop for messages
     * on a new thread.  The new connection will support query routing and pong
     * caching if possible, but speak plain Gnutella 0.4 otherwise.  
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {
        // Initialize and loop for messages on another thread.
        //TODO: should there be an option to specify new or old or both?
        new OutgoingConnectionThread(
                new ManagedConnection(hostname, portnum, _router, 
                                      this, false,
                                      ManagedConnection.PROTOCOL_BEST),
                true);
    }

    /**
     * Create and returns a new connection to a router, blocking until it's
     * initialized, but launching a new thread to do the message loop.  Throws
     * IOException if the connection couldn't be established.  The router
     * connection is treated like a normal connection, except that its pongs are
     * given higher priority.  
     */
    public ManagedConnection createRouterConnection(
            String hostname, int portnum) throws IOException {

		// Use dedicated pong server instead of defaul for LimeWire
		if ( hostname.equals(SettingsManager.DEFAULT_LIMEWIRE_ROUTER) ) {
			hostname = SettingsManager.DEDICATED_LIMEWIRE_ROUTER;
		}

        ManagedConnection c = 
		  new ManagedConnection(hostname, portnum, _router, this, true,
                                ManagedConnection.PROTOCOL_BEST);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        new OutgoingConnectionThread(c, false);

        return c;
    }

    /**
     * Create an incoming connection.  This method starts the message loop,
     * so it will block for a long time.  Make sure the thread that calls
     * this method is suitable doing a connection message loop.
     * If there are already too many connections in the manager, this method
     * will launch a RejectConnection to send pongs for other hosts.
     */
     void acceptConnection(Socket socket) {
         //TODO2: We need to re-enable the reject connection mechanism.  The
         //catch is that you don't know whether to reject for sure until you've
         //handshaked.  Basically RejectConnection is no longer sufficient, so I
         //propose eliminating it; just use a normal ManagedConnection but don't
         //add it to list of connections, and don't add it to gui.  This
         //requires some refactoring of that damn initialization code.

         //TODO2: as an optimization, you can check whether
         //_incomingConnections[i]>_keepAlive[i] for all i.  If so, you can
         //immediately reject, before initializing the connections.

         //1. Initialize connection.  It's always safe to recommend new headers.
         ManagedConnection connection=null;
         try {
             connection = new ManagedConnection(socket, _router, this,
                                                ManagedConnection.PROTOCOL_NEW);
             initializeExternallyGeneratedConnection(connection);
         } catch (IOException e) {
             return;
         }
         
         //2. Check if it supports query routing, and decide whether to keep.
         int isNew=(connection.getProperty("Query-Routing")!=null) ? NEW : OLD;
         try {   
             synchronized (_incomingConnectionsLock) {
                 _incomingConnections[isNew]++;
             }                  

             //a) Not needed: kill.  TODO: reject as described above.
             if (_incomingConnections[isNew]>_keepAlive[isNew])
                 synchronized (this) { removeInternal(connection); }
             //b) Normal case: accept.
             else {                     
                 sendInitialPingRequest(connection);
                 connection.loopForMessages();
             }
         } catch(IOException e) {
         } catch(Exception e) {
             //Internal error!
             _callback.error(ActivityCallback.INTERNAL_ERROR, e);
         } finally {
             synchronized (_incomingConnectionsLock) {
                 _incomingConnections[isNew]--;
             }
         }
     }

    /**
     * @modifies this, route table
     * @effects closes c and removes it from this' connection list and
     *  all corresponding errors from the route table.  If
     *  c is not in the connection list, does nothing.  May
     *  try to establish a new outgoing connection to replace
     *  this one.
     */
    public synchronized void remove(ManagedConnection c) {
        removeInternal(c);
        adjustConnectionFetchers(false);
        adjustConnectionFetchers(true);  //TODO3: only adjust one
    }

    /**
     * Get the number of connections wanted to be maintained
     */
    public int getKeepAlive() {
        //TODO: this should take an argument
        return _keepAlive[NEW];
    }

	/**
	 * this method reduces the number of connections
	 */
	public synchronized void reduceConnections() {
		setKeepAlive(Math.min(_keepAlive[NEW], 2), true);
        setKeepAlive(Math.min(_keepAlive[OLD], 2), false);
	}

    /**
     * Reset how many connections you want and start kicking more off if
     * required.  If isNew, the value refers to new connections only; otherwise
     * it refers to old connections only.  This IS synchronized because we don't
     * want threads adding or removing connections while this is deciding
     * whether to add more threads.  
     */
    public synchronized void setKeepAlive(int n, boolean isNew) {
        _keepAlive[isNew ? NEW : OLD]=n;
        adjustConnectionFetchers(isNew);
    }

    /**
     * Sets the maximum number of incoming connections.  This does not
     * affect the MAX_INCOMING_CONNECTIONS property.  It is useful to be
     * able to vary this without permanently setting the property.
     */
    //public void setMaxIncomingConnections(int max) {
	//_maxIncomingConnections = max;
    //}

    /**
     * @return true if there is a connection to the given host.
     */
    public boolean isConnected(Endpoint host) {
        return _endpoints.contains(host);
    }

    /**
     * @return the number of connections, which is greater than or equal
     *  to the number of initialized connections.
     */
    public int getNumConnections() {
        return _connections.size();
    }

   /**
    * Returns the number of (possibly uninitialized) connections to new (old)
    * clients.  In either case, the return value is less than or equal to the
    * number of connections.  
    */
    private int getNumConnections(boolean isNew) {
        //This is always safe to do since the list is never mutated.
        List _connectionsSnapshot=_connections;
        int ret=0;
        for (int i=0; i<_connectionsSnapshot.size(); i++) {
            ManagedConnection mc=
                (ManagedConnection)_connectionsSnapshot.get(i);
            if (mc.isOldClient()!=isNew)
                ret++;
        }
        return ret;
    }

    /**
     * @return the number of initialized connections, which is less than or 
     * equal to the number of connections.
     */
    public int getNumInitializedConnections() {
        return _initializedConnections.size();
    }

    /**
     * @return true if incoming connection slots are still available.
     */
    public boolean hasAvailableIncoming() {
        //TODO: should take a Properties argument!
        return (_incomingConnections[NEW] < _keepAlive[NEW]);
    }

    /**
     * @return a clone of this' initialized connections.
     * The iterator yields items in any order.  It <i>is</i> permissible
     * to modify this while iterating through the elements of this, but
     * the modifications will not be visible during the iteration.
     */
    public List getInitializedConnections() {
        List clone=new ArrayList();
        clone.addAll(_initializedConnections);
        return clone;
    }

    /**
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    List getInitializedConnections2() {
        return _initializedConnections;
    }

    /**
     * @return a clone of all of this' connections.
     * The iterator yields items in any order.  It <i>is</i> permissible
     * to modify this while iterating through the elements of this, but
     * the modifications will not be visible during the iteration.
     */
    public List getConnections() {
        List clone=new ArrayList(_connections);
        return clone;
    }

    /**
     * Adds an initializing connection.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection
     * and initializeFetchedConnection, both times from within a
     * synchronized(this) block.
     */
    private void connectionInitializing(Connection c) {
        //REPLACE _connections with the list _connections+[c]
        List newConnections=new ArrayList(_connections);
        newConnections.add(c);
        _connections=newConnections;
    }

    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     */
    private void connectionInitialized(Connection c) {
        if(_connections.contains(c)) {
            //REPLACE _initializedConnections with the list
            //_initializedConnections+[c]
            List newConnections=new ArrayList(_initializedConnections);
            newConnections.add(c);
            _initializedConnections=newConnections;

            //REPLACE _endpoints with the set _endpoints+{c}
            Set newEndpoints=new HashSet(_endpoints);
            newEndpoints.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                          c.getPort()));
            _endpoints=newEndpoints;
        }

		// Check for satisfied ultra-fast connection threshold
		if ( _ultraFastCheck != null )
			_ultraFastCheck.run();
    }

	/**
	 *  Activate the ultraFast runnable for returning keepAlive value to normal.
	 */
	public void activateUltraFastConnectShutdown() {
		_ultraFastCheck = new AllowUltraFastConnect();
	}

	/**
	 *  Deactivate the ultraFast runnable returning keepAlive value to normal.
	 */
	public void deactivateUltraFastConnectShutdown() {
		_ultraFastCheck = null;
	}

    /** 
     * Sends initial ping requests to the connection.  First ping request has a
     * TTL of 1 for handshaking purposes if needed.  Then, send another ping
     * request with TTL of 7 to get some pongs back.  
     */
    private void sendInitialPingRequest(ManagedConnection connection) {
        //Send a handshake ping to incoming connections in order to discover
        //their ports.  (We only know the ephemeral ports.)  These ports can be
        //given to crawlers later.
        //
        //There are two reasons for not sending pongs to outgoing connections.
        //First, we already know their ports--after all, we just connected to
        //them--so it would be wasteful.  More importantly, a pong cache like
        //router.limewire.com will send many pongs in response to a handshake
        //ping--not just its own address.  MessageRouter.handlePingReply would
        //not know whether to add these pongs to its pong cache or to call
        //connection.setRemotePong.  You might think to call
        //connection.isRouterConnection, but that is not sufficient.  For
        //example, the user may have typed "router.limewire.com" directly into
        //the GUI.
        if (!connection.isOutgoing()) {
            PingRequest handshake = new PingRequest((byte)1);
            //record the GUID of the handshake ping
            connection.setHandshakeGUID(handshake.getGUID());
            connection.send(handshake);
        }
        //Send a full-fledged ping.  TODO3: don't send if you don't need pongs,
        //especially if connection doesn't support pong caching.
        PingRequest initialPing = 
            new PingRequest((byte)MessageRouter.MAX_TTL_FOR_CACHE_REFRESH);
        connection.send(initialPing);
        //Ensure that the initial ping request is written in a timely fashion.
        try {
            connection.flush();
        } catch (IOException e) { /* close it later */ }
    }

    /**
     * Returns whether a connection is to a pong cache server such as router.
     * limewire.com or gnutellahosts.com
     */
    private boolean isRouterConnection(ManagedConnection connection) {
        String host = connection.getOrigHost();
        int port = connection.getOrigPort();
        String conn = new String(host + ":" + port);
        
        String[] routers = SettingsManager.instance().getQuickConnectHosts();
        for (int i = 0; i < routers.length; i++) {
            if (conn.equals(routers[i]))
                return true;
        }

        return false;
    }

    /**
     * This Runnable resets the KeepAlive to the appropriate value
	 * if there are an acceptible number of stable connections
     */
    private class AllowUltraFastConnect implements Runnable {
		
		public void run() {
            SettingsManager settings=SettingsManager.instance();
        	int outgoing=settings.getKeepAlive();
			int desired = Math.min(outgoing, 3);

			// Determine if we have 3/desired stable connections
            Iterator iter=getConnections().iterator();
            for ( ; iter.hasNext(); ) {
                ManagedConnection c=(ManagedConnection)iter.next();
				// Stable connections are measured by having 4 incoming msgs
			    if ( c.getNumMessagesReceived() >= 4 )
				    desired--;
            }
			if ( desired <= 0 ) {
        	    setKeepAlive(outgoing, true);
				// Deactivate extra ConnectionWatchdog Process
		        deactivateUltraFastConnectShutdown(); 
			}
		}
	}

    /**
     * An unsynchronized version of remove, meant to be used when the monitor
     * is already held.  This version does not kick off ConnectionFetchers;
     * only the externally exposed version of remove does that.
     */
    private void removeInternal(ManagedConnection c) {
        // 1a) Remove from the initialized connections list and clean up the
        // stuff associated with initialized connections.  For efficiency 
        // reasons, this must be done before (2) so packets are not forwarded
        // to dead connections (which results in lots of thrown exceptions).
        int i=_initializedConnections.indexOf(c);
        if (i != -1) {
            //REPLACE _initializedConnections with the list
            //_initializedConnections-[c]
            List newConnections=new ArrayList();
            newConnections.addAll(_initializedConnections);
            newConnections.remove(c);
            _initializedConnections=newConnections;

            //REPLACE _endpoints with the set _endpoints+{c}
            Set newEndpoints=new HashSet();
            newEndpoints.addAll(_endpoints);
            newEndpoints.remove(new Endpoint(
                c.getInetAddress().getHostAddress(), c.getPort()));
            _endpoints=newEndpoints;           
        }
        // 1b) Remove from the all connections list and clean up the
        // stuff associated all connections
        i=_connections.indexOf(c);
        if (i != -1) {
            //REPLACE _connections with the list _connections-[c]
            List newConnections=new ArrayList(_connections);
            newConnections.remove(c);
            _connections=newConnections;
        }

        // 2) Ensure that the connection is closed.  This must be done before
        // step (3) to ensure that dead connections are not added to the route
        // table, resulting in dangling references.
        c.close();

        // 3) Clean up route tables.
        _router.removeConnection(c);

        // 4) Notify the listener
        _callback.connectionClosed(c); 
    }

    /**
     * Starts or stops connection fetchers to maintain the invariant
     * that numConnections + numFetchers >= _keepAlive
     *
     * _keepAlive - numConnections - numFetchers is called the need.  This
     * method is called whenever the need changes:
     *   1. setKeepAlive() -- _keepAlive changes
     *   2. remove(Connection) -- numConnections drops.
     *   3. initializeExternallyGeneratedConnection() --
     *        numConnections rises.
     *   4. initialization error in initializeFetchedConnection() --
     *        numConnections drops when removeInternal is called.
     *   Note that adjustConnectionFetchers is not called when a connection is
     *   successfully fetched from the host catcher.  numConnections rises,
     *   but numFetchers drops, so need is unchanged.
     *
     * Only call this method when the monitor is held.
     */
    private void adjustConnectionFetchers(boolean isNew) {
        int i=isNew ? NEW : OLD;
        int need=_keepAlive[i]-getNumConnections(isNew)-_fetchers[i].size();
        
        // Start connection fetchers as necessary
        while(need > 0) {
            // This kicks off a thread and register the fetcher in the list
            new ConnectionFetcher(isNew); 
            need--;
        }

        // Stop ConnectionFetchers as necessary, but it's possible there
        // aren't enough fetchers to stop.  In this case, close some of the
        // connections started by ConnectionFetchers.
        int lastFetcherIndex = _fetchers[i].size();

        while((need < 0) && (lastFetcherIndex > 0)) {
            ConnectionFetcher fetcher = (ConnectionFetcher)
            _fetchers[i].remove(--lastFetcherIndex);
            fetcher.interrupt();
            need++;
        }
        int lastInitializingConnectionIndex =
        _initializingFetchedConnections[i].size();
        while((need < 0) && (lastInitializingConnectionIndex > 0)) {
            ManagedConnection connection = (ManagedConnection)
            _initializingFetchedConnections[i].remove(
                --lastInitializingConnectionIndex);
            removeInternal(connection);
            need++;
        }
    }

    /**
     * Initializes an outgoing connection created by a ConnectionFetcher
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeFetchedConnection(ManagedConnection c,
                                             ConnectionFetcher fetcher)
            throws IOException {
        int isNew=fetcher._isNew ? NEW : OLD;
        synchronized(this) {
            if(fetcher.isInterrupted()) 
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so throw IOException.
                // (This prevents fetcher from continuing!)
                throw new IOException();

            _initializingFetchedConnections[isNew].add(c);
            _fetchers[isNew].remove(fetcher);
            connectionInitializing(c);
            // No need to adjust connection fetchers here.  We haven't changed
            // the need for connections; we've just replaced a ConnectionFetcher
            // with a Connection.
        }
        _callback.connectionInitializing(c);

        try {
            c.initialize();
        } catch(IOException e) {
            synchronized(ConnectionManager.this) {
                _initializingFetchedConnections[isNew].remove(c);
                removeInternal(c);
                // We've removed a connection, so the need for connections went
                // up.  We may need to launch a fetcher.
                adjustConnectionFetchers(fetcher._isNew);
            }
            throw e;
        }

        boolean connectionOpen = false;
        synchronized(this) {
            _initializingFetchedConnections[isNew].remove(c);
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            if(_connections.contains(c)) {
                connectionInitialized(c);
                connectionOpen = true;
            }
        }
        if(connectionOpen)
            _callback.connectionInitialized(c);
    }

    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c)
            throws IOException {
        synchronized(this) {
            connectionInitializing(c);
            // We've added a connection, so the need for connections went down.
            adjustConnectionFetchers(true);
            adjustConnectionFetchers(false); //TODO3: you can remove one
        }
        _callback.connectionInitializing(c);

        try {
            c.initialize();
        } catch(IOException e) {
            remove(c);
            throw e;
        }

        boolean connectionOpen = false;
        synchronized(this) {
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            if(_connections.contains(c)) {
                connectionInitialized(c);
                connectionOpen = true;
            }
        }
        if(connectionOpen)
            _callback.connectionInitialized(c);
    }

    //
    // End connection list management functions
    //


    //
    // Begin connection launching thread inner classes
    //

    /**
     * This thread does the initialization and the message loop for
     * ManagedConnections create through createConnectionAsynchronously and
     * createConnectionBlocking
     */
    private class OutgoingConnectionThread
            extends Thread {
        private ManagedConnection _connection;
        private boolean _doInitialization;

        /**
         * The constructor calls start(), so allow you need to do
         * is construct the thread.
         */
        public OutgoingConnectionThread(
                ManagedConnection connection,
                boolean doInitialization) {
            _connection = connection;
            _doInitialization = doInitialization;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                if(_doInitialization)
                    initializeExternallyGeneratedConnection(_connection);

				// Send GroupPingRequest to router
				String origHost = _connection.getOrigHost();
				if (origHost != null && 
                    origHost.equals(SettingsManager.DEDICATED_LIMEWIRE_ROUTER))
				{
				    String group = "none:"+_settings.getConnectionSpeed();
				    PingRequest pingRequest = _router.createGroupPingRequest(group);
                    _connection.send(pingRequest);
				}
				else
                {
                    //send normal ping request (handshake or broadcast depending
                    //on num of connections and reserve cache size.
                    sendInitialPingRequest(_connection);
                }
                // Add any pongs sent in Old-Pongs header.
                _catcher.addOldPongs(_connection);
                _connection.loopForMessages();
                //Ensure that the initial ping request is written in a timely fashion
                try {
                    _connection.flush();
                } catch (IOException e) { /* close it later */ }
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
        }
    }

    //------------------------------------------------------------------------
    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public ManagedConnection createGroupConnectionBlocking(
      String hostname, int portnum, GroupPingRequest specialPing) 
	  throws IOException {
        //TODO: make sure server is upgraded so isNew=true works.
        ManagedConnection c = 
		  new ManagedConnection(hostname, portnum, _router, this, true,
                                ManagedConnection.PROTOCOL_BEST);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        new GroupOutgoingConnectionThread(c, specialPing);

        return c;
    }

    /**
     * This thread does the message loop for ManagedConnections created
     * through createGroupConnectionBlocking
     */
    private class GroupOutgoingConnectionThread
            extends Thread {
        private ManagedConnection _connection;
        private PingRequest       _specialPing;

        /**
         * The constructor calls start(), so allow you need to do
         * is construct the thread.
         */
        public GroupOutgoingConnectionThread(
                ManagedConnection connection, PingRequest specialPing) {
            _connection  = connection;
            _specialPing = specialPing;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                _connection.send(_specialPing );
                _connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }

            //SettingsManager settings = SettingsManager.instance();
		    //settings.setUseQuickConnect(true);
            //setKeepAlive(2);
        }
    }
	//------------------------------------------------------------------------

    /**
     * Asynchronously fetches a connection from PingReplyCache, then does
     * then initialization and message loop.
     *
     * The ConnectionFetcher is responsible for recording its instantiation
     * by adding itself to the fetchers list.  It is responsible  for recording
     * its death by removing itself from the fetchers list only if it
     * "interrupts itself", that is, only if it establishes a connection. If
     * the thread is interrupted externally, the interrupting thread is
     * responsible for recording the death.
     */
    private class ConnectionFetcher
            extends Thread {
        boolean _isNew;

        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher(boolean isNew) {
            // Record the fetcher creation
            _fetchers[isNew ? NEW : OLD].add(this);
            this._isNew=isNew;

            // Kick off the thread.
            setDaemon(true);
            start();
        }

        // Try a single connection
        public void run() {
            // Wait for an endpoint.
            Endpoint endpoint = null;

            do {
                try {
                    endpoint = _catcher.getAnEndpoint(_isNew);
                } catch (InterruptedException exc2) {
                    // Externally generated interrupt.
                    // The interrupting thread has recorded the
                    // death of the fetcher, so just return.
                    return;
                }               
            } while ( (isConnected(endpoint)) || 
                      (Acceptor.isMe(endpoint.getHostname(), 
                       endpoint.getPort())) );

            Assert.that(endpoint != null);

            ManagedConnection connection = new ManagedConnection(
                endpoint.getHostname(), endpoint.getPort(), _router,
                ConnectionManager.this, false,
                _isNew ? ManagedConnection.PROTOCOL_NEW 
                       : ManagedConnection.PROTOCOL_OLD);

            try {
                initializeFetchedConnection(connection, this);
                sendInitialPingRequest(connection);
                connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
        }
    }

    //
    // End connection launching thread inner classes
    //
}
