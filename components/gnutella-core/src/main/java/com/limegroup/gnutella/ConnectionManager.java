package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import java.util.Properties;
import java.util.StringTokenizer;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.handshaking.*;

/**
 * The list of all ManagedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, accepts incoming connections, and
 * fetches "automatic" outgoing connections as needed.  Creates threads for
 * handling these connections when appropriate.  Use the setKeepAlive(int)
 * method control the number of connections; modifying the KEEP_ALIVE property
 * of SettingsManager does not automatically affect this.<p>
 * 
 * Because this is the only list of all connections, it plays an important role
 * in message broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitializedConnections() methods.  Adding and removing
 * connections is a slower operation.  
 * 
 * ConnectionManager has methods to get up and downstream bandwidth, but it 
 * doesn't quite fit the BandwidthTracker interface.
 */
public class ConnectionManager {
    
    /*********** IMPORTANT NOTE ABOUT THIS CLASS *************
     * The acceptConnection() method is a template method, and the behaviour
     * is modified in the subclasses by providing alternate implementations 
     * for the 'protected' methods called from this method, as well as from the
     * methods invoked from this method.
     *
     * Therefore, please be lenient while thinking of changing the method names
     * of the 'protected' methods of this class, as well as their use. The
     * implementation of these methods can be changed (as long as the
     * specifications are not changed.
     *
     * NOTE: Note that we could have used Template Design Pattern over here,
     * and could move the algorithm to the abstract class, but after 
     * discussions with other developers, it was observed that the subclasses
     * of this class dont really wanna provide a different implementation,
     * but want to handle special cases, and fall back on the implementations 
     * in this class for normal cases. Also, most of the methods depend a
     * lot on the private variables. Therefore having abstract class wont
     * necessarily help too much.
     * 
     * Please send an email to asingla@limewire.com, if and when you plan to 
     * change the behaviour of these methods.
     **********************************************************
     */
    
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
     * List of connections to the shielded clients.
     * INVARIANTS: 
     * 1. _initializedConnections {intersection} _initializedClientConnections
     * = NULL
     * <p>
     * 2. _connections is a superset of _initializedClientConnections
     * 3. {Connection}.isClientConection == true iff its a client connection
     */
    private volatile List /* of ManagedConnection */ 
        _initializedClientConnections = new ArrayList();

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
    private volatile int _incomingConnections=0;
    private volatile int _incomingClientConnections = 0;
    /** The lock for the number of incoming connnections. */
    private Object _incomingConnectionsLock=new Object();

    private MessageRouter _router;
    private HostCatcher _catcher;
    private ActivityCallback _callback;
	private SettingsManager _settings;
	private ConnectionWatchdog _watchdog;
	private Runnable _ultraFastCheck;

    /**
     * For authenticating users
     */
    private Authenticator _authenticator;

    /**
     * Minimum umber of connections that a supernode with leaf connections,
     * must have
     */
    public static final int MIN_CONNECTIONS_FOR_SUPERNODE = 6;
    
