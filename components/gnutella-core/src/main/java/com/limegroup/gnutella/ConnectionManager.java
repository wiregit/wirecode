
// Edited for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.connection.ConnectionChecker;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.QuestionsHandler;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.SystemUtils;

/**
 * The list of all ManagedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, accepts incoming connections, and
 * fetches "automatic" outgoing connections as needed.  Creates threads for
 * handling these connections when appropriate.
 *
 * Because this is the only list of all connections, it plays an important role
 * in message broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitializedConnections() methods.  Adding and removing
 * connections is a slower operation.<p>
 *
 * LimeWire follows the following connection strategy:<br>
 * As a leaf, LimeWire will ONLY connect to 'good' Ultrapeers.  The definition
 * of good is constantly changing.  For a current view of 'good', review
 * HandshakeResponse.isGoodUltrapeer().  LimeWire leaves will NOT deny
 * a connection to an ultrapeer even if they've reached their maximum
 * desired number of connections (currently 4).  This means that if 5
 * connections resolve simultaneously, the leaf will remain connected to all 5.
 * <br>
 * As an Ultrapeer, LimeWire will seek outgoing connections for 5 less than
 * the number of it's desired peer slots.  This is done so that newcomers
 * on the network have a better chance of finding an ultrapeer with a slot
 * open.  LimeWire ultrapeers will allow ANY other ultrapeer to connect to it,
 * and to ensure that the network does not become too LimeWire-centric, it
 * reserves 3 slots for non-LimeWire peers.  LimeWire ultrapeers will allow
 * ANY leaf to connect, so long as there are atleast 15 slots open.  Beyond
 * that number, LimeWire will only allow 'good' leaves.  To see what consitutes
 * a good leave, view HandshakeResponse.isGoodLeaf().  To ensure that the
 * network does not remain too LimeWire-centric, it reserves 3 slots for
 * non-LimeWire leaves.<p>
 *
 * ConnectionManager has methods to get up and downstream bandwidth, but it
 * doesn't quite fit the BandwidthTracker interface.
 */
public class ConnectionManager {

    /**
     * Timestamp for the last time the user selected to disconnect.
     */
    private volatile long _disconnectTime = -1;
    
    /**
     * Timestamp for the last time we started trying to connect
     */
    private volatile long _connectTime = Long.MAX_VALUE;

    /**
     * Timestamp for the time we began automatically connecting.  We stop
     * trying to automatically connect if the user has disconnected since that
     * time.
     */
    private volatile long _automaticConnectTime = 0;

    /**
     * Flag for whether or not the auto-connection process is in effect.
     */
    private volatile boolean _automaticallyConnecting;

    /**
     * Timestamp of our last successful connection.
     */
    private volatile long _lastSuccessfulConnect = 0;

    /**
     * Timestamp of the last time we checked to verify that the user has a live
     * Internet connection.
     */
    private volatile long _lastConnectionCheck = 0;


    /**
     * Counter for the number of connection attempts we've made.
     */
    private volatile static int _connectionAttempts;

    private static final Log LOG = LogFactory.getLog(ConnectionManager.class);

    //done

    /*
     * Tour Point
     * 
     * This is the number of connections LimeWire makes.
     * For LimeWire Pro, it's 5 instead of 3.
     * It's not a setting, because then users could change it themselves.
     * The build script does a find and replace to turn the 3 here into a 5.
     */

    /** 3, as a leaf, we'll keep 3 connections up to ultrapeers. */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF = 3;

    //do

    /**
     * How many connect back requests to send if we have a single connection
     */
    public static final int CONNECT_BACK_REDUNDANT_REQUESTS = 3;

    //done

    /** If the user leaves the computer for a half hour, we'll drop down to just 1 ultrapeer connection instead of 3. */
    private static final int MINIMUM_IDLE_TIME = 30 * 60 * 1000; // 30 minutes

    //do
    
    /**
     * The number of leaf connections reserved for non LimeWire clients.
     * This is done to ensure that the network is not solely LimeWire centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_LEAVES = 2;

    //done

    /** The number of ultrapeers we should have, 32 if we're an ultrapeer or 3 if we're a leaf. */
    private volatile int _preferredConnections = -1; // Initialize to -1 to indicate the number isn't set yet

    //do
    
    /**
     * Reference to the <tt>HostCatcher</tt> for retrieving host data as well
     * as adding host data.
     */
    private HostCatcher _catcher;

    /** Threads trying to maintain the NUM_CONNECTIONS.
     *  LOCKING: obtain this. */
    private final List /* of ConnectionFetcher */ _fetchers =
        new ArrayList();
    /** Connections that have been fetched but not initialized.  I don't
     *  know the relation between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtain this. */
    private final List /* of ManagedConnection */ _initializingFetchedConnections =
        new ArrayList();

    /**
     * dedicated ConnectionFetcher used by leafs to fetch a
     * locale matching connection
     * NOTE: currently this is only used by leafs which will try
     * to connect to one connection which matches the locale of the
     * client.
     */
    private ConnectionFetcher _dedicatedPrefFetcher;

    /**
     * boolean to check if a locale matching connection is needed.
     */
    private volatile boolean _needPref = true;
    
    /**
     * boolean of whether or not the interruption of the prefFetcher thread
     * has been scheduled.
     */
    private boolean _needPrefInterrupterScheduled = false;

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
     * INVARIANT: _nonLimeWireLeaves is the number of connections
     *   in _initializedClientConnections for which isLimeWire is false
     * INVARIANT: _nonLimeWirePeers is the number of connections
     *   in _initializedConnections for which isLimeWire is false
     *
     * LOCKING: _connections, _initializedConnections and
     *   _initializedClientConnections MUST NOT BE MUTATED.  Instead they should
     *   be replaced as necessary with new copies.  Before replacing the
     *   structures, obtain this' monitor.  This avoids lock overhead when
     *   message broadcasting, though it makes adding/removing connections
     *   much slower.
     */

    //done

    /*
     * TODO:: why not use sets here??
     */

    /** _connections lists all the ultrapeers we're trying to connect to or are connected to. */
    private volatile List _connections                  = Collections.EMPTY_LIST; // We'll put ManagedConnection objects in this List
    /** _initializedConnections is a list of all the ultrapeers we have open connections to. */
    private volatile List _initializedConnections       = Collections.EMPTY_LIST; // We'll put ManagedConnection objects in this List
    /** _initializedClientConnections is a list of all our leaves. */
    private volatile List _initializedClientConnections = Collections.EMPTY_LIST; // We'll put ManagedConnection objects in this List

    /** How many connections up to ultapeers we have. We're a leaf. */
    private volatile int _shieldedConnections = 0;
    /** How many non-LimeWire leaves we have. We're an ultrapeer. */
    private volatile int _nonLimeWireLeaves   = 0;
    /** How many non-LimeWire ultrapeers we're connected to. We're an ultrapeer. */
    private volatile int _nonLimeWirePeers    = 0;
    /** How many ultrapeers we're connected to that match our language preference. We're an ultrapeer. */
    private volatile int _localeMatchingPeers = 0;

    /**
     * The number of times remote computers told us they didn't need an ultrapeer after we told them we were one.
     * 
     * allowLeafDemotion() increments this when HandshakeResponder.respondToOutgoing() calls it.
     * When this number grows bigger than _demotionLimit, we'll drop down to leaf mode.
	 */
	private volatile int _leafTries;

	/**
     * The number of times we'll ignore remote computers telling us to drop down to leaf mode when we're trying to connect as an ultrapeer.
     * 
     * By default, this value is 0.
     * If SupernodeAssigner.setUltrapeerCapable() finds us too good to pass up, it will set _demotionLimit to 4, 8, 12, or more.
     * Then, allowLeafDemotion() will return false until _leafTries grows to equal this _demotionLimit.
	 */
	private volatile int _demotionLimit = 0;

    /**
     * The speed we're uploading Gnutella packet data, in bytes/millisecond.
     * This is the total of the current upstream speeds from all our ManagedConnection objects.
     */
    private volatile float _measuredUpstreamBandwidth = 0.f;

