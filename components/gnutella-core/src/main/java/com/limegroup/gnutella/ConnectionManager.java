package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import java.util.Properties;
import java.util.StringTokenizer;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * The list of all Connection's.  Provides a factory method for creating
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

	/**
	 * The number of Ultrapeer connections to ideally maintain as an Ultrapeer.
	 */
	public static final int ULTRAPEER_CONNECTIONS =
        ConnectionSettings.NUM_CONNECTIONS.getValue();

    /** 
     * The number of connections leaves should maintain to Ultrapeers.
     */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF = 4;

	/**
	 * The number of leaf connections reserved for "good" clients.  As
	 * described above, the definition of good constantly changes with 
	 * advances in search architecture.
	 */
    public static final int RESERVED_GOOD_LEAF_CONNECTIONS = 
        UltrapeerSettings.MAX_LEAVES.getValue() - 15;  
 
    /** Similar to RESERVED_GOOD_CONNECTIONS, but measures the number of slots
     *  allowed for bad leaf connections.  A value of zero means that only
     *  LimeWire's leaves are allowed.  */
    public static final int ALLOWED_BAD_LEAF_CONNECTIONS = 2;

    /** The maximum number of ultrapeer endpoints to give out from the host
     *  catcher in X_TRY_ULTRAPEER headers. */
    private static final int MAX_ULTRAPEER_ENDPOINTS=10;
	
	/** The maximum number of leaves to allow -- also the preferred number.*/		
	private static final int MAX_LEAVES = 
		UltrapeerSettings.MAX_LEAVES.getValue();

    /**
     * Reference to the <tt>HostCatcher</tt> for retrieving host data as well 
     * as adding host data.
     */
    private HostCatcher _catcher;

    /** The number of connections to keep up.  */
    private volatile int _keepAlive=0;
    /** Threads trying to maintain the NUM_CONNECTIONS.  This is generally
     *  some multiple of _keepAlive.   LOCKING: obtain this. */
    private final List /* of ConnectionFetcher */ _fetchers =
        new ArrayList();
    /** Connections that have been fetched but not initialized.  I don't
     *  know the relation between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtain this. */
    private final List /* of Connection */ _initializingFetchedConnections =
        new ArrayList();


    /**
     * List of all connections.  The core data structures are lists, which allow
     * fast iteration for message broadcast purposes.  Actually we keep a couple
     * of lists: the list of all initialized and uninitialized connections
     * (_connections), the list of all initialized non-leaf connections
     * (_initializedConnections), and the list of all initialized leaf connections
     * (_initializedClientConnections).
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
     * INVARIANT: _shieldedConnections is the number of connections
     *   in _initializedConnections for which isClientSupernodeConnection()
     *   is true.
     *
     * LOCKING: _connections, _initializedConnections and
     *   _initializedClientConnections MUST NOT BE MUTATED.  Instead they should
     *   be replaced as necessary with new copies.  Before replacing the
     *   structures, obtain this' monitor.  This avoids lock overhead when
     *   message broadcasting, though it makes adding/removing connections
     *   much slower.  
     */
    //TODO:: why not use sets here??
    private volatile List /* of Connection */ 
        _connections = new ArrayList();
    private volatile List /* of Connection */ 
        _initializedConnections = new ArrayList();
    private volatile List /* of Connection */ 
        _initializedClientConnections = new ArrayList();
        
    private volatile int _shieldedConnections = 0;

    /**
     * For authenticating users
     */
    private final Authenticator _authenticator;

	/**
	 * Variable for the number of times since we attempted to force ourselves 
	 * to become an Ultrapeer that we were told to become leaves.  If this number
	 * is too great, we give up and become a leaf.
	 */
	private volatile int _leafTries;

	/**
	 * The number of demotions to ignore before allowing ourselves to become 
	 * a leaf -- this number depends on how good this potential Ultrapeer seems 
	 * to be.
	 */	
	private volatile int _demotionLimit = 0;

    /**
     * Constructs a ConnectionManager.  Must call initialize before using.
     * @param authenticator Authenticator instance for authenticating users
     */
    public ConnectionManager(Authenticator authenticator) {
        _authenticator = authenticator; 
    }

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initialize() {
        _catcher = RouterService.getHostCatcher();
    }


    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public Connection createConnectionBlocking(String hostname, int portnum) 
		throws IOException {
        Connection c = new Connection(hostname, portnum);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        Thread conn =
            new Thread(new OutgoingConnector(c, false), "OutgoingConnector");
        conn.setDaemon(true);
        conn.start();
        return c;
    }

    /**
     * Create a new connection, allowing it to initialize and loop for messages
     * on a new thread.
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {

		Runnable outgoingRunner = 
			new OutgoingConnector(new Connection(hostname, portnum), true);
        // Initialize and loop for messages on another thread.

		Thread outgoingConnectionRunner = 
			new Thread(outgoingRunner, "OutgoingConnectionThread");
		outgoingConnectionRunner.setDaemon(true);
		outgoingConnectionRunner.start();
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
         Thread.currentThread().setName("IncommingConnectionThread");
         Connection connection = new Connection(socket);
         try {
             initializeExternallyGeneratedConnection(connection);
         } catch (IOException e) {
			 connection.close();
             return;
         }
             
         try {   
			 startConnection(connection);			 
         } catch(IOException e) {
             // we could not start the connection for some reason --
             // this can easily happen, for example, if the connection
             // just drops
         } catch(Throwable e) {
             //Internal error!
             ErrorService.error(e);
         } finally {
            //if we were leaf to a ultrapeer, reconnect to network 
            if (connection.isClientSupernodeConnection())
                lostShieldedClientSupernodeConnection();
         }
     }

     
    /**
     * Removes the specified connection from currently active connections, also
     * removing this connection from routing tables and modifying active 
     * connection fetchers accordingly.
     *
     * @param mc the <tt>Connection</tt> instance to remove
     */
    public synchronized void remove(Connection conn) {
		// removal may be disabled for tests
		if(!ConnectionSettings.REMOVE_ENABLED.getValue()) return;        
        removeInternal(conn);

        adjustConnectionFetchers();
    }

    /**
     * Get the number of connections we are attempting to maintain.
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
        _keepAlive = newKeep;
        adjustConnectionFetchers();
    }
    
    /**
     * Tells whether the node is gonna be an Ultrapeer or not
     * @return true, if Ultrapeer, false otherwise
     */
    public boolean isSupernode() {
        return 
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue() && 
            !isShieldedLeaf();
    }
    
    /**
     * Returns true if this is a leaf node with a connection to a ultrapeer.  It
     * is not required that the ultrapeer support query routing, though that is
     * generally the case.  
     */
    public boolean isShieldedLeaf() {
        return _shieldedConnections != 0;
//        List connections=getInitializedConnections();
//        for (int i=0; i<connections.size(); i++) {
//            Connection first=(Connection)connections.get(i);
//            if (first.isClientSupernodeConnection())
//                return true;
//        }
//        return false;
    }
    
    /**
     * Returns true if this is a super node with a connection to a leaf. 
     */
    public boolean hasSupernodeClientConnection() {
        return getNumInitializedClientConnections() > 0;
    }

    /**
     * Returns whether or not this node has any available connection
     * slots.  This is only relevant for Ultrapeers -- leaves will 
     * always return <tt>false</tt> to this call since they do not 
     * accept any incoming connections, at least for now.
     *
     * @return <tt>true</tt> if this node is an Ultrapeer with free
     *  leaf or Ultrapeer connections slots, otherwise <tt>false</tt>
     */
    public boolean hasFreeSlots() {
        return isSupernode() && 
            (hasFreeUltrapeerSlots() || hasFreeLeafSlots());
    }

    /**
     * Utility method for determing whether or not we have any available
     * Ultrapeer connection slots.  If this node is a leaf, it will
     * always return <tt>false</tt>.
     *
     * @return <tt>true</tt> if there are available Ultrapeer connection
     *  slots, otherwise <tt>false</tt>
     */
    private boolean hasFreeUltrapeerSlots() {
        return getNumFreeNonLeafSlots() > 0;
    }

    /**
     * Utility method for determing whether or not we have any available
     * leaf connection slots.  If this node is a leaf, it will
     * always return <tt>false</tt>.
     *
     * @return <tt>true</tt> if there are available leaf connection
     *  slots, otherwise <tt>false</tt>
     */
    private boolean hasFreeLeafSlots() {
        return getNumFreeLeafSlots() > 0;
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
    private boolean isConnectedTo(Endpoint host) {        
        String hostName=host.getHostname();
        //A clone of the list of all connections, both initialized and
        //uninitialized, leaves and unrouted.  If Java could be prevented from
        //making certain code transformations, it would be safe to replace the
        //call to "getConnections()" with "_connections", thus avoiding a clone.
        //(Remember that _connections is never mutated.)
        List connections=getConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            Connection mc = (Connection)iter.next();
 
            if (mc.getIPString().equals(hostName))
                return true;
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
    public int getNumInitializedConnections() {
		return _initializedConnections.size();
    }
    
    /**
     * @return the number of initializedclient connections, which is less than
     * or equals to the number of connections.  
     */
    public int getNumInitializedClientConnections() {
		return _initializedClientConnections.size();
    }

    /**
     *@return the number of initialized connections for which
     * isClientSupernodeConnection is true.
     */
    public int getNumClientSupernodeConnections() {
        return _shieldedConnections;
    }
    
    /**
     *@return the number of ultrapeer -> ultrapeer connections.
     */
    public synchronized int getNumUltrapeerConnections() {
        return ultrapeerToUltrapeerConnections();
    }
    
    /**
     *@return the number of old unrouted connections.
     */
    public synchronized int getNumOldConnections() {
        return oldConnections();
    }
    
    /**
     * @return the number of free leaf slots.
     */
    int getNumFreeLeafSlots() {
        if (isSupernode())
			return MAX_LEAVES - 
				getNumInitializedClientConnections();
        else
            return 0;
    }

    
    /**
     * @return the number of free non-leaf slots.
     */
    int getNumFreeNonLeafSlots() {
        return ULTRAPEER_CONNECTIONS - getNumInitializedConnections();
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
            Connection conn = (Connection)iter.next();
            conn.stats().measureBandwidth();
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
            Connection conn = (Connection)iter.next();
            sum += conn.stats().getMeasuredUpstreamBandwidth();
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
            Connection conn = (Connection)iter.next();
            sum += conn.stats().getMeasuredDownstreamBandwidth();
        }
        return sum;
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer, 
     * temporary etc). 
     * @param c The connection we received, for which to 
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    private boolean allowConnection(Connection c) {
        if(!c.receivedHeaders()) return false;
		return allowConnection(c.headers(), false);
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer, 
     * temporary etc). 
     * @param c The connection we received, for which to 
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    public boolean allowConnectionAsLeaf(HandshakeResponse hr) {
		return allowConnection(hr, true);
    }

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer, 
     * temporary etc). 
     * @param c The connection we received, for which to 
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
     public boolean allowConnection(HandshakeResponse hr) {
         return allowConnection(hr, false);
     }

    
    /**
     * Checks if there is any available slot of any kind.
     * @return true, if we have incoming slot of some kind,
     * false otherwise
     */
    public boolean allowAnyConnection() {
        //Stricter than necessary.  
        //See allowAnyConnection(boolean,String,String).
        if (isShieldedLeaf())
            return false;

        //Do we have normal or leaf slots?
        return getNumInitializedConnections() < _keepAlive
            || (isSupernode() 
				&& getNumInitializedClientConnections() < MAX_LEAVES);
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
    public boolean allowConnection(HandshakeResponse hr, boolean leaf) {

		// preferencing may not be active for testing purposes --
		// just return if it's not
		if(!ConnectionSettings.PREFERENCING_ACTIVE.getValue()) return true;
		

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

        //Don't allow anything if disconnected or shielded leaf.  This rule is
        //critical to the working of gotShieldedClientSupernodeConnection.
        if (!ConnectionSettings.IGNORE_KEEP_ALIVE.getValue() && _keepAlive <= 0) {
            return false;
		} else if (RouterService.isShieldedLeaf()) {
			// we're a leaf -- don't allow any incoming connections
            return false;  
		} else if (hr.isLeaf() || leaf) {
            // Leaf. As the spec. says, this assumes we are an ultrapeer.
            // If the leaf supports features we're looking for, accept it
            // if we have slots.  Otherwise, only accept it if it's a 
            // trusted vendor and we have slots availble for older clients
			if(hr.isGoodLeaf()) {
                return getNumInitializedClientConnections() <
                    UltrapeerSettings.MAX_LEAVES.getValue();
			} else {
				return getNumInitializedClientConnections() <
					(trustedVendor(hr.getUserAgent()) ?
					 (UltrapeerSettings.MAX_LEAVES.getValue() - 
                      RESERVED_GOOD_LEAF_CONNECTIONS) :
					 ALLOWED_BAD_LEAF_CONNECTIONS);
			}
            //return getNumInitializedClientConnections() 
			//  < (trustedVendor(hr.getUserAgent())
			//   ? MAX_LEAVES : ALLOWED_BAD_LEAF_CONNECTIONS);

        } else if (hr.isUltrapeer()) {
            //2. Ultrapeer.  Preference trusted vendors using BearShare's
            //clumping algorithm (see above).		   

			//if(goodConnection(hr)) {
				// otherwise, it is a high degree connection, so allow it if we 
				// need more connections
				//return connections < ULTRAPEER_CONNECTIONS;
			//}
			
			// USE THIS FOR TESTING DEFLATE IN THE WILD,
			// TO EVENTUALLY HAVE ALL CONNECTIONS BE DEFLATE-ENABLED.
			// If this is our last few connections, only allow it if it
			// accepts deflate-encoding.
			//if( connections >= ULTRAPEER_CONNECTIONS-6 ) {
			//    return hr.isDeflateAccepted();
			//}

			// if it's not a new high-density connection, only allow it if
			// our number of connections is below the maximum number of old
			// connections to allow
			return hasFreeUltrapeerSlots(); 
        }
		return false;
    }

	/**
	 * Helper method for determining whether or not the connecting node is
	 * a "trusted vendor", or a vendor that we have interacted with enough
	 * to feel confident that they are not sending overly aggressive 
	 * automated requeries, producing other excessive traffic, or otherwise
	 * being abusive.
	 *
	 * @param userAgentHeader the user-agent for the node, as reported in
	 *  the 0.6 handshake
	 * @return <tt>true</tt> if the vendor is considered trusted, otherwise
	 *  <tt>false</tt>
	 */
    private static boolean trustedVendor(String userAgentHeader) {
        if(userAgentHeader == null) return false;
        return (userAgentHeader.startsWith("LimeWire") ||
				userAgentHeader.startsWith("Swapper"));
    }
        

    /** Returns the number of connections to other ultrapeers.  Caller MUST hold
     *  this' monitor. */
    private int ultrapeerConnections() {
        //TODO3: augment state of this if needed to avoid loop
        int ret=0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            Connection mc=(Connection)iter.next();
            if (mc.isSupernodeConnection())
                ret++;
        }
        return ret;
    }
    
    /**
     * Returns the number of connections that are ultrapeer -> ultrapeer.
     * Caller MUST hold this' monitor.
     */
    private int ultrapeerToUltrapeerConnections() {
        //TODO3: augment state of this if needed to avoid loop
        int ret=0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            Connection mc=(Connection)iter.next();
            if (mc.isSupernodeSupernodeConnection())
                ret++;
        }
        return ret;     
    }   

    /** Returns the number of old-fashioned unrouted connections.  Caller MUST
     *  hold this' monitor. */
    private int oldConnections() {		
		// technically, we can allow old connections.
		int ret = 0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            Connection mc=(Connection)iter.next();
            if (!mc.isSupernodeConnection())
                ret++;
        }
        return ret;
    }

    /**
     * Tells if this node thinks that more ultrapeers are needed on the 
     * network. This method should be invoked on a ultrapeer only, as
     * only ultrapeer may have required information to make informed
     * decision.
     * @return true, if more ultrapeers needed, false otherwise
     */
    public boolean supernodeNeeded() {
        //if more than 90% slots are full, return true         
		if(getNumInitializedClientConnections() >= 
           (UltrapeerSettings.MAX_LEAVES.getValue() * 0.9)){
            return true;
        } else {
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
    public List getInitializedConnections2() {
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
     * Accessor for the <tt>Set</tt> of push proxies for this node.  If
     * there are no push proxies available, or if this node is an Ultrapeer,
     * this will return an empty <tt>Set</tt>.
     *
     * @return a <tt>Set</tt> of push proxies with a maximum size of 4
     * 
     *  TODO: should the set of pushproxy UPs be cached and updated as
     *  connections are killed and created?
     */
    public Set getPushProxies() {
        if (isShieldedLeaf()) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for proxy support is cached boolean
            // value
            Iterator ultrapeers = getInitializedConnections().iterator();
            Set proxies = new HashSet();
            while (ultrapeers.hasNext() && (proxies.size() < 4)) {
                Connection currMC = (Connection) ultrapeers.next();
                if (currMC.getPushProxyPort() >= 0)
                    proxies.add(currMC);
            }
            return proxies;
        } 

        return DataUtils.EMPTY_SET;
    }

    /**
     * Returns the endpoints of the best known ultrapeers.  This include
     * both ultrapeers we are connected to and marked ultrapeer pongs.
     * @return Returns the endpoints it is connected to. 
     */ 
    public Set getSupernodeEndpoints(){
        Set retSet = new HashSet();
        //get an iterator over _initialized connections, and iterate to
        //fill the retSet with ultrapeer endpoints
        for(Iterator iterator = _initializedConnections.iterator();
            iterator.hasNext();)
        {
            Connection connection = (Connection)iterator.next();
            if(connection.isSupernodeConnection())
                retSet.add(new Endpoint(
                    connection.getInetAddress().getAddress(),
                    connection.getListeningPort()));
        }
        //add the best few endpoints from the hostcatcher.
        Iterator iterator =
			RouterService.getHostCatcher().getUltrapeerHosts(MAX_ULTRAPEER_ENDPOINTS);
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
			Connection connection = (Connection)iter.next();
			if(connection.isSupernodeConnection() && 
			   connection.isGUESSUltrapeer()) {				
				return new Endpoint(connection.getInetAddress().getAddress(),
									connection.getListeningPort());
			}
		}
		return null;
	}
    

    /** Returns a <tt>List<tt> of Ultrapeers connected via TCP that are GUESS
     *  enabled.
     *
     * @return A non-null List of GUESS enabled, TCP connected Ultrapeers.  The
     * are represented as Connections.
     */
	public List getConnectedGUESSUltrapeers() {
        List retList = new ArrayList();
		for(Iterator iter=_initializedConnections.iterator(); iter.hasNext();) {
			Connection connection = (Connection)iter.next();
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
    private void connectionInitializingIncoming(Connection c) {
        connectionInitializing(c);
    }
    
    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     */
    private boolean connectionInitialized(Connection c) {
        if(_connections.contains(c)) {
	        // build the queues and start the output runner.
	        // this MUST be done before _initializedConnections
	        // or _initializedClientConnections has added this
	        // connection to its list.  otherwise, messages may
	        // attempt to be sent to the connection before it has
	        // set up its output queues.
            c.buildAndStartQueues();
            
            //update the appropriate list of connections
            if(!c.isSupernodeClientConnection()){
                //REPLACE _initializedConnections with the list
                //_initializedConnections+[c]
                List newConnections=new ArrayList(_initializedConnections);
                newConnections.add(c);
                _initializedConnections=newConnections;
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections++;                
            }else{
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections+[c]
                List newConnections
                    =new ArrayList(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections=newConnections;
            }
	        // do any post-connection initialization that may involve sending.
	        c.postInit();
	        // sending the ping request.
    		sendInitialPingRequest(c);            
            return true;
        }
        return false;
        
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
        //1. Prevent any new threads from starting.  Note that this does not
        //   affect the permanent settings.  We have to use setKeepAliveNow
        //   to ignore the fact that we have a client-ultrapeer connection.
        setKeepAlive(0);
        //2. Remove all connections.
        for (Iterator iter=getConnections().iterator();
             iter.hasNext(); ) {
            Connection c=(Connection)iter.next();
            remove(c);
            //add the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                _catcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                    c.getListeningPort()), true);
            }   
        }
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * (keep-alive) is non-zero and recontacts the pong server as needed.  
     */
    public synchronized void connect() {

        //Tell the HostCatcher it's ok to reconnect to router.limewire.com.
        _catcher.expire();

        //Ensure outgoing connections is positive.
		int outgoing = ConnectionSettings.NUM_CONNECTIONS.getValue();
        if (outgoing < 1) {
			ConnectionSettings.NUM_CONNECTIONS.revertToDefault();
			outgoing = ConnectionSettings.NUM_CONNECTIONS.getValue();
        }

        //Actually notify the backend.		
        setKeepAlive(outgoing);
    }
    
    /** 
     * Sends the initial ping request to a newly initialized connection.  The
     * ttl of the PingRequest will be 1 if we don't need any connections.
     * Otherwise, the ttl = max ttl.
     */
    private void sendInitialPingRequest(Connection connection) {
        if(connection.supportsPongCaching()) return;

        //We need to compare how many connections we have to the keep alive to
        //determine whether to send a broadcast ping or a handshake ping, 
        //initially.  However, in this case, we can't check the number of 
        //connection fetchers currently operating, as that would always then
        //send a handshake ping, since we're always adjusting the connection 
        //fetchers to have the difference between keep alive and num of
        //connections.
        PingRequest pr;
        if (getNumInitializedConnections() >= _keepAlive)
            pr = new PingRequest((byte)1);   
        else
            pr = new PingRequest((byte)4);   
        
        connection.send(pr);
    }

    /**
     * An unsynchronized version of remove, meant to be used when the monitor
     * is already held.  This version does not kick off ConnectionFetchers;
     * only the externally exposed version of remove does that.
     */
    private void removeInternal(Connection c) {
        // 1a) Remove from the initialized connections list and clean up the
        // stuff associated with initialized connections.  For efficiency 
        // reasons, this must be done before (2) so packets are not forwarded
        // to dead connections (which results in lots of thrown exceptions).
        if(!c.isSupernodeClientConnection()){
            int i=_initializedConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initializedConnections with the list
                //_initializedConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedConnections);
                newConnections.remove(c);
                _initializedConnections=newConnections;
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections--;                
            }
        }else{
            //check in _initializedClientConnections
            int i=_initializedClientConnections.indexOf(c);
            if (i != -1) {
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
        RouterService.getMessageRouter().removeConnection(c);

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
        //achieve NUM_CONNECTIONS ultrapeer connections.  But to prevent
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
        int multiple;
        
        // The end result of the following logic, assuming _keepAlive
        // is 32 for Ultrapeers, is:
        // When we have 22 active peer connections, we fetch
        // (27-current)*2 connections.
        // All other times, for Ultrapeers, we will fetch
        // (32-current)*4, up to a maximum of 20.
        // For leaves, assuming they maintin 4 Ultrapeers,
        // we will fetch (4-current)*4 connections.
        
        // If we have not accepted incoming, fetch 4 times
        // as many connections as we need.
        if( !RouterService.acceptedIncomingConnection() ) {
            multiple = 4;
        }
        // Otherwise, if we're a leaf, not ultrapeer capable,
        // or have not become an Ultrapeer to anyone,
        // also fetch 4 times as many connections as we need.
        else if( isShieldedLeaf() || !isSupernode() ||
                 getNumUltrapeerConnections() == 0 ) {
            multiple = 4;
        }
        // Otherwise (we are actively Ultrapeering to someone)
        // If we needed more than connections, still fetch
        // 4 times as many connections as we need.
        else if( neededConnections > 10 ) {
            multiple = 4;
        }
        // Otherwise, if we need less than 10 connections,
        // decrement the amount of connections we need by 5,
        // leaving 5 slots open for newcomers to use,
        // and decrease the rate at which we fetch to
        // 2 times the amount of connections.
        else {
            multiple = 2;
            neededConnections -= 5;
        }
            
        int need = Math.min(20, multiple*neededConnections) 
                 - _fetchers.size()
                 - _initializingFetchedConnections.size();

        // Start connection fetchers as necessary
        while(need > 0) {
            // This kicks off the thread for the fetcher
            _fetchers.add(new ConnectionFetcher());
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
            Connection connection = (Connection)
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
    private void initializeFetchedConnection(Connection mc,
                                             ConnectionFetcher fetcher)
            throws NoGnutellaOkException, BadHandshakeException, IOException {
        synchronized(this) {
            if(fetcher.isInterrupted()) {
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so throw IOException.
                // (This prevents fetcher from continuing!)
                throw new IOException("connection fetcher");
            }

            _initializingFetchedConnections.add(mc);
            _fetchers.remove(fetcher);
            connectionInitializing(mc);
            // No need to adjust connection fetchers here.  We haven't changed
            // the need for connections; we've just replaced a ConnectionFetcher
            // with a Connection.
        }
        RouterService.getCallback().connectionInitializing(mc);

        try {
            mc.initialize();
        } catch(IOException e) {
            synchronized(ConnectionManager.this) {
                _initializingFetchedConnections.remove(mc);
                removeInternal(mc);
                // We've removed a connection, so the need for connections went
                // up.  We may need to launch a fetcher.
                adjustConnectionFetchers();
            }
            throw e;
        }
        finally {
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(mc);
        }
        
        completeConnectionInitialization(mc, true);
    }

    /** 
     * Indicates that we are a client node, and have received ultrapeer
     * connection.  This may choose to adjust its keep-alive. 
     */
    private synchronized void gotShieldedClientSupernodeConnection() {
        //How many leaf connections should we have?  There's a tension between
        //doing what LimeWire thinks is best and what the user wants.  Ideally
        //we would set the NUM_CONNECTIONS iff the user hasn't recently manually
        //adjusted it.  Because this is a pain to implement, we use a hack; only
        //adjust the NUM_CONNECTIONS if there is only one shielded leaf-ultrapeer
        //connection.  Typically this will happen just once, when we enter leaf
        //mode.  Note that we actually call ultrapeerConnections() instead of a
        //"clientSupernodeConnections()" method; if this method is called and
        //ultrapeerConnections()==1, there is exactly one client-ultrapeer
        //connection.
        boolean firstShieldedConnection = (ultrapeerConnections()==1) 
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
    private void processConnectionHeaders(Connection connection){
        if(!connection.receivedHeaders()) {
            return;
        }

        //get the connection headers
        Properties headers = connection.headers().props();
        //return if no headers to process
        if(headers == null) return;
        
        //update the addresses in the host cache (in case we received some
        //in the headers)
        updateHostCache(connection.headers());
                
        //get remote address.  If the more modern "Listen-IP" header is
        //not included, try the old-fashioned "X-My-Address".
        String remoteAddress 
            = headers.getProperty(HeaderNames.LISTEN_IP);
        if (remoteAddress==null)
            remoteAddress 
                = headers.getProperty(HeaderNames.X_MY_ADDRESS);

        //set the remote port if not outgoing connection (as for the outgoing
        //connection, we already know the port at which remote host listens)
        if((remoteAddress != null) && (!connection.isOutgoing())) {
            int colonIndex = remoteAddress.indexOf(':');
            if(colonIndex == -1) return;
            colonIndex++;
            if(colonIndex > remoteAddress.length()) return;
            try {
                int port =
                    Integer.parseInt(remoteAddress.substring(colonIndex).trim());
                if(NetworkUtils.isValidPort(port)) {
                	// for incoming connections, set the port based on what it's connection
                	// headers say the listening port is
                    connection.setListeningPort(port);
                }
            } catch(NumberFormatException e){
                // should nothappen though if the other client is well-coded
            } 
        }
    }
   
    /** 
     * Returns true if this can safely switch from Ultrapeer to leaf mode.
	 * Typically this means that we are an Ultrapeer and have no leaf
	 * connections.
	 *
	 * @return <tt>true</tt> if we will allow ourselves to become a leaf,
	 *  otherwise <tt>false</tt>
     */
    public boolean allowLeafDemotion() {
		_leafTries++;
        
        //if is a ultrapeer, and have other connections (client or ultrapeer),
        //or the ultrapeer status is forced, dont change mode
        int connections = getNumInitializedConnections()
			+ getNumInitializedClientConnections();
        
        if (UltrapeerSettings.FORCE_ULTRAPEER_MODE.getValue() 
            || (isSupernode() && connections > 0)) {
            return false;
		} else if(SupernodeAssigner.isTooGoodToPassUp() && 
				  _leafTries < _demotionLimit) {
			return false;
		}
		return true;
    }


	/**
	 * Notifies the connection manager that it should attempt to become an
	 * Ultrapeer.  If we already are an Ultrapeer, this will be ignored.
	 *
	 * @param demotionLimit the number of attempts by other Ultrapeers to
	 *  demote us to a leaf that we should allow before giving up in the
	 *  attempt to become an Ultrapeer
	 */
	public void tryToBecomeAnUltrapeer(int demotionLimit) {
		if(isSupernode()) return;
		_demotionLimit = demotionLimit;
		_leafTries = 0;		
		disconnect();
		connect();
	}
    
    /**
     * Adds the X-Try-Ultrapeer hosts from the connection headers to the
     * host cache.
     *
     * @param headers the connection headers received
     */
    private void updateHostCache(HandshakeResponse headers) { 	

        if(!headers.hasXTryUltrapeers()) return;

        //get the ultrapeers, and add those to the host cache
        String hostAddresses = headers.getXTryUltrapeers();
    
         
        //tokenize to retrieve individual addresses
        StringTokenizer st = new StringTokenizer(hostAddresses,
            Constants.ENTRY_SEPARATOR);
        //iterate over the tokens
        while(st.hasMoreTokens()){
            //get an address
            String address = st.nextToken().trim();
            Endpoint e;
            try{
                e = new Endpoint(address);
            }
            catch(IllegalArgumentException iae){
                continue;
            }
            //set the good priority, if specified
            //add it to the catcher
            _catcher.add(e, true);
        }
    }

    

    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.  If this is an incoming connection and there are no
     * slots available, rejects it and throws IOException.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(Connection c)
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
            RouterService.getCallback().connectionInitializing(c);
        }
            
        try {
            c.initialize();

        } catch(IOException e) {
            remove(c);
            throw e;
        }
        finally {
            //if the connection received headers, process the headers to
            //take steps based on the headers
            processConnectionHeaders(c);
        }

		
        //If there's not space for the connection, reject it.  This mechanism
        //works for Gnutella 0.4 connections, as well as some odd cases for 0.6
        //connections.  Sometimes Connections are handled by headers
        //directly.
        if (!c.isOutgoing() && !allowConnection(c)) {
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
            RouterService.getCallback().connectionInitializing(c);
        }

        completeConnectionInitialization(c, false);
    }

    /**
     * Performs the steps necessary to complete connection initialization.
     *
     * @param mc the <tt>Connection</tt> to finish initializing
     * @param fetched Specifies whether or not this connection is was fetched
     *  by a connection fetcher.  If so, this removes that connection from 
     *  the list of fetched connections being initialized, keeping the
     *  connection fetcher data in sync
     */
    private void completeConnectionInitialization(Connection mc, 
                                                  boolean fetched) {
        boolean connectionOpen = false;
        synchronized(this) {
            if(fetched) {
                _initializingFetchedConnections.remove(mc);
            }
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            connectionOpen = connectionInitialized(mc);
            if(connectionOpen) {
                // check to see if this is the first leaf to ultrapeer 
                // connection we've made.  if it is, then we're a leaf,
                // and we'll switch the keep alive to the number of 
                // Ultrapeer connections to maintain as a leaf
                if(mc.isClientSupernodeConnection()) {
                    gotShieldedClientSupernodeConnection();
                }
            }

            if(connectionOpen) {
                RouterService.getCallback().connectionInitialized(mc);
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
     * Connections created through createConnectionAsynchronously and
     * createConnectionBlocking
     */
    private class OutgoingConnector implements Runnable {
        private final Connection _connection;
        private final boolean _doInitialization;

        /**
		 * Creates a new <tt>OutgoingConnector</tt> instance that will 
		 * attempt to create a connection to the specified host.
		 *
		 * @param connection the host to connect to
         */
        public OutgoingConnector(Connection connection,
								 boolean initialize) {
            _connection = connection;
            _doInitialization = initialize;
        }

        public void run() {
            try {
				if(_doInitialization) {
					initializeExternallyGeneratedConnection(_connection);
				}
				startConnection(_connection);
            } catch(IOException e) {
            } catch(Throwable e) {
                //Internal error!
                ErrorService.error(e);
            }
            finally{
                if (_connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
            }
        }
    }

	/**
	 * Runs standard calls that should be made whenever a connection is fully
	 * established and should wait for messages.
	 *
	 * @param conn the <tt>Connection</tt> instance to start
	 * @throws <tt>IOException</tt> if there is an excpetion while looping
	 *  for messages
	 */
	private void startConnection(Connection conn) throws IOException {
	    Thread.currentThread().setName("MessageLoopingThread");
	    try {
    		if(conn.isGUESSUltrapeer()) {
    			QueryUnicaster.instance().addUnicastEndpoint(conn.getInetAddress(),
    				conn.getListeningPort());
    		}
    
    		// this can throw IOException
    		conn.loopForMessages();
        } finally {
            // If we ever exit this method, it is because the connection died,
            // so we must clean up the references to it.
            remove(conn);
        }
	}
    
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
    private class ConnectionFetcher extends Thread {

        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            setName("ConnectionFetcher");

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
            } while ( !IPFilter.instance().allow(endpoint.getHostname()) || 
                      isConnectedTo(endpoint) );                      

            Assert.that(endpoint != null);

            Connection connection = new Connection(
                endpoint.getHostname(), endpoint.getPort());

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
            } catch(Throwable e) {
                //Internal error!
                ErrorService.error(e);
            }
            finally{
                if (connection.isClientSupernodeConnection())
                    lostShieldedClientSupernodeConnection();
            }
        }
	}
}