    /** The maximum number of ultrapeer endpoints to give out from the host
     *  catcher in X_TRY_SUPERNODES headers. */
    private int MAX_SUPERNODE_ENDPOINTS=10;

    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     * @param authenticator Authenticator instance for authenticating users
     */
    public ConnectionManager(ActivityCallback callback, 
        Authenticator authenticator) {
        _callback = callback;		
        this._authenticator = authenticator; 
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

		if(_settings.getConnectOnStartup()) {
			setKeepAlive(_settings.getKeepAlive());
		}
        //setMaxIncomingConnections(
		//SettingsManager.instance().getMaxIncomingConnections());
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
            
		// Use dedicated pong server instead of defaul for LimeWire
		if ( hostname.equals(SettingsManager.DEFAULT_LIMEWIRE_ROUTER) ) {
			hostname = SettingsManager.DEDICATED_LIMEWIRE_ROUTER;
		}

        ManagedConnection c = 
		  new ManagedConnection(hostname, portnum, _router, this, true);

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
         //1. Initialize connection.  It's always safe to recommend new headers.
         ManagedConnection connection=null;
         try {
             connection = new ManagedConnection(socket, _router, this);
             initializeExternallyGeneratedConnection(connection);
         } catch (IOException e) {
             if(connection != null){
                    connection.close();
             }
             return;
         }
         
         //update the connection count
         try {   
             //increment the appropriate connection count to reflect the
             //addition of this new connection
             incrementConnectionCount(connection, false);    
             //keep handling the messages on the connection

             //if a leaf connected to us, ensure that we have enough non-leaf
             //connections opened
             if(connection.isSupernodeClientConnection())
                ensureConnectionsForSupernode();
             
             sendInitialPingRequest(connection);
             connection.loopForMessages();
         } catch(IOException e) {
         } catch(Exception e) {
             //Internal error!
             _callback.error(ActivityCallback.INTERNAL_ERROR, e);
         } finally {
            //increment the appropriate connection count to reflect the
            //addition of this new connection
            decrementConnectionCount(connection, false);  
            //if we were leaf to a supernode, reconnect to network 
            if (connection.isClientSupernodeConnection())
                lostShieldedClientSupernodeConnection();
         }
     }

     /**
      * Increments the appropriate connection count, based upon the 
      * type of the new connection opened
      * (e.g. incoming supernode, outgoing supernode, outgoing leaf etc.)
      * @param connection The new connection we received that led to
      * incrementing the count
      * @param outgoing Flag indicating if the connection is outgoing 
      * or incoming. True means outgoing, False means incoming
      */
     protected void incrementConnectionCount(ManagedConnection connection,
        boolean outgoing) {
        if(!outgoing) {
            synchronized (_incomingConnectionsLock) {
                 if(connection.isSupernodeClientConnection())
                     _incomingClientConnections++;
                 else
                     _incomingConnections++;
             }                 
        }
     }