    /**
     * The speed we're downloading Gnutella packet data, in bytes/millisecond.
     * This is the total of the current downstream speeds from all our ManagedConnection objects.
     */
    private volatile float _measuredDownstreamBandwidth = 0.f;

    /**
     * Make the ConnectionManager object.
     * Call initialize() before using any of the methods.
     * 
     * RouterService defines the static ConnectionManager object named manager, which calls this.
     * Java will set all the member variables to 0.
     * There's no code here because there's nothing else we have to do.
     */
    public ConnectionManager() {}

    //do
    
    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     * 
     * 
     * 
     * RouterService.start() calls this.
     * 
     */
    public void initialize() {
        _catcher = RouterService.getHostCatcher();

        // schedule the Runnable that will allow us to change
        // the number of connections we're shooting for if
        // we're idle.
        
        // If the operating system we're running on can tell us when the user has stepped away from the computer
        if (SystemUtils.supportsIdleTime()) {

            // Have the RouterService run some code every so often for us
            RouterService.schedule(

                // Make a new class right here that doesn't have a name, but is Runnable and has a run() method so a thread can run it
                new Runnable() {

                    // The RouterService will create a thread, and that thread will call this run() method
                    public void run() {

                        
                        
                        setPreferredConnections();
                    }
                },
                
                1000,
                1000
            );
        }
    }


    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public ManagedConnection createConnectionBlocking(String hostname,
        int portnum)
		throws IOException {
        ManagedConnection c =
			new ManagedConnection(hostname, portnum);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c);
        // Kick off a thread for the message loop.
        Thread conn =
            new ManagedThread(new OutgoingConnector(c, false), "OutgoingConnector");
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
			new OutgoingConnector(new ManagedConnection(hostname, portnum),
								  true);
        // Initialize and loop for messages on another thread.

