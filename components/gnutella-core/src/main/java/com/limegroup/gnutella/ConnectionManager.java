package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.gui.Utilities;

/**
 * The list of all connections.  Accepts new connections and creates
 * new threads to handle them.<p>
 *
 * You should call the shutdown() method when you're done to ensure
 * that the gnutella.net file is written to disk.
 */
public class ConnectionManager {
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
    private List /* of ConnectionFetcher */ _fetchers =
        new ArrayList();
    private List /* of ManagedConnection */ _initializingFetchedConnections =
        new ArrayList();

    /** 
     * The number of connections to keep up.  Initially we will try _keepAlive
     * outgoing connections.  At the same time we will accept up to _keepAlive
     * incoming connections.  As outgoing connections fail, we will not accept
     * any more incoming connections.  Hence we will converge on exactly
     * _keepAlive incoming connections.  
     */
    private volatile int _keepAlive=0;
    /** The number of incoming connections.  Used to avoid the cost of scanning
     * through _initializedConnections when deciding whether to accept incoming..
     *
     *  INVARIANT: _incomingConnections>=the number of incoming connections in
     *  _connections.  In the "steady state", i.e., when no incoming connections
     *  are being initialized, this value is exactly equal to the number of
     *  incoming connections.
     *
     *  LOCKING: obtain _incomingConnectionLock */
    private int _incomingConnections=0;
    /** The lock for the number of incoming connnections. */
    private Object _incomingConnectionsLock=new Object();

    private MessageRouter _router;
    private HostCatcher _catcher;
    private ActivityCallback _callback;
	private SettingsManager _settings;
	private ConnectionWatchdog _watchdog;

    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     */
    public ConnectionManager(ActivityCallback callback) {
        _callback = callback;		
		_settings = SettingsManager.instance(); 
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

        setKeepAlive(_settings.getKeepAlive());
        //setMaxIncomingConnections(
		//SettingsManager.instance().getMaxIncomingConnections());
    }

	/**
	 *  Set a time trigger for a runnable action relative to current time
	 *  Pass this on to the ConnectionWatchdog.
     *  @param incrementalTime the incremental time of the event in milliseconds
     *  @param action the Runnable for the event
	 */
	public void setTimeEvent(long incrementalTime, Runnable action) {
		_watchdog.setTimeEvent(incrementalTime, action);
	}

    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public ManagedConnection createConnectionBlocking(
            String hostname, int portnum) throws IOException {
        ManagedConnection c = new ManagedConnection(hostname, portnum, _router,
                                                    this);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        new OutgoingConnectionThread(c, false);

        return c;
    }

    /**
     * Create a new connection, allowing it to initialize and loop for messages
     * on a new thread.
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {
        // Initialize and loop for messages on another thread.
        new OutgoingConnectionThread(
                new ManagedConnection(hostname, portnum, _router, this),
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
        ManagedConnection c = new ManagedConnection(hostname, portnum, _router,
                                                    this, true);
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
         //Atomically decide whether to accept connection.  Release lock
         //before actually managing the connection.
         boolean allowConnection=false;
         synchronized (_incomingConnectionsLock) {
             if (_incomingConnections < _keepAlive) {
                 //Yes, we'll allow it.  Increment the incoming count NOW even
                 //before this connection is processed.  The value will be
                 //decremented in the finally clause below.
                 _incomingConnections++;
                 allowConnection=true;
             }
         }
         
         if (allowConnection) {
             //a) Handle normal connection (blocking).
             ManagedConnection connection = new ManagedConnection(
                 socket, _router, this);
             try {                     
                 initializeExternallyGeneratedConnection(connection);
                 //We DO send ping requests on incoming connections.  This may
                 //double ping traffic, but if gives us more accurate horizon
                 //stats.  And it won't really affect traffic if people implement
                 //caching properly.
                 _router.sendPingRequest(new PingRequest(_settings.getTTL()),
                                         connection);
                 connection.loopForMessages();
             } catch(IOException e) {
             } catch(Exception e) {
                 //Internal error!
                 _callback.error(ActivityCallback.ERROR_20, e);
             } finally {
                 synchronized (_incomingConnectionsLock) {
                     _incomingConnections--;
                 }
             }
         } else {
             //b) Handle reject connection if on windows.  The constructor does the
             //whole deal -- intializing, looking for and responding to a
             //PingRequest.  It's all synchronous, because we have a dedicated
             //thread right here.
			if(Utilities.isWindows()) {
				new RejectConnection(socket, _catcher);
			}
            
			// Otherwise, we're not on windows.  We did this 
			// because we know that RejectConnection was causing
			// problems on the Mac (periodically freezing the
			// system and leading to a 40% approval rating on
			// download.com), and we have not been able to test
			// it on other systems.  Since we know that not using
			// a reject connection will not cause a problem, then
			// we might as well just be safe and not use one on 
			// non-windows systems.
			else {
				try {
					socket.close();
				}
				catch(IOException ioe) {}
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
        adjustConnectionFetchers();
    }

    /**
     * Get the number of connections wanted to be maintained
     */
    public int getKeepAlive() {
        return _keepAlive;
    }