     /**
      * Decrements the appropriate connection count, based upon the 
      * type of the connection closed
      * (e.g. incoming supernode, outgoing supernode, outgoing leaf etc.)
      * @param connection The connection closed that led to
      * decrementing the count
      * @param outgoing Flag indicating if the connection was outgoing 
      * or incoming. True means outgoing, False means incoming
      */
     protected void decrementConnectionCount(ManagedConnection connection,
        boolean outgoing) {
        if(!outgoing) {
            synchronized (_incomingConnectionsLock) {
                 if(connection.isSupernodeClientConnection())
                     _incomingClientConnections--;
                 else
                     _incomingConnections--;
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
	 * this method reduces the number of connections
	 */
	public synchronized void reduceConnections() {
		int newKeepAlive = Math.min(_keepAlive, 2);
		setKeepAlive(newKeepAlive);
	}

    /**
     * Reset how many connections you want and start kicking more off
     * if required.  This IS synchronized because we don't want threads
     * adding or removing connections while this is deciding whether
     * to add more threads.  Ignores request if a shielded leaf node
     * and newKeep>1 (sic).
     */
    public synchronized void setKeepAlive(int newKeep) {
        //The request for increasing keep alive if we are leaf node is invalid.
        //This logic is duplicated in RouterService.setKeepAlive.
        if ((newKeep > 1) && hasClientSupernodeConnection())
            return;
        
        _keepAlive = newKeep;
        adjustConnectionFetchers();
    }
    
    /**
     * Ensures that if a node is acting as supernode, it has atleast 
     * some minimum number of connections opened
     */ 
    public synchronized void ensureConnectionsForSupernode(){
        //Note: not holding the _incomingConnectionsLock as just reading the 
        //volatile value
        if(_incomingClientConnections > 0 
            && _keepAlive < MIN_CONNECTIONS_FOR_SUPERNODE){
            setKeepAlive(MIN_CONNECTIONS_FOR_SUPERNODE);
        }
    }
    
    /**
     * Tells whether the node is gonna be a supernode or not
     * @return true, if supernode, false otherwise
     */
    public boolean isSupernode() {
        boolean isCapable=SettingsManager.instance().getEverSupernodeCapable();
        return isCapable && !hasClientSupernodeConnection();
    }
    
    /**
     * Returns true if this is a leaf node with a connection to a supernode.  It
     * is not required that the supernode support query routing, though that is
     * generally the case.  
     */
    public synchronized boolean hasClientSupernodeConnection() {
        //TODO3: it may be possible to remove the synchronized statement here,
        //depending on whether Java ALWAYS stores the value of
        //_incomingConnections in a register before proceeding.
        List connections=_initializedConnections;
        if (connections.size()!=1)
            return false;
        else {
            ManagedConnection first=(ManagedConnection)connections.get(0);
            return first.isClientSupernodeConnection();
        }
    }
    
    /**
     * Returns true if this is a super node with a connection to a leaf. 
     */
    public boolean hasSupernodeClientConnection() {
        if(_incomingClientConnections > 0)
            return true;
        else
            return false;
    }
    
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
     * @return the number of initialized connections, which is less than or equals
     *  to the number of connections.
     */
    private int getNumInitializedConnections() {
		return _initializedConnections.size();
    }

	/**
	 * Returns whether or not the client has an established connection with
	 * another Gnutella client.
	 *
	 * @return <tt>true</tt> if the client is currently connected to 
	 *  another Gnutella client, <tt>false</tt> otherwise
	 */
	public boolean isConnected() {
		return ((_initializedClientConnections.size() > 0) ||
				(_initializedConnections.size() > 0));
	}

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public void measureBandwidth() {
        List connections=getInitializedConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc=(ManagedConnection)iter.next();
            mc.measureBandwidth();
        }
    }

    /**
     * Returns the upstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredUpstreamBandwidth() {
        float sum=0.f;
        List connections=getInitializedConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc=(ManagedConnection)iter.next();
            sum+=mc.getMeasuredUpstreamBandwidth();
        }
        return sum;
    }

    /**
     * Returns the downstream bandwidth between the last two calls to
     * measureBandwidth.
     * @see BandwidthTracker#measureBandwidth 
     */
    public float getMeasuredDownstreamBandwidth() {
        float sum=0.f;
        List connections=getInitializedConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc=(ManagedConnection)iter.next();
            sum+=mc.getMeasuredDownstreamBandwidth();
        }
        return sum;
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, supernode, 
     * temporary etc). 
     * @param receivedConnection The connection we received, for which to 
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    public boolean hasAvailableIncoming(ManagedConnection receivedConnection) {
        return hasAvailableIncoming(
            receivedConnection.isSupernodeClientConnection());
    }
    
    /**
     * Checks if there is any available slot of any kind.
     * @return true, if we have incoming slot of some kind,
     * false otherwise
     */
    public boolean hasAnyAvailableIncoming() {
        //return true, if there's an available Ultrapeer OR leaf slot
        return (hasAvailableIncoming(false) || 
            (isSupernode() && hasAvailableIncoming(true)));
    }
    
    /**
     * If leaf==true, returns true if this has slots for incoming leaf
     * connections.  If leaf==false, returns false if this has slots 
     * for incoming supernode or 0.4 connections.  Does not account for
     * supernode capabilities in this decision.
     */
    private boolean hasAvailableIncoming(boolean leaf) {
        SettingsManager settings=SettingsManager.instance();
        //Don't allow anything if disconnected or shielded leaf.  This rule is
        //critical to the working of gotShieldedClientSupernodeConnection.
        if (_keepAlive<=0)
            return false;
        else if (hasClientSupernodeConnection())
            return false;
        else if (leaf) {
            //As the spec. says, this assumes we are in supernode mode.
            int shieldedMax=
                SettingsManager.instance().getMaxShieldedClientConnections();
            return _incomingClientConnections < shieldedMax;
        } else {
            return _incomingConnections < _keepAlive;
        }
    }
    
    
    /**
     * Provides handle to the activity callback
     * @return Handle to the activity callback
     */
    public ActivityCallback getCallback(){
        return _callback;
    }

    /**
     * Tells if this node thinks that more supernodes are needed on the 
     * network. This method should be invoked on a supernode only, as
     * only supernode may have required information to make informed
     * decision.
     * @return true, if more supernodes needed, false otherwise
     */
    public boolean supernodeNeeded(){
        //if more than 70% slots are full, return true 
        if(isSupernode() &&
            _incomingClientConnections > 
            (SettingsManager.instance()
            .getMaxShieldedClientConnections() * 0.7)){
            return true;
        }else{
            //else return false
            return false;
        }
    }
    
    /**
     * Provides handle to the authenticator instance
     * @return Handle to the authenticator
     */
    public Authenticator getAuthenticator(){
        return _authenticator;
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
     * @return a clone of this' initialized connections to shielded-clients.
     * The iterator yields items in any order.  It <i>is</i> permissible
     * to modify this while iterating through the elements of this, but
     * the modifications will not be visible during the iteration.
     */
    public List getInitializedClientConnections() {
        List clone=new ArrayList();
        clone.addAll(_initializedClientConnections);
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
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    public List getInitializedClientConnections2() {
        return _initializedClientConnections;
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
     * Returns the endpoints of the best known ultrapeers.  This include
     * both ultrapeers we are connected to and marked ultrapeer pongs.
     * @return Returns the endpoints it is connected to. 
     */ 
    public Set getSupernodeEndpoints(){
        Set retSet = new HashSet();
        //get an iterator over _initialized connections, and iterate to
        //fill the retSet with supernode endpoints
        for(Iterator iterator = _initializedConnections.iterator();
            iterator.hasNext();)
        {
            ManagedConnection connection = (ManagedConnection)iterator.next();
            if(connection.isSupernodeConnection())
                retSet.add(new Endpoint(
                    connection.getInetAddress().getAddress(),
                    connection.getOrigPort()));
        }
        //add the best few endpoints from the hostcatcher.  TODO: limit this
        //to N entries?
        Iterator iterator=_catcher.getBestHosts();
        for (int i=0; iterator.hasNext() && i<MAX_SUPERNODE_ENDPOINTS; i++) {
            Endpoint e=(Endpoint)iterator.next();
            retSet.add(e);
        }
        return retSet;
    }
    
    /**
     * @return Returns endpoint representing its own address and port
     */
    public Endpoint getSelfAddress()
    {
       return new Endpoint(_router.getAddress(), _router.getPort()); 
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
     * Adds an incoming connection to the list of connections. Note that
     * the incoming connection has already been initialized before 
     * this method is invoked.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection, for 
     * incoming connections
     */
    protected void connectionInitializingIncoming(ManagedConnection c) {
        connectionInitializing(c);
    }
    
    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     */
    private void connectionInitialized(ManagedConnection c) {
        if(_connections.contains(c)) {
            //update the appropriate list of connections
            if(!c.isSupernodeClientConnection()){
                //REPLACE _initializedConnections with the list
                //_initializedConnections+[c]
                List newConnections=new ArrayList(_initializedConnections);
                newConnections.add(c);
                _initializedConnections=newConnections;
            }else{
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections+[c]
                List newConnections
                    =new ArrayList(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections=newConnections;
            }

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
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
		// Deactivate checking for Ultra Fast Shutdown
		deactivateUltraFastConnectShutdown(); 

        SettingsManager settings=SettingsManager.instance();
        int oldKeepAlive=settings.getKeepAlive();

        //1. Prevent any new threads from starting.  Note that this does not
        //   affect the permanent settings.  We have to use setKeepAliveNow
        //   to ignore the fact that we have a client-supernode connection.
        setKeepAlive(0);
        //2. Remove all connections.
        for (Iterator iter=getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            remove(c);
        }
    }
    
    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public synchronized void connect() {
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
                && _catcher.getNumHosts()==0;
        if (useHack) {
            settings.setUseQuickConnect(true);
            disconnect();
        }

        //Force reconnect to pong server.
        _catcher.expire();

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
			activateUltraFastConnectShutdown();
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
     * Sends the initial ping request to a newly initialized connection.  The ttl
     * of the PingRequest will be 1 if we don't need any connections.  Otherwise,
     * the ttl = max ttl.
     */
    private void sendInitialPingRequest(ManagedConnection connection) {
        PingRequest pr;
        //we need to compare how many connections we have to the keep alive to
        //determine whether to send a broadcast ping or a handshake ping, 
        //initially.  However, in this case, we can't check the number of 
        //connection fetchers currently operating, as that would always then
        //send a handshake ping, since we're always adjusting the connection 
        //fetchers to have the difference between keep alive and num of
        //connections.
        if (getNumInitializedConnections() >= _keepAlive)
            pr = new PingRequest((byte)1);
        else
            pr = new PingRequest(SettingsManager.instance().getTTL());
        connection.send(pr);
        //Ensure that the initial ping request is written in a timely fashion.
        try {
            connection.flush();
        } catch (IOException e) { /* close it later */ }
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
        	    setKeepAlive(outgoing);
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
        boolean removed = false;
        if(!c.isSupernodeClientConnection()){
            int i=_initializedConnections.indexOf(c);
            if (i != -1) {
                removed = true;
                //REPLACE _initializedConnections with the list
                //_initializedConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedConnections);
                newConnections.remove(c);
                _initializedConnections=newConnections;
            }
        }else{
            //check in _initializedClientConnections
            int i=_initializedClientConnections.indexOf(c);
            if (i != -1) {
                removed = true;
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedClientConnections);
                newConnections.remove(c);
                _initializedClientConnections=newConnections;
            }
        }
        
        //if connection was removed from any of the initialized lists of
        //connections, remove from the set of connected endpoints too.
        if(removed)
        {
            //REPLACE _endpoints with the set _endpoints+{c}
            Set newEndpoints=new HashSet();
            newEndpoints.addAll(_endpoints);
            newEndpoints.remove(new Endpoint(
                c.getInetAddress().getHostAddress(), c.getPort()));
            _endpoints=newEndpoints;           
        }
        
        // 1b) Remove from the all connections list and clean up the
        // stuff associated all connections
        int i=_connections.indexOf(c);
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
        //This is just an approximation.  The assumption is that all the
        //initializing connections are going to be unrouted connections.  Leaf
        //nodes, if any, will come to us.
        int nonLeafConnections=getNumConnections()
                                   - _initializedClientConnections.size();
        int need = _keepAlive - nonLeafConnections - _fetchers.size();

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
        finally{
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(c);
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
        {
            _callback.connectionInitialized(c);
            //check if we are a client node, and now opened a connection 
            //to supernode. In this case, we will drop all other connections
            //and just keep this one
            //check for shieldedclient-supernode connection
            if(c.isClientSupernodeConnection()) {
                gotShieldedClientSupernodeConnection(c);
            }
        }
    }

    /** 
     * Indicates that we are a client node, and have received supernode
     * connection. Executing this method will lead to dropping all the 
     * connections except the one to the supernode. 
     * @param supernodeConnection The connectionto the supernode that we
     * wanna preserve
     */
    private synchronized void gotShieldedClientSupernodeConnection(
        ManagedConnection supernodeConnection)
    {
        // Deactivate checking for Ultra Fast Shutdown
		deactivateUltraFastConnectShutdown(); 
        //Set keep alive to 1, so that we are not fetching any connections.
        //KEEP_ALIVE property is not modified, so that when this connection
        //drops, we can restore _keepAlive to its old value.  Note that we do
        //not set _keepAlive to 0.  This allows
        //lostShieldedClientSupernodeConnection to distinguish between being
        //disconnected by the user and being disconnected by the remote host.
        //(An earlier version required you to press the disconnect button twice
        //when in leaf mode.)  hasAvailableIncoming will not allow incoming
        //connections when we have a client/supernode connection, regardless of
        //_keepAlive.  The call to math.min prevents us from reconnecting if
        //disconnected.
        setKeepAlive(Math.min(1, _keepAlive));
        
        //close all other connections
        Iterator iterator = _connections.iterator();
        while(iterator.hasNext())
        {
            ManagedConnection connection = (ManagedConnection)iterator.next();
            if(!connection.equals(supernodeConnection))
                remove(connection);
        }
    }
    
    /** 
     * Indicates that the node is in the client node, and has now
     * lost its only connection to the supernode
     */
    private synchronized void lostShieldedClientSupernodeConnection()
    {
        //Return KEEP_ALIVE to old value...unless we're disconnected.
        //(Recall that the KEEP_ALIVE is set to *one* when getting 
        //a shielded leaf connection.)
        if(_connections.size() == 0 && _keepAlive>0)
        {
            setKeepAlive(SettingsManager.instance().getKeepAlive());
        }
    }
    
    /**
     * Processes the headers received during connection handshake and updates
     * itself with any useful information contained in those headers.
     * Also may change its state based upon the headers.
     * @param headers The headers to be processed
     * @param connection The connection on which we received the headers
     */
    private void processConnectionHeaders(ManagedConnection connection){
        //get the connection headers
        Properties headers = connection.getHeaders();
        //return if no headers to process
        if(headers == null) return;
        
        //update the addresses in the host cache (in case we received some
        //in the headers)
        updateHostCache(headers, connection);
                
        //get remote address
        String remoteAddress 
            = headers.getProperty(ConnectionHandshakeHeaders.X_MY_ADDRESS);
        //set the remote port if not outgoing connection (as for the outgoing
        //connection, we already know the port at which remote host listens)
        if((remoteAddress != null) && (!connection.isOutgoing()))
        {
            try
            {
                connection.setOrigPort(
                    Integer.parseInt(remoteAddress.substring(
                    remoteAddress.indexOf(':') + 1).trim()));
            }
            catch(Exception e){
                //no problem
                //should never happen though if the other client is well-coded
            }
        }
        
        //We used to check X_NEED_SUPERNODE here, but that is no longer
        //necessary.
    }
   
    /** 
     * Returns true if this can safely switch from supernode to leaf mode.
     * Typically that means there are no leaf connections, but it could be
     * stricter.  
     */
    public boolean allowClientMode() {
        //if is a supernode, and have client connections, 
        //or the supernode status is forced, dont change mode
        if (_settings.getForceSupernodeMode() 
            || (isSupernode() && _incomingClientConnections > 0))
            return false;
        else
            return true;
    }
    
    /**
     * Updates the addresses in the hostCache by parsing the passed string
     * @param headers The connection headers received
     * @param connection The connection on which we received the headers
     */
    private void updateHostCache(Properties headers, ManagedConnection
        connection){
        //get the supernodes, and add those to the host cache
        updateHostCache(headers.getProperty(
                ConnectionHandshakeHeaders.X_TRY_SUPERNODES),
                connection, true);
        //add the addresses received
        updateHostCache(headers.getProperty(
                ConnectionHandshakeHeaders.X_TRY),
                connection, false);        
    }
    
    /**
     * Updates the addresses in the hostCache by parsing the passed string
     * @param hostAddresses The string representing the addressess to be 
     * added. It should be in the form:
     * <p> IP Address:Port [,IPAddress:Port]* 
     * <p> e.g. 123.4.5.67:6346, 234.5.6.78:6347 
     * @param connection The connection on which we received the addresses
     * @param goodPriority Flag that specifies if the addresses have to be
     * given high priority
     */
    private void updateHostCache(String hostAddresses, 
                                 ManagedConnection connection, 
                                 boolean goodPriority){
        //check for null param
        if(hostAddresses == null)
            return;
         
        //tokenize to retrieve individual addresses
        StringTokenizer st = new StringTokenizer(hostAddresses,
            Constants.ENTRY_SEPARATOR);
        //iterate over the tokens
        while(st.hasMoreTokens()){
            //get an address
            String address = ((String)st.nextToken()).trim();
            Endpoint e;
            try{
                e = new Endpoint(address);
            }
            catch(IllegalArgumentException iae){
                continue;
            }
            //set the good priority, if specified
            //add it to the catcher
            _catcher.add(e, goodPriority);
        }
    }
    
    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.  If this is an incoming connection and there are no
     * slots available, rejects it and throws IOException.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c)
            throws IOException {
        //For outgoing connections add it to the GUI and the fetcher lists now.
        //For incoming, we'll do this below after checking incoming connection
        //slots.  This keeps reject connections from appearing in the GUI, as
        //well as improving performance slightly.
        if (c.isOutgoing()) {
            synchronized(this) {
                connectionInitializing(c);
                // We've added a connection, so the need for connections went down.
                adjustConnectionFetchers();
            }
            _callback.connectionInitializing(c);
        }
            
        try {
            c.initialize();
        } catch(IOException e) {
            remove(c);
            throw e;
        }
        finally{
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(c);
        }

        //If there's not space for the connection, reject it.  This mechanism
        //works for Gnutella 0.4 connections, as well as some odd cases for 0.6
        //connections.  Sometimes ManagedConnections are handled by headers
        //directly.
        if (!c.isOutgoing() && 
                !hasAvailableIncoming(c)) {
            c.loopToReject(_catcher);     
            //No need to remove, since it hasn't been added to any lists.
            throw new IOException("No space for connection");
        }

        //For incoming connections, add it to the GUI.  For outgoing connections
        //this was done at the top of the method.  See note there.
        if (! c.isOutgoing()) {
            synchronized(this) {
                connectionInitializingIncoming(c);
                // We've added a connection, so the need for connections went down.
                adjustConnectionFetchers();
            }
            _callback.connectionInitializing(c);
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
        {
            _callback.connectionInitialized(c);
            //check if we are a client node, and now opened a connection 
            //to supernode. In this case, we will drop all other connections
            //and just keep this one
            //check for shieldedclient-supernode connection
            if(c.isClientSupernodeConnection()) {
                gotShieldedClientSupernodeConnection(c);
            }
        }
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
				    PingRequest pingRequest = 
                        _router.createGroupPingRequest(group);
                    _connection.send(pingRequest);
                    //Ensure that the initial ping request is written in a timely fashion.
                    _connection.flush();
				}
				else
                {
                    //send normal ping request (handshake or broadcast depending
                    //on num of current connections.
                    sendInitialPingRequest(_connection);
                }
                _connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
            finally{
                if (_connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
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
     * @requires n>0
     * @effects returns an iterator that yields up the best n endpoints of this.
     *  It's not guaranteed that these are reachable. This can be modified while
     *  iterating through the result, but the modifications will not be
     *  observed.  
     */
    public synchronized Iterator getBestHosts(int n) {
        return _catcher.getBestHosts(n);
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
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
            finally{
                if (_connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
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
                endpoint.getHostname(), endpoint.getPort(), _router,
                ConnectionManager.this);

            try {
                initializeFetchedConnection(connection, this);
                sendInitialPingRequest(connection);
                connection.loopForMessages();
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
            finally{
                if (connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
            }
        }
    }
}