		Thread outgoingConnectionRunner =
			new ManagedThread(outgoingRunner, "OutgoingConnectionThread");
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
         Thread.currentThread().setName("IncomingConnectionThread");
         ManagedConnection connection = new ManagedConnection(socket);
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
         }
     }


    /**
     * Removes the specified connection from currently active connections, also
     * removing this connection from routing tables and modifying active
     * connection fetchers accordingly.
     *
     * @param mc the <tt>ManagedConnection</tt> instance to remove
     */
    public synchronized void remove(ManagedConnection mc) {

        // True, let this method remove a connection
		if (!ConnectionSettings.REMOVE_ENABLED.getValue()) return; // This would only be disabled for tests

        removeInternal(mc);

        adjustConnectionFetchers();
    }


    //done

    /**
     * True if we're sending "X-Ultrapeer: true" or have a fellow ultrapeer connection or a leaf.
     * 
     * Here's how the process of becoming an ultrapeer works:
     * Offline and by ourselves, we determine if we have the computer, Internet connection, and online time we need to be an ultrapeer.
     * When we decide that we do, isSupernodeCapable() is true.
     * When this is true, we'll start greeting remote computers with "X-Ultrapeer: true".
     * When one acceps us as their ultrapeer, and they become our leaf, then we are an ultrapeer on the network.
     * When this happens, isActiveSupernode() is true.
     * 
     * @return True if we're sending handshake headers that say we're an ultrapeer, or have connections as an ultrapeer.
     *         False if we're not saying we're an ultrapeer and we don't have any ultrapeer connections.
     */
    public boolean isSupernode() {

        // Return true if we're trying to become an ultrapeer, or are one
        return

            isActiveSupernode() || // We're on the network acting as an ultrapeer right now, or
            isSupernodeCapable();  // We have a fast enough computer and Internet connection to be one
    }

    /**
     * True if we have the computer, Internet connection, and upload time we need to be an ultrapeer on the Gnutella network.
     * If this is true, we'll present ourself to remote computers as an ultrapeer, greeting them with a "X-Ultrapeer: true" header.
     * 
     * We can be ultrapeer-capable without having any leaves yet, it doesn't have anything to do with our connections on the network.
     * When a remote computer accepts our ultrapeer status in the handshake and becomes our leaf, then we'll be an ultrapeer.
     * 
     * Recently, LimeWire developers added the minConnectTimePassed() test.
     * minConnectTimePassed() returns false for the first 10 seconds of our attempts to connect, and true after that.
     * So, even when we are connecting as an ultrapeer, we'll say "X-Ultrapeer: false" to computers we reach in the first 10 seconds.
     * This is part of a strategy to have fewer ultrapeers on the Gnutella network.
     * 
     * @return True if we have what we need to be an ultrapeer on the Gnutella network, false if we don't.
     */
    public boolean isSupernodeCapable() {

        // Return true if all of the following things are true
        return

            // The IP address we've been telling remote computers is a real Internet IP address
            !NetworkUtils.isPrivate() && // Until we're externally contactable, we tell remote computers our LAN address

            // At some point in the past, SupernodeAssigner.setUltrapeerCapable() found our computer and Internet connection worthy of ultrapeer status
            UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue() &&

            // We don't have any connections up to ultrapeers
            !isShieldedLeaf() &&

            // Settings allow us to be an ultrapeer
            !UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue() &&

            // We don't have to connect through a proxy server
            !isBehindProxy() &&

            // We started trying to connect to Gnutella computers more than 10 seconds ago
            minConnectTimePassed();
    }

    /**
     * True if it's been 10 seconds since we started connecting to Gnutella computers, false if it hasn't been 10 seconds yet.
     * 
     * @return False for the first 10 seconds after _connectTime.
     *         True after that.
     */
    private boolean minConnectTimePassed() {

        // Return true if we've been trying to connect for more than 10 seconds, false if it hasn't been 10 seconds yet
        return Math.max(0, (System.currentTimeMillis() - _connectTime)) / 1000 >= UltrapeerSettings.MIN_CONNECT_TIME.getValue();
    }

    /**
     * True if we have to connect through a proxy server.
     * 
     * @return True if the user configured a proxy server for us to use, false if we can make connections directly.
     */
    public boolean isBehindProxy() {

        // If settings have a proxy server configured, return true
        return ConnectionSettings.CONNECTION_METHOD.getValue() != ConnectionSettings.C_NO_PROXY;
    }

    /*
     * On the Gnutella network, we can be an ultrapeer or a leaf.
     * isActiveSupernode() and isShieldedLeaf() determine which role we are in.
     * They do it by looking only at the Gnutella connections to remote computers we have.
     */
    
    /**
     * True if we have no connections up to ultrapeers, and at least one to a fellow ultrapeer or one to a leaf below us.
     * This means we're on the Gnutella network as an ultrapeer.
     * 
     * There are 3 kinds of connections on the Gnutella network.
     * As a leaf, we'll have 3 connections up to ultrapeers.
     * As an ultrapeer, we'll have 32 connections to fellow ultrapeers, and 30 connections down to leaves.
     * 
     * isActiveSupernode() looks at our open connections to determine which network role we are in.
     * If we don't have any connections up to ultrapeers, and we have at least 1 fellow ultrapeer or down-to-leaf connection, we're an active supernode.
     * Otherwise, we're not.
     * 
     * @return True if we don't have any connections up to ultrapeers, and we have at least 1 fellow ultrapeer or down-to-leaf connection.
     *         False otherwise.
     */
    public boolean isActiveSupernode() {

        // Return true if we don't have any connections up to ultrapeers, and we have some leaves, or we're connected to some ultrapeers
        return

            // We don't have any connections up to ultrapeers, and
            !isShieldedLeaf() &&

            (_initializedClientConnections.size() > 0 || // We have some leaves, or
            _initializedConnections.size() > 0);         // We're connected to some fellow ultrapeers
    }

    /**
     * True if we have some connections up to ultrapeers.
     * This means we are a leaf.
     * 
     * @return True if we don't have any connections up to ultrapeers, false if we do
     */
    public boolean isShieldedLeaf() {

        // Return true if we have some connections up to ultrapeers
        return _shieldedConnections != 0;
    }

    //do
    
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
    boolean isConnectedTo(String hostName) {
        //A clone of the list of all connections, both initialized and
        //uninitialized, leaves and unrouted.  If Java could be prevented from
        //making certain code transformations, it would be safe to replace the
        //call to "getConnections()" with "_connections", thus avoiding a clone.
        //(Remember that _connections is never mutated.)
        List connections=getConnections();
        for (Iterator iter=connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            if (mc.getAddress().equals(hostName))
                return true;
        }
        return false;
    }
    
    //done

    /**
     * The number of Gnutella computers we're connected to or at least trying to connect to.
     * 
     * @return The size of the _connections list
     */
    public int getNumConnections() {

        // The _connections list contains all the remote computers we're trying to connect to and are connected to
        return _connections.size();
    }

    /**
     * The number of ultrapeers we're connected to.
     * This is the size of the _initializedConnections list.
     * The _connections list is bigger, because it also includes ultrapeers we're trying to connect to.
     * 
     * @return The number of ultrapeers we have open connections to
     */
    public int getNumInitializedConnections() {

        // _initializedConnections lists the ultrapeers we have open connections to
		return _initializedConnections.size(); // It's size is the number of ultrapeer connections we have
    }

    /**
     * The number of leaves we're connected to.
     * This is the size of the _initializedClientConnections list.
     * 
     * @return The number of leaves we have open connections to
     */
    public int getNumInitializedClientConnections() {
        
        // _initializedClientConnections lists the leaves we have open connections to
		return _initializedClientConnections.size(); // It's size is the number of leaves we have
    }

    /**
     * How many connections up to ultrapeers we have.
     * We are a leaf, and these connections are shielding us from the traffic of the Gnutella network.
     * 
     * @return The value of _shieldedConnections
     */
    public int getNumClientSupernodeConnections() {

        // Return how many connections up to ultrapeers we have
        return _shieldedConnections;
    }

    //do

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
    public int getNumFreeLeafSlots() {
        if (isSupernode())
			return UltrapeerSettings.MAX_LEAVES.getValue() -
				getNumInitializedClientConnections();
        else
            return 0;
    }

    //done

    /**
     * Calculate how many more LimeWire leaves we want.
     * 
     * @return The number of free leaf slots that LimeWire leaves can connect to
     */
    public int getNumFreeLimeWireLeafSlots() {

        /*
         * RESERVED_NON_LIMEWIRE_LEAVES is how many non-LimeWire leaves we want
         * _nonLimeWireLeaves           is how many non-LimeWire leaves we have
         * getNumFreeLeafSlots()        is how many more leaves we want
         */

        // Calculate how many more LimeWire leaves we want
        return Math.max(0, getNumFreeLeafSlots() - Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES - _nonLimeWireLeaves));
    }

    /**
     * We should connect to this many more ultrapeers.
     * This is the number of free non-leaf slots.
     * We calculate it by taking how many ultrapeers we should have, and subtracting the number we're connected to.
     * 
     * @return The number of free ultrapeer slots
     */
    public int getNumFreeNonLeafSlots() {

        // The number of ultrapeers we should have, minus the number we're connected to
        return _preferredConnections - getNumInitializedConnections();
    }

    //do
    
    /**
     * @return the number of free non-leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireNonLeafSlots() {
        
        /*
         * _nonLimeWirePeers is the number of other ultrapeers we're connected to.
         */
        return Math.max(0,
                        getNumFreeNonLeafSlots()
                        - Math.max(0, (int)
                                (ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections) 
                                - _nonLimeWirePeers)
                        - getNumLimeWireLocalePrefSlots()
                        );
    }
    
    /**
     * Returns true if we've made a locale-matching connection (or don't
     * want any at all).
     * 
     * Returns true if settings tell us not to worry about matching language with connections, or we have some language matching connections anyway.
     * Returns false if settings tell us to match language with connections, and we don't have any.
     */
    public boolean isLocaleMatched() {
        
        /*
         * _localeMatchingPeers is the number of ultrapeer connections we have to the same language as us.
         */
        
        return !ConnectionSettings.USE_LOCALE_PREF.getValue() || // If the program is not trying to connect to same-language computers, or
               _localeMatchingPeers != 0;                        // We have some connections that match our language, return true
    }

    /**
     * @return the number of locale reserved slots to be filled
     *
     * An ultrapeer may not have Free LimeWire Non Leaf Slots but may still
     * have free slots that are reserved for locales
     */
    public int getNumLimeWireLocalePrefSlots() {
        
        /*
         * _localeMatchingPeers is the number of ultrapeer connections we have to the same language as us.
         */
        
        // Return the number of empty slots we've reserved for remote computers with our language preference
        return Math.max(0, ConnectionSettings.NUM_LOCALE_PREF.getValue() - _localeMatchingPeers);
    }

    //done
    
    /**
     * True if we have enough connections to ultrapeers, false if we need more.
     * 
     * If we're a leaf, this means we have 3 connections to ultrapeers.
     * If we're an ultrapeer, this means we have 32 connections to ultrapeers.
     * 
     * _initializedConnections lists the ultrapeers we're connected to.
     * _preferredConnections is the number of ultrapers we should have.
     * 
     * @return True if we have enough connections to ultrapeers, false if we need more
     */
    public boolean isFullyConnected() {

        // If there are more ManagedConnections in the _initializedConnections list than _preferredConnections, we have enough
        return _initializedConnections.size() >= _preferredConnections;
    }

	/**
     * True if we have at least 1 Gnutella connection to a remote computer.
     * 
     * @return True if we're connected to the Gnutella network
	 */
	public boolean isConnected() {

        // Return true if we're connected to one ultrapeer or one leaf
		return

            ((_initializedClientConnections.size() > 0) || // We have some leaves
			(_initializedConnections.size()        > 0));  // We're connected to some ultrapeers, either as an ultrapeer or as a leaf
	}

    //do

	/**
	 * Returns whether or not we are currently attempting to connect to the
	 * network.
	 */
	public boolean isConnecting() {
	    if(_disconnectTime != 0)
	        return false;
	    if(isConnected())
	        return false;
	    synchronized(this) {
	        return _fetchers.size() != 0 ||
	               _initializingFetchedConnections.size() != 0;
	    }
	}

    //done

    /**
     * Totals the upload and download speeds of all the remote computers we're exchanging Gnutella packet data with.
     * SupernodeAssigner.collectBandwidthData() calls this once a second.
     */
    public void measureBandwidth() {

        // Make floating point variables to total the upload and download speeds from each ManagedConnection object
        float upstream   = 0.f;
        float downstream = 0.f;

        // Loop through the list of remote computers we have open connections to
        List connections = getInitializedConnections();
        for (Iterator iter = connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next(); // Point mc at each ManagedConnection object in the list

            // Have this ManagedConnection object compute new speeds based on what's happened to it in the last 10 seconds
            mc.measureBandwidth();

            // Add the upload and download speeds from this Gnutella connection to our totals
            upstream   += mc.getMeasuredUpstreamBandwidth(); // Get the current speed in bytes/millisecond
            downstream += mc.getMeasuredDownstreamBandwidth();
        }

        // Save the totals in member variables
        _measuredUpstreamBandwidth   = upstream;   // The speed we're uploading Gnutella packet data
        _measuredDownstreamBandwidth = downstream; // The speed we're downloading Gntuella packet data
    }

    /**
     * How fast we're sending Gnutella packet data to our connections on the Gnutella network.
     * 
     * @return The speed we're uploading Gnutella packet data, a float in bytes/millisecond
     */
    public float getMeasuredUpstreamBandwidth() {

        // Return the value that measureBandwidth() totaled
        return _measuredUpstreamBandwidth;
    }

    /**
     * How fast we're receiving Gnutella packet data from our connections on the Gnutella network.
     * 
     * @return The speed we're downloading Gnutella packet data, a float in bytes/millisecond
     */
    public float getMeasuredDownstreamBandwidth() {

        // Return the value that measureBandwidth() totaled
        return _measuredDownstreamBandwidth;
    }

    //do

    /**
     * Checks if the connection received can be accepted,
     * based upon the type of connection (e.g. client, ultrapeer,
     * temporary etc).
     * @param c The connection we received, for which to
     * test if we have incoming slot.
     * @return true, if we have incoming slot for the connection received,
     * false otherwise
     */
    private boolean allowConnection(ManagedConnection c) {

    	// TODO:kfaaborg receivedHeaders always returns true
        if(!c.receivedHeaders()) return false;
		return allowConnection(c.headers(), false);
    }

    /**
     * Determines if we have room to accept a new connection
     * Includes the possiblity of telling the remote ultrapeer to become a leaf
     * 
     * @param hr The headers from the remote computer
     * @return   true if we have an incoming slot for this, false if we don't
     */
    public boolean allowConnectionAsLeaf(HandshakeResponse hr) {
    	
    	// Call allow connection telling it the remote computer is a leaf
		return allowConnection(hr, true);
    }

    /**
     * Determines if we have room to accept a new connection
     * 
     * @param hr The headers from the remote computer
     * @return   true if we have an incoming slot for this, false if we don't
     */
     public boolean allowConnection(HandshakeResponse hr) {
    	 
    	 // Call allowConnection telling it if the remote computer is a leaf or ultrapeer
         return allowConnection(hr, !hr.isUltrapeer());
     }


    /**
     * Checks if there is any available slot of any kind.
     * @return true, if we have incoming slot of some kind,
     * false otherwise
     */
    public boolean allowAnyConnection() {
        //Stricter than necessary.
        //See allowAnyConnection(boolean,String,String).
        
        // If we have some connections up to ultrapeers
        if (isShieldedLeaf())
            return false;

        //Do we have normal or leaf slots?
        
        // Return true if
        return

            // We need more ultrapeers, or
            getNumInitializedConnections() < _preferredConnections ||
        
            // We're an ultrapeer
            (isSupernode() && getNumInitializedClientConnections() < UltrapeerSettings.MAX_LEAVES.getValue());
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

        // If we're testing the program with connection preferencing off, return true to allow everything
		if (!ConnectionSettings.PREFERENCING_ACTIVE.getValue()) return true;

		// If it has not said whether or not it's an Ultrapeer or a Leaf
		// (meaning it's an old-style connection), don't allow it.
		if(!hr.isLeaf() && !hr.isUltrapeer())
		    return false;


        /*
         * We prefer connections by vendor, using BearShare's clumping algorithm.
         * Let N be the keep-alive and K be RESERVED_GOOD_CONNECTIONS.
         * In BearShare's implementation, K is 1.
         * 
         * Allow any connections in for the first N-K slots.
         * But only allow good vendors for the last K slots.
         * In other words, accept a connection C if there are fewer than N connections and one of the following is true:
         * C is a good vendor or
         * there are fewer than N-K connections.
         * With time, this converges on all good connections.
         */

        // The number of our connection attempts that have failed
		int limeAttempts = ConnectionSettings.LIME_ATTEMPTS.getValue();
		
        //Don't allow anything if disconnected.
        if (!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getValue() && // If settings tell us not to make connections when the program is disconnected, and
            _preferredConnections <= 0) {                              // We don't want to maintain any connections right now
            
            // Refuse this new connection
            return false;

        //If a leaf (shielded or not), check rules as such.
		} else if (isShieldedLeaf() || !isSupernode()) {
		    // require ultrapeer.
		    if(!hr.isUltrapeer())
		        return false;
		    
		    // If it's not good, or it's the first few attempts & not a LimeWire, 
		    // never allow it.
		    if(!hr.isGoodUltrapeer() || 
		      (Sockets.getAttempts() < limeAttempts && !hr.isLimeWire())) {
		        return false;
		    // if we have slots, allow it.
		    } else if (_shieldedConnections < _preferredConnections) { // We have fewer connections up to ultrapeers than we want to have
		        // if it matched our preference, we don't need to preference
		        // anymore.
		        if(checkLocale(hr.getLocalePref()))
		            _needPref = false;

                // while idle, only allow LimeWire connections.
                if (isIdle()) 
                    return hr.isLimeWire();

                return true;
            } else {
                // if we were still trying to get a locale connection
                // and this one matches, allow it, 'cause no one else matches.
                // (we would have turned _needPref off if someone matched.)
                if(_needPref && checkLocale(hr.getLocalePref()))
                    return true;

                // don't allow it.
                return false;
            }
		} else if (hr.isLeaf() || leaf) {
		    // no leaf connections if we're a leaf.
		    if(isShieldedLeaf() || !isSupernode())
		        return false;

            if(!allowUltrapeer2LeafConnection(hr))
                return false;

            int leaves = getNumInitializedClientConnections();
            int nonLimeWireLeaves = _nonLimeWireLeaves; // How many non-LimeWire leaves we have

            // Reserve RESERVED_NON_LIMEWIRE_LEAVES slots
            // for non-limewire leaves to ensure that the network
            // is well connected.
            if(!hr.isLimeWire()) {
                if( leaves < UltrapeerSettings.MAX_LEAVES.getValue() &&
                    nonLimeWireLeaves < RESERVED_NON_LIMEWIRE_LEAVES ) {
                    return true;
                }
            }
            
            // Only allow good guys.
            if(!hr.isGoodLeaf())
                return false;

            // if it's good, allow it.
            if(hr.isGoodLeaf())
                return (leaves + Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES -
                        nonLimeWireLeaves)) <
                          UltrapeerSettings.MAX_LEAVES.getValue();

        } else if (hr.isGoodUltrapeer()) {
            // Note that this code is NEVER CALLED when we are a leaf.
            // As a leaf, we will allow however many ultrapeers we happen
            // to connect to.
            // Thus, we only worry about the case we're connecting to
            // another ultrapeer (internally or externally generated)
            
            int peers = getNumInitializedConnections();
            int nonLimeWirePeers = _nonLimeWirePeers; // The number of ultrapeers we're connected to that aren't running LimeWire
            int locale_num = 0;
            
            if(!allowUltrapeer2UltrapeerConnection(hr)) {
                return false;
            }
            
            // If the program is set to connect to remote computers with the same language preference as us
            if (ConnectionSettings.USE_LOCALE_PREF.getValue()) { // True by default
                
                //if locale matches and we haven't satisfied the
                //locale reservation then we force return a true
                if (checkLocale(hr.getLocalePref()) &&
                   _localeMatchingPeers < ConnectionSettings.NUM_LOCALE_PREF.getValue()) { // We have a slot open for a computer that matches our language
                    return true;
                }

                //this number will be used at the end to figure out
                //if the connection should be allowed
                //(the reserved slots is to make sure we have at least
                // NUM_LOCALE_PREF locale connections but we could have more so
                // we get the max)
                locale_num =
                    getNumLimeWireLocalePrefSlots();
            }

            // Reserve RESERVED_NON_LIMEWIRE_PEERS slots
            // for non-limewire peers to ensure that the network
            // is well connected.
            if(!hr.isLimeWire()) {
                double nonLimeRatio = ((double)nonLimeWirePeers) / _preferredConnections;
                
                // If nonLimeRatio is less than 10%, (do)
                if (nonLimeRatio < ConnectionSettings.MIN_NON_LIME_PEERS.getValue()) return true;
                
                // If nonLimeRatio is less than 20%, accept this connection, if it's more than 20%, refuse it
                return (nonLimeRatio < ConnectionSettings.MAX_NON_LIME_PEERS.getValue());
                
            } else {
                
                // Calculate the minimum number of non-LimeWire connections we want to have
                int minNonLime = (int)(ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections);
                
                return (peers + 
                        Math.max(0,minNonLime - nonLimeWirePeers) + 
                        locale_num) < _preferredConnections;
            }
        }
		return false;
    }

    /**
     * Utility method for determining whether or not the connection should be
     * allowed as an Ultrapeer<->Ultrapeer connection.  We may not allow the
     * connection for a variety of reasons, including lack of support for
     * specific features that are vital for good performance, or clients of
     * specific vendors that are leechers or have serious bugs that make them
     * detrimental to the network.
     *
     * @param hr the <tt>HandshakeResponse</tt> instance containing the
     *  connections headers of the remote host
     * @return <tt>true</tt> if the connection should be allowed, otherwise
     *  <tt>false</tt>
     */
    private static boolean allowUltrapeer2UltrapeerConnection(HandshakeResponse hr) {
        if(hr.isLimeWire())
            return true;
        
        String userAgent = hr.getUserAgent();
        if(userAgent == null)
            return false;
        userAgent = userAgent.toLowerCase();
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue(); // Get the list of program names like "morpheus" we want to avoid
        for(int i = 0; i < bad.length; i++)
            if(userAgent.indexOf(bad[i]) != -1)
                return false;
        return true;
    }

    /**
     * Utility method for determining whether or not the connection should be
     * allowed as a leaf when we're an Ultrapeer.
     *
     * @param hr the <tt>HandshakeResponse</tt> containing their connection
     *  headers
     * @return <tt>true</tt> if the connection should be allowed, otherwise
     *  <tt>false</tt>
     */
    private static boolean allowUltrapeer2LeafConnection(HandshakeResponse hr) {
        if(hr.isLimeWire())
            return true;
        
        String userAgent = hr.getUserAgent();
        if(userAgent == null)
            return false;
        userAgent = userAgent.toLowerCase();
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue(); // Get the list of program names like "morpheus" we want to avoid 
        for(int i = 0; i < bad.length; i++)
            if(userAgent.indexOf(bad[i]) != -1)
                return false;
        return true;
    }

    /**
     * Returns the number of connections that are ultrapeer -> ultrapeer.
     * Caller MUST hold this' monitor.
     */
    private int ultrapeerToUltrapeerConnections() {
        //TODO3: augment state of this if needed to avoid loop
        int ret=0;
        for (Iterator iter=_initializedConnections.iterator(); iter.hasNext();){
            ManagedConnection mc=(ManagedConnection)iter.next();
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
            ManagedConnection mc=(ManagedConnection)iter.next();
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
    
    //done

    /**
     * Get the list of computers we're connected to.
     * These are ManagedConnection objects.
     * We opened a TCP socket connection with each one, did the Gnutella handshake, and are now exchanging Gnutella packets.
     * 
     * Don't modify this list, just read it.
     * 
     * @return The _initializedConnections list
     */
    public List getInitializedConnections() {

        // Return the _initializedConnections list
        return _initializedConnections;
    }

    //do

    /**
     * return a list of initialized connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List getInitializedConnectionsMatchLocale(String loc) {
        List matches = new LinkedList();
        for(Iterator itr= _initializedConnections.iterator();
            itr.hasNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }
    
    //done

    /**
     * Get the list of all our leaves.
     * These are the ManagedConnection objects that represent remote computers we've connected to.
     * We did the Gnutella handshake, and are now exchanging packets with them.
     * 
     * Don't change this list, just read it.
     * 
     * @return The _initializedClientConnections list of ManagedConnection objects
     */
    public List getInitializedClientConnections() {

        // Return the list
        return _initializedClientConnections;
    }

    //do

    /**
     * return a list of initialized client connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List getInitializedClientConnectionsMatchLocale(String loc) {
    	List matches = new LinkedList();
        for(Iterator itr= _initializedClientConnections.iterator();
            itr.hasNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }
    
    //done

    /**
     * Get the list that includes all the remote computers we're trying to connect to and are connected to.
     * This is the list of all the ManagedConnection objects.
     * 
     * @return The _connections list of ManagedConnection objects
     */
    public List getConnections() {

        // Return the list
        return _connections;
    }

    //do
    
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

        // If we have some connections up to ultrapeers
        if (isShieldedLeaf()) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for proxy support is cached boolean
            // value
            Iterator ultrapeers = getInitializedConnections().iterator();
            Set proxies = new IpPortSet();
            while (ultrapeers.hasNext() && (proxies.size() < 4)) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.isPushProxy())
                    proxies.add(currMC);
            }
            return proxies;
        }

        return Collections.EMPTY_SET;
    }

    /**
     * Sends a TCPConnectBack request to (up to) 2 connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendTCPConnectBackRequests() {
        int sent = 0;
        
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for (Iterator iter = peers.iterator(); iter.hasNext();) {
            ManagedConnection currMC = (ManagedConnection) iter.next();
            if (currMC.remoteHostSupportsTCPRedirect() < 0)
                iter.remove();
        }
        
        if (peers.size() == 1) {
            ManagedConnection myConn = (ManagedConnection) peers.get(0);
            for (int i = 0; i < CONNECT_BACK_REDUNDANT_REQUESTS; i++) {
                Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
                myConn.send(cb);
                sent++;
            }
        } else {
            final Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
            for(Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) {
                ManagedConnection currMC = (ManagedConnection)i.next();
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends a UDPConnectBack request to (up to) 4 (and at least 2)
     * connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendUDPConnectBackRequests(GUID cbGuid) {
        int sent =  0;
        final Message cb =
            new UDPConnectBackVendorMessage(RouterService.getPort(), cbGuid);
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) {
            ManagedConnection currMC = (ManagedConnection)i.next();
            if (currMC.remoteHostSupportsUDPConnectBack() >= 0) {
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends a QueryStatusResponse message to as many Ultrapeers as possible.
     *
     * @param
     */
    public void updateQueryStatus(QueryStatusResponse stat) {
        
        // If we have some connections up to ultrapeers
        if (isShieldedLeaf()) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for query status response is a cached
            // value
            Iterator ultrapeers = getInitializedConnections().iterator();
            while (ultrapeers.hasNext()) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.remoteHostSupportsLeafGuidance() >= 0)
                    currMC.send(stat);
            }
        }
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
									connection.getPort());
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
        _connections = Collections.unmodifiableList(newConnections);
    }

    /**
     * Adds an incoming connection to the list of connections. Note that
     * the incoming connection has already been initialized before
     * this method is invoked.
     * Should only be called from a thread that has this' monitor.
     * This is called from initializeExternallyGeneratedConnection, for
     * incoming connections
     */
    private void connectionInitializingIncoming(ManagedConnection c) {
        connectionInitializing(c);
    }

    /**
     * Marks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initialization.
     * Should only be called from a thread that has this' monitor.
     */
    private boolean connectionInitialized(ManagedConnection c) {
        if(_connections.contains(c)) {
            // Double-check that we haven't improperly allowed
            // this connection.  It is possible that, because of race-conditions,
            // we may have allowed both a 'Peer' and an 'Ultrapeer', or an 'Ultrapeer'
            // and a leaf.  That'd 'cause undefined results if we allowed it.
            if(!allowInitializedConnection(c)) {
                removeInternal(c);
                return false;
            }
            

            //update the appropriate list of connections
            if(!c.isSupernodeClientConnection()){
                //REPLACE _initializedConnections with the list
                //_initializedConnections+[c]
                List newConnections=new ArrayList(_initializedConnections);
                newConnections.add(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                
                if(c.isClientSupernodeConnection()) {
                	killPeerConnections(); // clean up any extraneus peer conns.
                    _shieldedConnections++; // Count that now we have one more connection up to an ultrapeer
                }
                if(!c.isLimeWire())
                    _nonLimeWirePeers++; // Count we're connected to one more ultrapeer that isn't running LimeWire
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers++; // Count we're connected to one more ultrapeer with the same language preference as us
            } else {
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections+[c]
                List newConnections
                    =new ArrayList(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves++; // Count that we have another leaf that isn't running LimeWire
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
     * like allowConnection, except more strict - if this is a leaf,
     * only allow connections whom we have told we're leafs.
     * @return whether the connection should be allowed 
     */
    private boolean allowInitializedConnection(Connection c) {
        
        // If we have some connections up to ultrapeers, or 
    	if ((isShieldedLeaf() || !isSupernode()) &&
    			!c.isClientSupernodeConnection())
    		return false;
    	
    	return allowConnection(c.headers());
    }
    
    /**
     * removes any supernode->supernode connections
     */
    private void killPeerConnections() {
    	List conns = _initializedConnections;
    	for (Iterator iter = conns.iterator(); iter.hasNext();) {
			ManagedConnection con = (ManagedConnection) iter.next();
			if (con.isSupernodeSupernodeConnection()) 
				removeInternal(con);
		}
    }
    
    /**
     * Iterates over all the connections and sends the updated CapabilitiesVM
     * down every one of them.
     */
    public void sendUpdatedCapabilities() {        
        for(Iterator iter = getInitializedConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();
            c.sendUpdatedCapabilities();
        }
        for(Iterator iter = getInitializedClientConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();
            c.sendUpdatedCapabilities();
        }        
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
        _disconnectTime = System.currentTimeMillis();
        _connectTime = Long.MAX_VALUE;
        _preferredConnections = 0;
        adjustConnectionFetchers(); // kill them all
        //2. Remove all connections.
        for (Iterator iter=getConnections().iterator();
             iter.hasNext(); ) {
            ManagedConnection c=(ManagedConnection)iter.next();
            remove(c);
            //add the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                //add to catcher with the locale info.
                _catcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                          c.getPort()), true, c.getLocalePref());
            }
        }
        
        Sockets.clearAttempts();
    }

    /**
     * Connects to the network.  Ensures the number of messaging connections
     * is non-zero and recontacts the pong server as needed.
     */
    public synchronized void connect() {

        // Reset the disconnect time to be a long time ago.
        _disconnectTime = 0;
        _connectTime = System.currentTimeMillis();

        // Ignore this call if we're already connected
        // or not initialized yet.
        if(isConnected() || _catcher == null) {
            return;
        }
        
        _connectionAttempts = 0;
        _lastConnectionCheck = 0;
        _lastSuccessfulConnect = 0;


        // Notify HostCatcher that we've connected.
        _catcher.expire();
        
        // Set the number of connections we want to maintain
        setPreferredConnections();
        
        // tell the catcher to start pinging people.
        _catcher.sendUDPPings();
    }

    /**
     * Sends the initial ping request to a newly initialized connection.  The
     * ttl of the PingRequest will be 1 if we don't need any connections.
     * Otherwise, the ttl = max ttl.
     */
    private void sendInitialPingRequest(ManagedConnection connection) {
        if(connection.supportsPongCaching()) return;

        //We need to compare how many connections we have to the keep alive to
        //determine whether to send a broadcast ping or a handshake ping,
        //initially.  However, in this case, we can't check the number of
        //connection fetchers currently operating, as that would always then
        //send a handshake ping, since we're always adjusting the connection
        //fetchers to have the difference between keep alive and num of
        //connections.
        PingRequest pr;
        if (getNumInitializedConnections() >= _preferredConnections)
            pr = new PingRequest((byte)1);
        else
            pr = new PingRequest((byte)4);

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
        if(!c.isSupernodeClientConnection()){
            int i=_initializedConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initializedConnections with the list
                //_initializedConnections-[c]
                List newConnections=new ArrayList();
                newConnections.addAll(_initializedConnections);
                newConnections.remove(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections--; // Count that we now have one less connection up to an ultrapeer
                if(!c.isLimeWire())
                    _nonLimeWirePeers--; // Count that we're connected to one less ultrapeer that isn't running LimeWire
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers--; // Count that we're connected to one less ultrapeer with the same language preference as us
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
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves--; // We've got one fewer leaf that isn't running LimeWire
            }
        }

        // 1b) Remove from the all connections list and clean up the
        // stuff associated all connections
        int i=_connections.indexOf(c);
        if (i != -1) {
            //REPLACE _connections with the list _connections-[c]
            List newConnections=new ArrayList(_connections);
            newConnections.remove(c);
            _connections = Collections.unmodifiableList(newConnections);
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
     * Stabilizes connections by removing extraneous ones.
     *
     * This will remove the connections that we've been connected to
     * for the shortest amount of time.
     * 
     * setPreferredConnections() changes the value of _preferredConnections, and then calls this method.
     * 
     * 
     */
    private synchronized void stabilizeConnections() {
        
        // Loop while we're connected to more ultrapeers than we need to be
        while (getNumInitializedConnections() > _preferredConnections) {

            ManagedConnection newest = null;

            for (Iterator i = _initializedConnections.iterator(); i.hasNext(); ) {

                ManagedConnection c = (ManagedConnection)i.next();
                
                // first see if this is a non-limewire connection and cut it off
                // unless it is our only connection left
                
                if (!c.isLimeWire()) {
                    newest = c;
                    break;
                }
                
                if(newest == null || 
                   c.getConnectionTime() > newest.getConnectionTime())
                    newest = c;
            }
            if(newest != null)
                remove(newest);
        }
        adjustConnectionFetchers();
    }    

    /**
     * Starts or stops connection fetchers to maintain the invariant
     * that numConnections + numFetchers >= _preferredConnections
     *
     * _preferredConnections - numConnections - numFetchers is called the need.
     * This method is called whenever the need changes:
     *   1. setPreferredConnections() -- _preferredConnections changes
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
     * 
     * 
     * 
     */
    private void adjustConnectionFetchers() {

        // True, we want to connect to remote computers with the same language preference as us
        if (ConnectionSettings.USE_LOCALE_PREF.getValue()) {

            //if it's a leaf and locale preferencing is on
            //we will create a dedicated preference fetcher
            //that tries to fetch a connection that matches the
            //clients locale
            if(RouterService.isShieldedLeaf()
               && _needPref
               && !_needPrefInterrupterScheduled
               && _dedicatedPrefFetcher == null) {
                _dedicatedPrefFetcher = new ConnectionFetcher(true);
                Runnable interrupted = new Runnable() {
                        public void run() {
                            synchronized(ConnectionManager.this) {
                                // always finish once this runs.
                                _needPref = false;

                                if (_dedicatedPrefFetcher == null)
                                    return;
                                _dedicatedPrefFetcher.interrupt();
                                _dedicatedPrefFetcher = null;
                            }
                        }
                    };
                _needPrefInterrupterScheduled = true;
                // shut off this guy if he didn't have any luck
                RouterService.schedule(interrupted, 15 * 1000, 0);
            }
        }
        int goodConnections = getNumInitializedConnections();
        int neededConnections = _preferredConnections - goodConnections;
        //Now how many fetchers do we need?  To increase parallelism, we
        //allocate 3 fetchers per connection, but no more than 10 fetchers.
        //(Too much parallelism increases chance of simultaneous connects,
        //resulting in too many connections.)  Note that we assume that all
        //connections being fetched right now will become ultrapeers.
        int multiple;

        // The end result of the following logic, assuming _preferredConnections
        // is 32 for Ultrapeers, is:
        // When we have 22 active peer connections, we fetch
        // (27-current)*1 connections.
        // All other times, for Ultrapeers, we will fetch
        // (32-current)*3, up to a maximum of 20.
        // For leaves, assuming they maintin 4 Ultrapeers,
        // we will fetch (4-current)*2 connections.

        // If we have not accepted incoming, fetch 3 times
        // as many connections as we need.
        // We must also check if we're actively being a Ultrapeer because
        // it's possible we may have turned off acceptedIncoming while
        // being an Ultrapeer.
        if( !RouterService.acceptedIncomingConnection() && !isActiveSupernode() ) {
            multiple = 3;
        }
        // Otherwise, if we're not ultrapeer capable,
        // or have not become an Ultrapeer to anyone,
        // also fetch 3 times as many connections as we need.
        // It is critical that active ultrapeers do not use a multiple of 3
        // without reducing neededConnections, otherwise LimeWire would
        // continue connecting and rejecting connections forever.
        else if( !isSupernode() || getNumUltrapeerConnections() == 0 ) {
            multiple = 3;
        }
        // Otherwise (we are actively Ultrapeering to someone)
        // If we needed more than connections, still fetch
        // 2 times as many connections as we need.
        // It is critical that 10 is greater than RESERVED_NON_LIMEWIRE_PEERS,
        // else LimeWire would try connecting and rejecting connections forever.
        else if( neededConnections > 10 ) {
            multiple = 2;
        }
        // Otherwise, if we need less than 10 connections (and we're an Ultrapeer), 
        // decrement the amount of connections we need by 5,
        // leaving 5 slots open for newcomers to use,
        // and decrease the rate at which we fetch to
        // 1 times the amount of connections.
        else {
            multiple = 1;
            neededConnections -= 5 +
            
                // The minimum number of non-LimeWire connections we want to have
                ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections;
        }

        int need = Math.min(10, multiple*neededConnections)
                 - _fetchers.size()
                 - _initializingFetchedConnections.size();

        // do not open more sockets than we can
        need = Math.min(need, Sockets.getNumAllowedSockets());
        
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
    private void initializeFetchedConnection(ManagedConnection mc,
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
            if(fetcher == _dedicatedPrefFetcher)
                _dedicatedPrefFetcher = null;
            else
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
     * Processes the headers received during connection handshake and updates
     * itself with any useful information contained in those headers.
     * Also may change its state based upon the headers.
     * @param headers The headers to be processed
     * @param connection The connection on which we received the headers
     */
    private void processConnectionHeaders(Connection connection) {

    	// TODO:kfaaborg receivedHeaders always returns true
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
                    Integer.parseInt(
                        remoteAddress.substring(colonIndex).trim());
                if(NetworkUtils.isValidPort(port)) {
                	// for incoming connections, set the port based on what it's
                	// connection headers say the listening port is
                    connection.setListeningPort(port);
                }
            } catch(NumberFormatException e){
                // should nothappen though if the other client is well-coded
            }
        }
    }
    
    //done

    /**
     * True if we can safely switch from ultrapeer to leaf mode.
     * We're trying to connect as an ultrapeer, but don't have any of our own leaves yet.
     * 
     * In the handshake with a remote computer, we said "X-Ultrapeer: true" but the remote computer said "X-Ultrapeer-Needed: false".
     * Now, we have to decide what to do.
     * We could either drop down to leaf mode and connect to this computer.
     * Or, we could stick to our plan and connect as an ultrapeer even though this computer says it doesn't want one. (ask)
     * 
     * If allowLeafDemotion() returns true, we'll take the remote computer's advice.
     * We'll drop down to leaf mode and become a leaf under the remote computer.
     * When we connect to new computers, we'll say "X-Ultrapeer: false".
     * We'll still be ultrapeer capable, but we won't try to connect as an ultrapeer for now.
     * 
     * If allowLeafDemotion() returns false, we'll ignore the remote computer's advice.
     * We'll continue with the handshake as an ultrapeer even though it told us it doesn't need any more ultrapeers.
     * We'll greet other remote computers with "X-Ultrapeer: true" also.
     * 
     * @return True if we should drop down to leaf mode.
     *         False if we should keep trying to connect as an ultrapeer.
     */
    public boolean allowLeafDemotion() {

        // Count that we told another computer "X-Ultrapeer: true" and got back "X-Ultrapeer-Needed: false"
		_leafTries++;

        // If settings are forcing us into ultrapeer mode, no, we can't drop down to leaf mode
        if (UltrapeerSettings.FORCE_ULTRAPEER_MODE.getValue()) return false;

        // If we have no connections up to ultrapeers, and at least one fellow ultrapeer next to us or a leaf of our own, no, we can't drop down to leaf mode
        if (isActiveSupernode()) return false;

        // If we passed every test in SupernodeAssigner.setUltrapeerCapable() and we're still ignoring demotion commands, no, we won't drop down to leaf mode
        if (SupernodeAssigner.isTooGoodToPassUp() && // Our Internet speed and online time would make us an excellent ultrapeer, and
            _leafTries < _demotionLimit)             // We've been told to drop down to leaf mode, but not enough times to pay attention to it yet
            return false;                            // No, we can't drop down to leaf mode

        // OK, we'll take the remote computer's advice, become a leaf, and put aside our dreams of being an ultrapeer for now
        return true;
    }

	/**
     * Reconnect to the Gnutella network as an ultrapeer right now.
     * 
     * If SupernodeAssigner.setUltrapeerCapable() finds us too good to pass up, it will make an "UltrapeerAttemptThread" thread that calls this method.
     * Sets _demotionLimit to the given limit and _leafTries to 0.
     * Disconnects from the network, and then connects to tell new remote computers we're an ultrapeer in the handshake.
     * 
     * @param demotionLimit The number of times we'll ignore remote computers telling us to become a leaf before we give up and be a leaf
	 */
	public void tryToBecomeAnUltrapeer(int demotionLimit) {

        // If we're already an ultrapeer, there is nothing more to do
		if (isSupernode()) return;

        // Set member variables we'll use to track this attempt of ours to join the Gnutella network as an ultrapeer
		_demotionLimit = demotionLimit; // This many other computers can tell us to become a leaf, and we'll ignore them
		_leafTries     = 0;             // We'll count how many times that happens in _leafTries

        // Close all our Gnutella connections, and then open new ones telling remote computers "X-Ultrapeer: true"
		disconnect();
		connect();
	}

    //do
    
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

        List hosts = new ArrayList(st.countTokens());
        while(st.hasMoreTokens()){
            String address = st.nextToken().trim();
            try {
                Endpoint e = new Endpoint(address);
                hosts.add(e);
            } catch(IllegalArgumentException iae){
                continue;
            }
        }
        _catcher.add(hosts);        
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
                // We've added a connection, so the need for connections went
                // down.
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

        //If there's not space for the connection, destroy it.
        //It really should have been destroyed earlier, but this is just in case.
        if (!c.isOutgoing() && !allowConnection(c)) {
            //No need to remove, since it hasn't been added to any lists.
            throw new IOException("No space for connection");
        }

        //For incoming connections, add it to the GUI.  For outgoing connections
        //this was done at the top of the method.  See note there.
        if (! c.isOutgoing()) {
            synchronized(this) {
                connectionInitializingIncoming(c);
                // We've added a connection, so the need for connections went
                // down.
                adjustConnectionFetchers();
            }
            RouterService.getCallback().connectionInitializing(c);
        }

        completeConnectionInitialization(c, false);
    }

    /**
     * Performs the steps necessary to complete connection initialization.
     *
     * @param mc the <tt>ManagedConnection</tt> to finish initializing
     * @param fetched Specifies whether or not this connection is was fetched
     *  by a connection fetcher.  If so, this removes that connection from
     *  the list of fetched connections being initialized, keeping the
     *  connection fetcher data in sync
     */
    private void completeConnectionInitialization(ManagedConnection mc,
                                                  boolean fetched) {
        synchronized(this) {
            if(fetched) {
                _initializingFetchedConnections.remove(mc);
            }
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            boolean connectionOpen = connectionInitialized(mc);
            if(connectionOpen) {
                RouterService.getCallback().connectionInitialized(mc);
                setPreferredConnections();
            }
        }
    }

    /**
     * Gets the number of preferred connections to maintain.
     */
    public int getPreferredConnectionCount() {
        return _preferredConnections;
    }

    /**
     * Determines if we're attempting to maintain the idle connection count.
     */
    public boolean isConnectionIdle() {

        // Return true if _perferredConnections is only 1 right now
        return _preferredConnections == ConnectionSettings.IDLE_CONNECTIONS.getValue();
    }

    
    
    /**
     * Sets _preferredConnections to 32 if we're an ultrapeer, or 3 if we're a leaf.
     * 
     * Sets the maximum number of connections we'll maintain.
     * 
     * 
     * 
     * This method is private, and called 3 places from within the ConnectionManager class:
     * ConnectionManager.completeConnectionInitialization() calls this after we've connected a new remote computer.
     * ConnectionManager.initialize() sets up a thread that calls this every second.
     * ConnectionManager.connect() calls this when the program starts trying to connect to the network.
     * 
     * Sets the value of _preferredConnections, and then calls stabilizeConnections().
     * 
     * 
     * 
     */
    private void setPreferredConnections() {

        // If settings don't allow us to make connections when disconnected, and we've recorded a time when we disconnected
        if (!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getValue() && _disconnectTime != 0) {

            // We're disconnected, do nothing and leave now
            return;
        }

        // Save the number of connections we've been trying to maintain up to now
        int oldPreferred = _preferredConnections;

        /* Now, we'll decide how many connections we should maintain. */

        // We're an ultrapeer
        if (isSupernode()) {

            // Set _preferredConnections to 32, as an ultrapeer, we'll try to keep this many connections to other ultrapeers
            _preferredConnections = ConnectionSettings.NUM_CONNECTIONS.getValue();

            /*
             * Where do we configure how many leaves we should have?
             */

        // We're a leaf, and the user has been away for a half hour
        } else if (isIdle()) {

            // Set _preferredConnections to 1, we're a leaf and the user is gone, we just need a single connection up to an ultrapeer
            _preferredConnections = ConnectionSettings.IDLE_CONNECTIONS.getValue();

        // We're a leaf, and the user is at the computer
        } else {

            // Set _preferredConnections to 3, we're a leaf and the user is here, we should have 3 connections up to an ultrapeer
            _preferredConnections = PREFERRED_CONNECTIONS_FOR_LEAF;
        }

        // If we changed the number of connections we should have, call stabilizeConnections() to connect more or disconnect from some.
        if (oldPreferred != _preferredConnections) stabilizeConnections();
    }


    //done

    /**
     * True if the user has been away for a half hour, and we should drop down to 1 ultrapeer connection instead of 3.
     * 
     * @return True if the user has been away for more than a half hour, false if the user is still here
     */
    private boolean isIdle() {

        /*
         * If the operating system we're running on can't tell us how long the user has been gone, getIdleTime() returns 0.
         * This makes it looks like the user is always at the computer.
         */

        // If the user has been away from the computer for a half hour or more, return true
        return SystemUtils.getIdleTime() >= MINIMUM_IDLE_TIME;
    }

    //do


    //
    // End connection list management functions
    //


    //
    // Begin connection launching thread inner classes
    //

    /**
     * This thread does the initialization and the message loop for
     * ManagedConnections created through createConnectionAsynchronously and
     * createConnectionBlocking
     */
    private class OutgoingConnector implements Runnable {
        private final ManagedConnection _connection;
        private final boolean _doInitialization;

        /**
		 * Creates a new <tt>OutgoingConnector</tt> instance that will
		 * attempt to create a connection to the specified host.
		 *
		 * @param connection the host to connect to
         */
        public OutgoingConnector(ManagedConnection connection,
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
            } catch(IOException ignored) {}
        }
    }

	/**
	 * Runs standard calls that should be made whenever a connection is fully
	 * established and should wait for messages.
	 *
	 * @param conn the <tt>ManagedConnection</tt> instance to start
	 * @throws <tt>IOException</tt> if there is an excpetion while looping
	 *  for messages
	 */
	private void startConnection(ManagedConnection conn) throws IOException {
	    Thread.currentThread().setName("MessageLoopingThread");
		if(conn.isGUESSUltrapeer()) {
			QueryUnicaster.instance().addUnicastEndpoint(conn.getInetAddress(),
				conn.getPort());
		}

		// this can throw IOException
		conn.loopForMessages();
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
     * 
     * 
     * 
     * 
     * ConnectionFetcher extends ManagedThread, which extends Thread.
     * So, ConnectionFetcher is a Thread.
     * 
     * 
     * 
     * 
     */
    private class ConnectionFetcher extends ManagedThread {
        
        //set if this connectionfetcher is a preferencing fetcher
        private boolean _pref = false;
        
        /**
         * Tries to add a connection.  Should only be called from a thread
         * that has the enclosing ConnectionManager's monitor.  This method
         * is only called from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            
            this(false);
        }

        public ConnectionFetcher(boolean pref) {
            
            setName("ConnectionFetcher");
            _pref = pref;
            
            // Kick off the thread.
            setDaemon(true);
            start();
        }

        // Try a single connection
        public void managedRun() {
            
            try {
                
                // Wait for an endpoint.
                Endpoint endpoint = null;
                
                do {
                    
                    endpoint = _catcher.getAnEndpoint();
                    
                } while (!IPFilter.instance().allow(endpoint.getAddress()) || isConnectedTo(endpoint.getAddress()) );
                Assert.that(endpoint != null);
                _connectionAttempts++;
                ManagedConnection connection = new ManagedConnection(
                    endpoint.getAddress(), endpoint.getPort());
                //set preferencing
                connection.setLocalePreferencing(_pref);

                // If we've been trying to connect for awhile, check to make
                // sure the user's internet connection is live.  We only do
                // this if we're not already connected, have not made any
                // successful connections recently, and have not checked the
                // user's connection in the last little while or have very
                // few hosts left to try.
                long curTime = System.currentTimeMillis();
                if(!isConnected() &&
                   _connectionAttempts > 40 &&
                   ((curTime-_lastSuccessfulConnect)>4000) &&
                   ((curTime-_lastConnectionCheck)>60*60*1000)) {
                    _connectionAttempts = 0;
                    _lastConnectionCheck = curTime;
                    LOG.debug("checking for live connection");
                    ConnectionChecker.checkForLiveConnection();
                }

                //Try to connect, recording success or failure so HostCatcher
                //can update connection history.  Note that we declare
                //success if we were able to establish the TCP connection
                //but couldn't handshake (NoGnutellaOkException).
                try {
                    initializeFetchedConnection(connection, this);
                    _lastSuccessfulConnect = System.currentTimeMillis();
                    _catcher.doneWithConnect(endpoint, true);
                    if(_pref) // if pref connection succeeded
                        _needPref = false;
                } catch (NoGnutellaOkException e) {
                    _lastSuccessfulConnect = System.currentTimeMillis();
                    if(e.getCode() == HandshakeResponse.LOCALE_NO_MATCH) {
                        //if it failed because of a locale matching issue
                        //readd to hostcatcher??
                        _catcher.add(endpoint, true,
                                     connection.getLocalePref());
                    }
                    else {
                        _catcher.doneWithConnect(endpoint, true);
                        _catcher.putHostOnProbation(endpoint);
                    }
                    throw e;
                } catch (IOException e) {
                    _catcher.doneWithConnect(endpoint, false);
                    _catcher.expireHost(endpoint);
                    throw e;
                }

				startConnection(connection);
            } catch(IOException e) {
            } catch (InterruptedException e) {
                // Externally generated interrupt.
                // The interrupting thread has recorded the
                // death of the fetcher, so just return.
                return;
            } catch(Throwable e) {
                //Internal error!
                ErrorService.error(e);
            }
        }

        public String toString() {
            return "ConnectionFetcher";
        }
	}

    /**
     * This method notifies the connection manager that the user does not have
     * a live connection to the Internet to the best of our determination.
     * In this case, we notify the user with a message and maintain any
     * Gnutella hosts we have already tried instead of discarding them.
     */
    public void noInternetConnection() {

        if(_automaticallyConnecting) {
            // We've already notified the user about their connection and we're
            // alread retrying automatically, so just return.
            return;
        }

        
        // Notify the user that they have no internet connection and that
        // we will automatically retry
        MessageService.showError("NO_INTERNET_RETRYING",
                QuestionsHandler.NO_INTERNET_RETRYING);
        
        // Kill all of the ConnectionFetchers.
        disconnect();
        
        // Try to reconnect in 10 seconds, and then every minute after
        // that.
        RouterService.schedule(new Runnable() {
            public void run() {
                // If the last time the user disconnected is more recent
                // than when we started automatically connecting, just
                // return without trying to connect.  Note that the
                // disconnect time is reset if the user selects to connect.
                if(_automaticConnectTime < _disconnectTime) {
                    return;
                }
                
                if(!RouterService.isConnected()) {
                    // Try to re-connect.  Note this call resets the time
                    // for our last check for a live connection, so we may
                    // hit web servers again to check for a live connection.
                    connect();
                }
            }
        }, 10*1000, 2*60*1000);
        _automaticConnectTime = System.currentTimeMillis();
        _automaticallyConnecting = true;
        

        recoverHosts();
    }

    /**
     * Utility method that tells the host catcher to recover hosts from disk
     * if it doesn't have enough hosts.
     */
    private void recoverHosts() {
        // Notify the HostCatcher that it should keep any hosts it has already
        // used instead of discarding them.
        // The HostCatcher can be null in testing.
        if(_catcher != null && _catcher.getNumHosts() < 100) {
            _catcher.recoverHosts();
        }
    }

    /**
     * Utility method to see if the passed in locale matches
     * that of the local client. As of now, we assume that
     * those clients not advertising locale as english locale
     */
    private boolean checkLocale(String loc) {
        if(loc == null)
            loc = /** assume english if locale is not given... */
                ApplicationSettings.DEFAULT_LOCALE.getValue();
        return ApplicationSettings.LANGUAGE.getValue().equals(loc);
    }

}