    /**
     * Reset how many connections you want and start kicking more off
     * if required.  This IS synchronized because we don't want threads
     * adding or removing connections while this is deciding whether
     * to add more threads.
     */
    public synchronized void setKeepAlive(int newKeep) {
        _keepAlive = newKeep;
        adjustConnectionFetchers();
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
     * @return the number of connections
     */
    public int getNumConnections() {
        return _connections.size();
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
    private void adjustConnectionFetchers() {
        int need = _keepAlive - getNumConnections() - _fetchers.size();

        // Start connection fetchers as necessary
        while(need > 0) {
            new ConnectionFetcher(); // This kicks off a thread and registers
                                     // the fetcher in the list
            need--;
        }

        // Stop ConnectionFetchers as necessary, but it's possible there
        // aren't enough fetchers to stop.  In this case, close some of the
        // connections started by ConnectionFetchers.
        int lastFetcherIndex = _fetchers.size();
        while((need < 0) && (lastFetcherIndex > 0)) {
            ConnectionFetcher fetcher = (ConnectionFetcher)
                _fetchers.remove(--lastFetcherIndex);
            fetcher.interrupt();
            need++;
        }
        int lastInitializingConnectionIndex =
            _initializingFetchedConnections.size();
        while((need < 0) && (lastInitializingConnectionIndex > 0)) {
            ManagedConnection connection = (ManagedConnection)
                _initializingFetchedConnections.remove(
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
        synchronized(this) {
            if(fetcher.isInterrupted())
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so throw IOException.
                // (This prevents fetcher from continuing!)
                throw new IOException();

            _initializingFetchedConnections.add(c);
            _fetchers.remove(fetcher);
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
                _initializingFetchedConnections.remove(c);
                removeInternal(c);
                // We've removed a connection, so the need for connections went
                // up.  We may need to launch a fetcher.
                adjustConnectionFetchers();
            }
            throw e;
        }

        boolean connectionOpen = false;
        synchronized(this) {
            _initializingFetchedConnections.remove(c);
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
            adjustConnectionFetchers();
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

				PingRequest pingRequest;

				// Send GroupPingRequest to router
				String origHost = _connection.getOrigHost();
				if (origHost != null && origHost.equals("router.limewire.com"))
				{
				    String group = "none:"+_settings.getConnectionSpeed();
				    pingRequest = _router.createGroupPingRequest(group);
				}
				else
				    pingRequest = 
				      new PingRequest(_settings.getTTL());

                _router.sendPingRequest(pingRequest, _connection);
                _connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.ERROR_20, e);
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
        ManagedConnection c = 
		  new ManagedConnection(hostname, portnum, _router, this, true);

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
                _router.sendPingRequest(_specialPing, _connection);
                _connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.ERROR_20, e);
            }

            //SettingsManager settings = SettingsManager.instance();
		    //settings.setUseQuickConnect(true);
            //setKeepAlive(2);
        }
    }
	//------------------------------------------------------------------------

    /**
     * Asynchronously fetches a connection from hostcatcher, then does
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
        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            // Record the fetcher creation
            _fetchers.add(this);

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
                    endpoint = _catcher.getAnEndpoint();
                } catch (InterruptedException exc2) {
                    // Externally generated interrupt.
                    // The interrupting thread has recorded the
                    // death of the fetcher, so just return.
                    return;
                }               
            } while (isConnected(endpoint));

            Assert.that(endpoint != null);

            ManagedConnection connection = new ManagedConnection(
                endpoint.hostname, endpoint.port, _router,
                ConnectionManager.this);

            try {
                initializeFetchedConnection(connection, this);
                _router.sendPingRequest(
                    new PingRequest(_settings.getTTL()),
                    connection);
                connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.ERROR_20, e);
            }
        }
    }

    //
    // End connection launching thread inner classes
    //
}
