package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import java.util.Properties;
import java.util.StringTokenizer;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.statistics.*;

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
 * connections is a slower operation.<p>
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
     * Please send an email to dev@core.limewire.org if and when you plan to 
     * change the behaviour of these methods.
     **********************************************************
     */
    
    /** Minimum number of connections that a supernode with leaf connections,
     * must have. */
    public static final int MIN_CONNECTIONS_FOR_SUPERNODE = 6;
    /** Ideal number of connections for a leaf.  */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF = 3;
    /** The desired number of slots to reserve for good connections (e.g.,
     *  LimeWire) unrouted connections. */
    public static final int RESERVED_GOOD_CONNECTIONS = 2;   
    /** Similar to RESERVED_GOOD_CONNECTIONS, but measures the number of slots
     *  allowed for bad leaf connections.  A value of zero means that only
     *  LimeWire's leaves are allowed.  */
    public static final int ALLOWED_BAD_LEAF_CONNECTIONS = 4;
    /** The maximum number of ultrapeer endpoints to give out from the host
     *  catcher in X_TRY_SUPERNODES headers. */
    private int MAX_SUPERNODE_ENDPOINTS=10;

    
    /* Sister backend classes. */
    private MessageRouter _router;
    private HostCatcher _catcher;
	private SettingsManager _settings;
	private ConnectionWatchdog _watchdog;


    /** The number of connections to keep up.  */
    private volatile int _keepAlive=0;
    /** Threads trying to maintain the KEEP_ALIVE.  This is generally
     *  some multiple of _keepAlive.   LOCKING: obtain this. */
    private List /* of ConnectionFetcher */ _fetchers =
        new ArrayList();
    /** Connections that have been fetched but not initialized.  I don't
     *  know the relation between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtain this. */
    private List /* of ManagedConnection */ _initializingFetchedConnections =
        new ArrayList();


    /* List of all connections.  The core data structures are lists, which allow
     * fast iteration for message broadcast purposes.  Actually we keep a couple
     * of lists: the list of all initialized and uninitialized connections
     * (_connections), the list of all initialized non-leaf connections
     * (_incomingConnections), and the list of all initialized leaf connections
     * (_incomingClientConnections).
     * 
     * INVARIANT: neither _connections, _initializedConnections, nor 
     *   _initializedClientConnections contains any duplicates.
     * INVARIANT: for all c in _initializedConnections, 
     *   c.isSupernodeClientConnection()==false
     * INVARIANT: for all c in _initializedClientConnections, 
     *   c.isSupernodeClientConnection()==true
     * COROLLARY: the intersection of _initializedClientConnections 
     *   and _initializedConnections is the empty set
     * INVARIANT: _initializedConnections is a subset of _connections
     * INVARIANT: _initializedClientConnections is a subset of _connections
     *
     * LOCKING: _connections, _initializedConnections and
     *   _initializedClientConnections MUST NOT BE MUTATED.  Instead they should
     *   be replaced as necessary with new copies.  Before replacing the
     *   structures, obtain this' monitor.  This avoids lock overhead when
     *   message broadcasting, though it makes adding/removing connections
     *   much slower.  
     */
    private volatile List /* of ManagedConnection */ 
        _connections = new ArrayList();
    private volatile List /* of ManagedConnection */ 
        _initializedConnections = new ArrayList();
    private volatile List /* of ManagedConnection */ 
        _initializedClientConnections = new ArrayList();

    /**
     * For authenticating users
     */
    private Authenticator _authenticator;

    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     * @param authenticator Authenticator instance for authenticating users
     */
    public ConnectionManager(Authenticator authenticator) {
        _authenticator = authenticator; 
		_settings = SettingsManager.instance(); 
    }

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initialize() {
        _router = RouterService.getMessageRouter();//router;
        _catcher = RouterService.getHostCatcher();//catcher;

        // Start a thread to police connections.
        // Perhaps this should use a low priority?
        _watchdog = new ConnectionWatchdog(this, _router);
        Thread watchdog=new Thread(_watchdog);
        watchdog.setDaemon(true);
  		watchdog.start();
        
        //We used to set the keep-alive to zero here, but that caused problems
        //because connection fetchers could wait in HostCatcher.getAnEndpoint()
        //before HostCatcher.expire() had been called.  As a result, LimeWire
        //sometimes failed to connect on startup, esp. if the gnutella.net file
        //was empty.  Turns out this code isn't needed, as
        //RouterService.initialize() does the right thing.
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
             //keep handling the messages on the connection

             //if a leaf connected to us, ensure that we have enough non-leaf
             //connections opened
             if(connection.isSupernodeClientConnection())
                ensureConnectionsForSupernode();
             
			 startConnection(connection);
			 
         } catch(IOException e) {
         } catch(Exception e) {
             //Internal error!
             RouterService.error(ActivityCallback.INTERNAL_ERROR, e);
         } finally {
            //if we were leaf to a supernode, reconnect to network 
            if (connection.isClientSupernodeConnection())
                lostShieldedClientSupernodeConnection();
         }
     }

    /** 
     * Used by killExcess.  Sorts by outgoing/incoming, then by number of
     * messages sent. 
     */
    private static class ManagedConnectionComparator implements Comparator {
        public int compare(Object connection1, Object connection2) {
            ManagedConnection mc1=(ManagedConnection)connection1;
            ManagedConnection mc2=(ManagedConnection)connection2;
            if (mc1.isOutgoing()!=mc2.isOutgoing()) {
                //Primary key: outgoing status
                return mc1.isOutgoing() ? -1 : 1;
            } else {
                //Secondary key: number of messages
                int total1=mc1.getNumMessagesSent()
                          +mc1.getNumMessagesReceived();
                int total2=mc2.getNumMessagesSent()
                          +mc2.getNumMessagesReceived();
                return total1-total2;
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
     * to add more threads.  Ignores request if a shielded leaf node
     * and newKeep>1 (sic).
     */
    public synchronized void setKeepAlive(int newKeep) {
        //TODO: we may want to re-enable this...with a higher limit.
        ////The request for increasing keep alive if we are leaf node is invalid
        ////This logic is duplicated in RouterService.setKeepAlive.
        //if ((newKeep > 1) && hasClientSupernodeConnection())
        //    return;
        
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
        if(getNumInitializedClientConnections() > 0 
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
    public boolean hasClientSupernodeConnection() {
        //TODO2: should we make this faster by augmenting state?  We could
        //also return false if isSupernode().
        List connections=getInitializedConnections();
        for (int i=0; i<connections.size(); i++) {
            ManagedConnection first=(ManagedConnection)connections.get(i);
            if (first.isClientSupernodeConnection())
                return true;
        }
        return false;
    }
    
    /**
     * Returns true if this is a super node with a connection to a leaf. 
     */
    public boolean hasSupernodeClientConnection() {
        return getNumInitializedClientConnections() > 0;
    }
    
    /**
     * Returns whether this (probably) has a connection to the given host.  This
     * method is currently implemented by iterating through all connections and
     * comparing addresses but not ports.  (Incoming connections use ephemeral
     * ports.)  As a result, this test may conservatively return true even if
     * this is not connected to <tt>host</tt>.  Likewise, it may it mistakenly
     * return false if <tt>host</tt> is a multihomed system.  In the future,
     * additional connection headers may make the test more precise.
     *
     * @return true if this is probably connected to <tt>host</tt> 
     */
    public boolean isConnected(Endpoint host) {        
        String hostName=host.getHostname();
        //A clone of the list of all connections, both initialized and
        //uninitialized, leaves and unrouted.  If Java could be prevented from
        //making certain code transformations, it would be safe to replace the
        //call to "getConnections()" with "_connections", thus avoiding a clone.
        //(Remember that _connections is never mutated.)
        List connections=getConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc=(ManagedConnection)iter.next();
            try {
                if (mc.getInetAddress().getHostAddress().equals(hostName))
                    return true;
            } catch (IllegalStateException e) {
                //Connection still initializing...ignore.
            }
        }
        return false;
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
     * @return the number of initializedclient connections, which is less than
     * or equals to the number of connections.  
     */
    private int getNumInitializedClientConnections() {
		return _initializedClientConnections.size();
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
     * @param c The connection we received, for which to 
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    public boolean allowConnection(ManagedConnection c) {
        return allowConnection(
            c.isOutgoing(),
            c.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE),
            c.getProperty(ConnectionHandshakeHeaders.USER_AGENT));
    }
    
    /**
     * Checks if there is any available slot of any kind.
     * @return true, if we have incoming slot of some kind,
     * false otherwise
     */
    public boolean allowAnyConnection() {
        int shieldedMax=
            SettingsManager.instance().getMaxShieldedClientConnections();

        //Stricter than necessary.  
        //See allowAnyConnection(boolean,String,String).
        if (hasClientSupernodeConnection())
            return false;

        //Do we have normal or leaf slots?
        return getNumInitializedConnections() < _keepAlive
            || (isSupernode() 
                    && getNumInitializedClientConnections() < shieldedMax);
    }
    
    /**
     * Returns true if this has slots for an incoming connection, <b>without
     * accounting for this' ultrapeer capabilities</b>.  More specifically:
     * <ul>
     * <li>if ultrapeerHeader==null, returns true if this has space for an 
     *  unrouted old-style connection.
     * <li>if ultrapeerHeader.equals("true"), returns true if this has slots
     *  for a leaf connection.
     * <li>if ultrapeerHeader.equals("false"), returns true if this has slots 
     *  for an ultrapeer connection.
     * </ul>
     *
     * <tt>useragentHeader</tt> is used to prefer LimeWire and certain trusted
     * vendors.  <tt>outgoing</tt> is currently unused, but may be used to 
     * prefer incoming or outgoing connections in the forward.
     *
     * @param outgoing true if this is an outgoing connection; true if incoming
     * @param ultrapeerHeader the value of the X-Ultrapeer header, or null
     *  if it was not written
     * @param useragentHeader the value of the User-Agent header, or null if
     *  it was not written
     * @return true if a connection of the given type is allowed
     */
    public synchronized boolean allowConnection(boolean outgoing,
                                                String ultrapeerHeader,
                                                String useragentHeader) {
        //Old versions of LimeWire used to prefer incoming connections over
        //outgoing.  The rationale was that a large number of hosts were
        //firewalled, so those who weren't had to make extra space for them.
        //With the introduction of ultrapeers, this is not an issue; all
        //firewalled hosts become leaf nodes.  Hence we make no distinction
        //between incoming and outgoing.
        //
        //At one point we would actively kill old-fashioned unrouted connections
        //for ultrapeers.  Later, we preferred ultrapeers to old-fashioned
        //connections as follows: if the HostCatcher had marked ultrapeer pongs,
        //we never allowed more than DESIRED_OLD_CONNECTIONS old
        //connections--incoming or outgoing.
        //
        //Now we simply prefer connections by vendor, which has some of the same
        //effect.  We use BearShare's clumping algorithm.  Let N be the
        //keep-alive and K be RESERVED_GOOD_CONNECTIONS.  (In BearShare's
        //implementation, K=1.)  Allow any connections in for the first N-K
        //slots.  But only allow good vendors for the last K slots.  In other
        //words, accept a connection C if there are fewer than N connections and
        //one of the following is true: C is a good vendor or there are fewer
        //than N-K connections.  With time, this converges on all good
        //connections.
        
        SettingsManager settings=SettingsManager.instance();
        boolean isUltrapeerAware=ultrapeerHeader!=null;
        boolean isLeaf=ConnectionHandshakeHeaders.isFalse(ultrapeerHeader);

        //Don't allow anything if disconnected or shielded leaf.  This rule is
        //critical to the working of gotShieldedClientSupernodeConnection.
        if (_keepAlive<=0)
            return false;
        else if (hasClientSupernodeConnection())
            //TODO3: not necessarily true since 2.1, but we want to fetch ultrapeers
            return false;  

        else if (isLeaf && isUltrapeerAware) {
            //1. Leaf. As the spec. says, this assumes we are an ultrapeer.
            //Preference trusted vendors using BearShare's clumping algorithm
            //(see above).
            int shieldedMax=
                SettingsManager.instance().getMaxShieldedClientConnections();
            return getNumInitializedClientConnections() 
                < (trustedVendor(useragentHeader)
                      ? shieldedMax : ALLOWED_BAD_LEAF_CONNECTIONS);
        } else {
            //2. Ultrapeer or 0.6.  Preference trusted vendors using BearShare's
            //clumping algorithm (see above).
            return getNumInitializedConnections()
                 < (trustedVendor(useragentHeader)
                       ? _keepAlive : _keepAlive-RESERVED_GOOD_CONNECTIONS);
        }
    }

    private static boolean trustedVendor(String useragentHeader) {
        if (useragentHeader==null)
            return false;
        return useragentHeader.startsWith("LimeWire") 
            || useragentHeader.startsWith("Swapper");
    }
        

    /** Returns the number of connections to other ultrapeers.  Caller MUST hold
     *  this' monitor. */
    private int ultrapeerConnections() {
        //TODO3: augment state of this if needed to avoid loop
        int ret=0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            ManagedConnection mc=(ManagedConnection)iter.next();
            if (mc.isSupernodeConnection())
                ret++;
        }
        return ret;
    }

    /** Returns the number of old-fashioned unrouted connections.  Caller MUST
     *  hold this' monitor. */
    private int oldConnections() {
        //TODO3: augment state of this if needed to avoid loop
        int ret=0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            ManagedConnection mc=(ManagedConnection)iter.next();
            if (mc.getProperty(ConnectionHandshakeHeaders.X_SUPERNODE)==null)
                ret++;            
        }
        return ret;
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
            getNumInitializedClientConnections() > 
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
        //add the best few endpoints from the hostcatcher.
        Iterator iterator=_catcher.getUltrapeerHosts(MAX_SUPERNODE_ENDPOINTS);
        while (iterator.hasNext()) {
            Endpoint e=(Endpoint)iterator.next();
            retSet.add(e);
        }
        return retSet;
    }

	/**
	 * Returns the <tt>Endpoint</tt> for an Ultrapeer connected via TCP, 
	 * if available.
	 *
	 * @return the <tt>Endpoint</tt> for an Ultrapeer connected via TCP if
	 *  there is one, otherwise returns <tt>null</tt>
	 */
	public Endpoint getConnectedGUESSUltrapeer() {
		for(Iterator iter=_initializedConnections.iterator(); iter.hasNext();) {
			ManagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() && 
			   connection.isGUESSUltrapeer()) {				
				return new Endpoint(connection.getInetAddress().getAddress(),
									connection.getOrigPort());
			}
		}
		return null;
	}
    

    /** Returns a <tt>List<tt> of Ultrapeers connected via TCP that are GUESS
     *  enabled.
     *
     * @return A non-null List of GUESS enabled, TCP connected Ultrapeers.  The
     * are represented as ManagedConnections.
     */
	public List getConnectedGUESSUltrapeers() {
        List retList = new ArrayList();
		for(Iterator iter=_initializedConnections.iterator(); iter.hasNext();) {
			ManagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() && 
               connection.isGUESSUltrapeer()) 
				retList.add(connection);
		}
		return retList;
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
        }
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
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
            //add the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                _catcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                    c.getPort()), true);
            }   
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

        //Tell the HostCatcher it's ok to reconnect to router.limewire.com.
        _catcher.expire();

        //Ensure outgoing connections is positive.
        int outgoing=settings.getKeepAlive();
        if (outgoing<1) {
            outgoing = settings.DEFAULT_KEEP_ALIVE;
            settings.setKeepAlive(outgoing);
        }
        //Actually notify the backend.
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
     * Sends the initial ping request to a newly initialized connection.  If
     * connection is router.limewire.com (e.g.,
     * connection.isRouterConnection()), sends a special group ping.  Otherwise
     * the ttl of the PingRequest will be 1 if we don't need any connections.
     * Otherwise, the ttl = max ttl.
     */
    private void sendInitialPingRequest(ManagedConnection connection) {
        PingRequest pr;
        //Bootstrap server: send group ping.
        if (connection.isRouterConnection()) {
            String group = "none:"+_settings.getConnectionSpeed();
            pr = _router.createGroupPingRequest(group);                 //a
        }
        //We need to compare how many connections we have to the keep alive to
        //determine whether to send a broadcast ping or a handshake ping, 
        //initially.  However, in this case, we can't check the number of 
        //connection fetchers currently operating, as that would always then
        //send a handshake ping, since we're always adjusting the connection 
        //fetchers to have the difference between keep alive and num of
        //connections.
        else if (getNumInitializedConnections() >= _keepAlive)
            pr = new PingRequest((byte)1);                              //b
        else
            pr = new PingRequest(SettingsManager.instance().getTTL());  //c

        connection.send(pr);
        //Ensure that the initial ping request is written in a timely fashion.
        try {
            connection.flush();
        } catch (IOException e) { /* close it later */ }
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
        RouterService.getCallback().connectionClosed(c); 

        // 5) Clean up Unicaster
        QueryUnicaster.instance().purgeQuery(c);
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
        //How many connections do we need?  To prefer ultrapeers, we try to
        //achieve KEEP_ALIVE ultrapeer connections.  But to prevent
        //fragmentation with clients that don't support ultrapeers, we'll give
        //the first DESIRED_OLD_CONNECTIONS ultrapeers protected status.  See
        //allowConnection(boolean, boolean) and killExcessConnections().
        int goodConnections=getNumInitializedConnections();
        int neededConnections=_keepAlive - goodConnections;
        //Now how many fetchers do we need?  To increase parallelism, we
        //allocate 4 fetchers per connection, but no more than 10 fetchers.
        //(Too much parallelism increases chance of simultaneous connects,
        //resulting in too many connections.)  Note that we assume that all
        //connections being fetched right now will become ultrapeers.
        int need = Math.min(10,4*neededConnections) 
                 - _fetchers.size()
                 - _initializingFetchedConnections.size();

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
     * Throws any of the exceptions listed in Connection.initialize on
     * failure; no cleanup is necessary in this case.
     *
     * @exception IOException we were unable to establish a TCP connection
     *  to the host
     * @exception NoGnutellaOkException we were able to establish a 
     *  messaging connection but were rejected
     * @exception BadHandshakeException some other problem establishing 
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during handshaking, etc. 
     * @see com.limegroup.gnutella.Connection#initialize(int)
     */
    private void initializeFetchedConnection(ManagedConnection c,
                                             ConnectionFetcher fetcher)
            throws NoGnutellaOkException, BadHandshakeException, IOException {
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
		ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
        RouterService.getCallback().connectionInitializing(c);

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
            RouterService.getCallback().connectionInitialized(c);
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
     * connection.  This may choose to adjust its keep-alive. 
     * @param supernodeConnection the newly initialized leaf-ultrapeer
     *  connection
     */
    private synchronized void gotShieldedClientSupernodeConnection(
        ManagedConnection supernodeConnection)
    {
        //How many leaf connections should we have?  There's a tension between
        //doing what LimeWire thinks is best and what the user wants.  Ideally
        //we would set the KEEP_ALIVE iff the user hasn't recently manually
        //adjusted it.  Because this is a pain to implement, we use a hack; only
        //adjust the KEEP_ALIVE if there is only one shielded leaf-ultrapeer
        //connection.  Typically this will happen just once, when we enter leaf
        //mode.  Note that we actually call ultrapeerConnections() instead of a
        //"clientSupernodeConnections()" method; if this method is called and
        //ultrapeerConnections()==1, there is exactly one client-supernode
        //connection.
        boolean firstShieldedConnection=(ultrapeerConnections()==1) 
                                       && _keepAlive>0;
        if (firstShieldedConnection)
            setKeepAlive(PREFERRED_CONNECTIONS_FOR_LEAF);    
    }
    
    /** 
     * Indicates that the node is in client mode and has lost a leaf
     * to ultrapeer connection.
     */
    private synchronized void lostShieldedClientSupernodeConnection()
    {
        //Does nothing!  adjustConnectionFetchers takes care of everything now.
        //I'm leaving this method here as a nice place holder in case we
        //need to take action in the future.
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
                
        //get remote address.  If the more modern "Listen-IP" header is
        //not included, try the old-fashioned "X-My-Address".
        String remoteAddress 
            = headers.getProperty(ConnectionHandshakeHeaders.LISTEN_IP);
        if (remoteAddress==null)
            remoteAddress 
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
        //if is a supernode, and have other connections (client or ultrapeer),
        //or the supernode status is forced, dont change mode
        int connections=getNumInitializedConnections()
                       +getNumInitializedClientConnections();
        if (_settings.getForceSupernodeMode() 
            || (isSupernode() && connections > 0))
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
			ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
            RouterService.getCallback().connectionInitializing(c);
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
                !allowConnection(c)) {
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
			ConnectionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
            RouterService.getCallback().connectionInitializing(c);
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
            RouterService.getCallback().connectionInitialized(c);
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

				startConnection(_connection);
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                RouterService.error(ActivityCallback.INTERNAL_ERROR, e);
            }
            finally{
                if (_connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
            }
        }
    }

	/**
	 * Runs standard calls that should be made whenever a connection if fully
	 * established and should wait for messages.
	 *
	 * @param conn the <tt>ManagedConnection</tt> instance to start
	 * @throws <tt>IOException</tt> if there is an excpetion while looping
	 *  for messages
	 */
	private void startConnection(ManagedConnection conn) throws IOException {	
		// Send ping...possibly group ping.
		sendInitialPingRequest(conn);

		if(conn.isGUESSUltrapeer()) {
			int port = conn.getOrigPort();
			QueryUnicaster.instance().addUnicastEndpoint(conn.getInetAddress(),
														 port);
		}

		// this can throw IOException
		conn.loopForMessages();		
	}

    //------------------------------------------------------------------------
//     /**
//      * Create a new connection, blocking until it's initialized, but launching
//      * a new thread to do the message loop.
//      */
//     public ManagedConnection createGroupConnectionBlocking(
//       String hostname, int portnum, GroupPingRequest specialPing) 
// 	  throws IOException {
//         ManagedConnection c = 
// 		  new ManagedConnection(hostname, portnum, _router, this, true);

//         // Initialize synchronously
//         initializeExternallyGeneratedConnection(c);
//         // Kick off a thread for the message loop.
//         new GroupOutgoingConnectionThread(c, specialPing);

//         return c;
//     }
    
    /**
     * @requires n>0
     * @effects returns an iterator that yields up the best n non-ultrapeer
     *  endpoints of this.  It's not guaranteed that these are reachable. This
     *  can be modified while iterating through the result, but the modifications
     *  will not be observed.  
     */
    public synchronized Iterator getNormalHosts(int n) {
        //TODO: this method doesn't really belong here.  It's used because parts
        //of ManagedConnection have a reference to this but not the HostCatcher.
        return _catcher.getNormalHosts(n);
    }
    

    /**
     * This thread does the message loop for ManagedConnections created
     * through createGroupConnectionBlocking
     */
    private class GroupOutgoingConnectionThread extends Thread {
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
                RouterService.error(ActivityCallback.INTERNAL_ERROR, e);
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
                //Try to connect, recording success or failure so HostCatcher
                //can update connection history.  Note that we declare 
                //success if we were able to establish the TCP connection
                //but couldn't handshake (NoGnutellaOkException).
                try {
                    initializeFetchedConnection(connection, this);
                    _catcher.doneWithConnect(endpoint, true);
                } catch (NoGnutellaOkException e) {
                    _catcher.doneWithConnect(endpoint, true);
                    throw e;                    
                } catch (IOException e) {
                    _catcher.doneWithConnect(endpoint, false);
                    throw e;
                }

				startConnection(connection);
            } catch(IOException e) {
            } catch(Exception e) {
                //Internal error!
                RouterService.error(ActivityCallback.INTERNAL_ERROR, e);
            }
            finally{
                //Record that we're done with the connection, which may allow
                //HostCatcher to go on to other endpoints.
                _catcher.doneWithMessageLoop(endpoint);
                if (connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
            }
        }
    }

    /*
    public static void main(String[] args) {
        TestManagedConnection i13=new TestManagedConnection(false, 1, 3);
        TestManagedConnection o13=new TestManagedConnection(true, 1, 3);
        TestManagedConnection i10=new TestManagedConnection(false, 1, 0);
        TestManagedConnection o10=new TestManagedConnection(true, 1, 0);
        
        List l=new ArrayList(); l.add(i13); l.add(o13); l.add(i10); l.add(o10);
        Collections.sort(l, new ManagedConnectionComparator());
        Assert.that(l.get(0)==o10);
        Assert.that(l.get(1)==o13);
        Assert.that(l.get(2)==i10);
        Assert.that(l.get(3)==i13);
    }

    private static class TestManagedConnection extends ManagedConnection {
        private boolean isOutgoing;
        private int sent;
        private int received;

        public TestManagedConnection(boolean isOutgoing, int sent, int received) {
            super();  //may require adding stub constructor to ManagedConnection
            this.isOutgoing=isOutgoing;
            this.sent=sent;
            this.received=received;
        }

        public boolean isOutgoing() {
            return isOutgoing;
        }

        public int getNumMessagesSent() {
            return sent;
        }
        
        public int getNumMessagesReceived() {
            return received;
        }        
    }
    */
}
