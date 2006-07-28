
// Commented for the Learning branch

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
 * ConnectionManager holds the list of remote computers running Gnutella software that we're connected to.
 * 
 * The list of all ManagedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, accepts incoming connections, and
 * fetches "automatic" outgoing connections as needed.  Creates threads for
 * handling these connections when appropriate.
 *
 * Because this is the only list of all connections, it plays an important role
 * in message broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitializedConnections() methods.  Adding and removing
 * connections is a slower operation.
 *
 * LimeWire follows the following connection strategy:
 * As a leaf, LimeWire will ONLY connect to 'good' Ultrapeers.  The definition
 * of good is constantly changing.  For a current view of 'good', review
 * HandshakeResponse.isGoodUltrapeer().  LimeWire leaves will NOT deny
 * a connection to an ultrapeer even if they've reached their maximum
 * desired number of connections (currently 4).  This means that if 5
 * connections resolve simultaneously, the leaf will remain connected to all 5.
 * 
 * As an Ultrapeer, LimeWire will seek outgoing connections for 5 less than
 * the number of its desired peer slots.  This is done so that newcomers
 * on the network have a better chance of finding an ultrapeer with a slot
 * open.  LimeWire ultrapeers will allow ANY other ultrapeer to connect to it,
 * and to ensure that the network does not become too LimeWire-centric, it
 * reserves 3 slots for non-LimeWire peers.  LimeWire ultrapeers will allow
 * ANY leaf to connect, so long as there are atleast 15 slots open.  Beyond
 * that number, LimeWire will only allow 'good' leaves.  To see what consitutes
 * a good leaf, view HandshakeResponse.isGoodLeaf().  To ensure that the
 * network does not remain too LimeWire-centric, it reserves 3 slots for
 * non-LimeWire leaves.
 *
 * ConnectionManager has methods to get up and downstream bandwidth, but it
 * doesn't quite fit the BandwidthTracker interface.
 */
public class ConnectionManager {

    /**
     * The time when the user disconnected the program from the Gnutella network.
     * 
     * Initialized to -1.
     * connect() sets to 0.
     * disconnect() sets to now.
     */
    private volatile long _disconnectTime = -1;

    /**
     * The time when the program started trying to connect to the Gnutella network.
     * 
     * Initialized to Long.MAX_VALUE.
     * connect() sets _connectTime to now.
     * disconnect() sets _connectTime to Long.MAX_VALUE.
     */
    private volatile long _connectTime = Long.MAX_VALUE;

    /**
     * The time when we realized we had lost our Internet connection, and started trying to connect every 2 minutes.
     * 
     * Initialized to 0.
     * noInternetConnection() sets to now.
     */
    private volatile long _automaticConnectTime = 0;

    /**
     * True if we've lost our Internet connection, and are trying to connect every 2 minutes.
     * 
     * Initialized to false.
     * noInternetConnection() sets to true.
     */
    private volatile boolean _automaticallyConnecting;

    /**
     * The time when we last successfully opened a TCP socket to a remote computer.
     * ConnectionFetcher.managedRun() calls this after initializeFetchedConnection() has the ManagedConnection object connect.
     */
    private volatile long _lastSuccessfulConnect = 0;

    /**
     * The time when we started checking to see if we have a live Internet connection.
     * ConnectionFetcher.managedRun() calls this when it realizes we've had a lot of trouble connecting, and starts the ConnectionChecker.
     */
    private volatile long _lastConnectionCheck = 0;

    /** Counter for the number of connection attempts we've made. */
    private volatile static int _connectionAttempts;

    /** A log that we can write lines of text into as the code here runs. */
    private static final Log LOG = LogFactory.getLog(ConnectionManager.class);

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

    /** 3, if we only have 1 ultrapeer that understands the TCP connect back vendor message, we'll send it 3 of them. */
    public static final int CONNECT_BACK_REDUNDANT_REQUESTS = 3;

    /** If the user leaves the computer for a half hour, we'll drop down to just 1 ultrapeer connection instead of 3. */
    private static final int MINIMUM_IDLE_TIME = 30 * 60 * 1000; // 30 minutes

    /**
     * 2, as an ultrapeer, we'll try to get 2 leaves that aren't running LimeWire.
     * We want some diversity amongst our leaves.
     * If all our leaves were LimeWire, the Gnutella network could become LimeWire-centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_LEAVES = 2;

    /** The number of ultrapeers we should have, 32 if we're an ultrapeer or 3 if we're a leaf. */
    private volatile int _preferredConnections = -1; // Initialize to -1 to indicate the number isn't set yet

    /**
     * The HostCatcher object that keeps the list of IP addresses of remote computers like us running Gnutella software.
     * We'll give it the IP addresses we encounter, and ask it for some to try to connect to.
     */
    private HostCatcher _catcher;

    /**
     * A list of the ConnectionFetcher threads we have running right now.
     * Each one is trying to connect to a single remote Gnutella computer.
     * 
     * Synchronize on this ConnectionManager object before using this list.
     */
    private final List _fetchers = new ArrayList(); // We'll put ConnectionFetcher threads in this ArrayList

    /**
     * A list of ManagedConnection objects that we have ConnectionFetcher threads trying to open.
     * We haven't done the Gnutella handshake with these remote computers yet.
     * 
     * Synchronize on this ConnectionManager object before using this list.
     */
    private final List _initializingFetchedConnections = new ArrayList(); // We'll put ManagedConnection objects in this ArrayList

    /**
     * A ConnectionFetcher thread that will refuse foreign language remote computers in the Gnutella handshake.
     * We'll use _dedicatedPrefFetcher when we're a leaf and need an ultrapeer that matches our language.
     */
    private ConnectionFetcher _dedicatedPrefFetcher;

    /** True until we have a connection to a remote computer with the same language preference as us. */
    private volatile boolean _needPref = true;

    /**
     * True when the RouterService has some code scheduled to kill the language preferenced ConnectionFetcher 15 seconds from now.
     * 
     * adjustConnectionFetchers() may find that we're a leaf with at least one connection up to an ultrapeer, but none that speak our language yet.
     * If it does, it will make a special ConnectionFetcher thread that will refuse foreign language computers in the Gnutella handshake.
     * It will also schedule some code with the RouterService that will kill the thread 15 seconds later.
     */
    private boolean _needPrefInterrupterScheduled = false;

    /*
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

    /*
     * TODO:: why not use sets here??
     */

    /** _connections lists all the ultrapeers we're trying to connect to or are connected to. */
    private volatile List _connections                  = Collections.EMPTY_LIST; // We'll put ManagedConnection objects in this List
    /** _initializedConnections is a list of all the ultrapeers we've finished the Gnutella handshake with. */
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

    /**
     * Schedule a method that will keep _preferredConnections, the number of ultrapeer connections we'll try to get, up to date.
     * 
     * Has the RouterService call setPreferredConnections() every second.
     * This method will set _preferredConnections, the number of ultrapeer connections we will try to get.
     * The value is 32 if we're an ultrapeer, 3 if we're a leaf, or 1 if we're a leaf and the user has been gone for a half hour.
     * 
     * RouterService.start() calls this.
     */
    public void initialize() {

        // Get and save a reference to the HostCatcher the RouterService made when the program started
        _catcher = RouterService.getHostCatcher();

        /*
         * schedule the Runnable that will allow us to change
         * the number of connections we're shooting for if
         * we're idle.
         */

        // If the operating system we're running on can tell us when the user has stepped away from the computer
        if (SystemUtils.supportsIdleTime()) {

            // Have the RouterService run some code every so often for us
            RouterService.schedule(

                // Make a new class right here that doesn't have a name, but is Runnable and has a run() method so a thread can run it
                new Runnable() {

                    // The RouterService will create a thread, and that thread will call this run() method
                    public void run() {

                        // Set _preferredConnections to 32 if we're an ultrapeer, or 3 if we're a leaf, or 1 if we're a leaf and the user has been gone for a half hour
                        setPreferredConnections();
                    }
                },

                // The RouterService will do this one second from now
                1000,

                // After that, it will do it every second
                1000
            );
        }
    }

    /**
     * Connect to an IP address, do the Gnutella handshake, get ready to exchange Gnutella packets, and return a new ManagedConnection object.
     * 
     * This blocks while we're waiting to make the TCP socket connection, and while we're doing the Gnutella handshake.
     * Then, it starts a new thread named "OutgoingConnector" to call startConnection(), which sets up the chain of readers.
     * 
     * Only RouterService.connectToHostBlocking(hostname, portnum) calls this.
     * 
     * @param hostname The IP address of a remote computer on the Internet that may be running Gnutella software right now, like "67.163.131.22"
     * @param portnum  The port number we can contact the remote computer on
     * @return         A new ManagedConnection object that represents the newly connected remote computer
     */
    public ManagedConnection createConnectionBlocking(String hostname, int portnum) throws IOException {

        // Make a new ManagedConnection object from the given IP address and port number
        ManagedConnection c = new ManagedConnection(hostname, portnum);

        // Connect to the IP address, do the Gnutella handshake, setup compression, and send the remote computer a ping packet
        initializeExternallyGeneratedConnection(c); // Does this synchronously, execution blocks here while we're waiting to connect

        // Start a new thread called "OutgoingConnector" that will build the chain of readers
        Thread conn = new ManagedThread(new OutgoingConnector(c, false), "OutgoingConnector"); // False, we've already called initializeExternallyGeneratedConnection(c)
        conn.setDaemon(true); // Let the program exit if this thread is still running
        conn.start(); // Have the thread run OutgoingConnector.run(), which calls startConnection()

        // Return the ManagedConnection object that's connected and ready to send and receive Gnutella packets
        return c;
    }

    /**
     * Connect to an IP address, do the Gnutella handshake, and get ready to exchange Gnutella packets.
     * 
     * This doesn't block.
     * It makes a thread called "OutgoingConnectionThread" which connects to the new computer.
     * 
     * Only RouterService.connectToHostAsynchronously(hostname, portnum) calls this.
     * 
     * @param hostname The IP address of a remote computer on the Internet that may be running Gnutella software right now, like "67.163.131.22"
     * @param portnum  The port number we can contact the remote computer on
     */
    public void createConnectionAsynchronously(String hostname, int portnum) {

        // Make a new ManagedConnection object with the given IP address and port number, and put it in a new OutgoingConnector object
		Runnable outgoingRunner = new OutgoingConnector(new ManagedConnection(hostname, portnum), true); // True, have the thread do the connecting instead of us

        /*
         * Initialize and loop for messages on another thread.
         */

        // Start a new thread called "OutgoingConnectionThread" that will connect a socket, do the Gnutella handshake, and make the chain of readers
		Thread outgoingConnectionRunner = new ManagedThread(outgoingRunner, "OutgoingConnectionThread");
		outgoingConnectionRunner.setDaemon(true); // Let the program exit if this thread is still running
		outgoingConnectionRunner.start(); // Have the thread run OutgoingConnector.run()
    }

    /**
     * Make a ManagedConnection object, add it to our list, do the Gnutella handshake, and get ready to exchange Gnutella packets.
     * 
     * Acceptor.ConnectionDispatchRunner.run() calls this.
     * A remote computer connected to our listening socket, and said "GNUTELLA" first.
     * The Acceptor calls this acceptConnection(socket) method, giving it the new connection socket.
     * This method makes a ManagedConnection object, adds it to our list, does the Gnutella handshake, and makes the chain of readers.
     * 
     * Create an incoming connection.  This method starts the message loop,
     * so it will block for a long time.  Make sure the thread that calls
     * this method is suitable doing a connection message loop.
     * If there are already too many connections in the manager, this method
     * will launch a RejectConnection to send pongs for other hosts.
     * 
     * @param socket A LimeWire NIOSocket object that contains the connection socket Java gave us when the remote computer connected to our listening socket
     */
    void acceptConnection(Socket socket) {

        /*
         * 1. Initialize connection. It's always safe to recommend new headers.
         */

        // Rename this thread "IncommingConnectionThread"
        Thread.currentThread().setName("IncomingConnectionThread");

        // Make a new ManagedConnection object from the given connection socket
        ManagedConnection connection = new ManagedConnection(socket); // Marks the new ManagedConnection as incoming

        try {

            // Do the Gnutella handshake, setup compression, and send the remote computer a ping packet
            initializeExternallyGeneratedConnection(connection); // Won't connect because this ManagedConnection is incoming

        // The remote computer refused us in the Gnutella handshake
        } catch (IOException e) {

            // Close the socket connection to this remote computer, and leave
            connection.close();
            return;
        }

        try {

            // Rename this thread "MessageLoopingThread" and make the chain of readers
            startConnection(connection);
        
        // We lost the socket connection
        } catch (IOException e) {

            /*
             * we could not start the connection for some reason --
             * this can easily happen, for example, if the connection
             * just drops
             */
        }
    }

    /**
     * Close the connection and remove it from our lists.
     * 
     * Removes the given connection from our currently active connections.
     * Removes the connection from routing tables.
     * If we need another connection, starts a new ConnectionFetcher thread.
     * 
     * @param mc The ManagedConnection to close and remove
     */
    public synchronized void remove(ManagedConnection mc) {
        
        // Make sure settings allow us to remove a connection, REMOVE_ENABLED is only false for testing
        if (!ConnectionSettings.REMOVE_ENABLED.getValue()) return;
        
        // Close the connection and remove it from our lists
        removeInternal(mc);
        
        // If we need another connection, start a thread that will get one
        adjustConnectionFetchers();
    }

    /**
     * True if we're sending "X-Ultrapeer: true" or have a fellow ultrapeer connection or a leaf.
     * Returns true if isActiveSupernode() or isSupernodeCapable() is true.
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

    	/*
        //zootella
        return true;
        */

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

    /**
     * True if we're an ultrapeer with a connection down to a leaf.
     * 
     * @return True if we have at least 1 leaf, false if we don't have any leaves
     */
    public boolean hasSupernodeClientConnection() {

        // If we're connected to at least 1 leaf, return true
        return getNumInitializedClientConnections() > 0;
    }

    /**
     * Find out if we have free ultrapeer or leaf slots.
     * If we're a leaf, returns false.
     * 
     * Returns whether or not this node has any available connection
     * slots.  This is only relevant for Ultrapeers -- leaves will
     * always return <tt>false</tt> to this call since they do not
     * accept any incoming connections, at least for now.
     * 
     * @return True if we have open ultrapeer or leaf slots.
     *         False if we have all the ultrapeers and leaves we need.
     *         False if we're a leaf.
     */
    public boolean hasFreeSlots() {

        // Return true if we're an ultrapeer with a slot for an ultrapeer or a leaf still open
        return
            isSupernode() && // If we're greeting new computers with "X-Ultrapeer: true" or have a connection as an ultrapeer, and
            (hasFreeUltrapeerSlots() || hasFreeLeafSlots()); // We have room for more ultrapeer or leaf connections, return true
    }

    /**
     * Find out if we have free ultrapeer slots, and are trying to connect to more ultrapeers.
     * If we're a leaf, returns false.
     * 
     * @return True if we have open ultrapeer slots.
     *         False if we have all the ultrapeers we need.
     *         False if we're a leaf.
     */
    private boolean hasFreeUltrapeerSlots() {

        // If we're trying to connect to some more ultrapeers, return true
        return getNumFreeNonLeafSlots() > 0;
    }

    /**
     * Find out if we have free leaf slots, and are hoping more leaves connect to us.
     * If we're a leaf, returns false.
     * 
     * @return True if we have open leaf slots.
     *         False if we have all the leaves we need.
     *         False if we're a leaf.
     */
    private boolean hasFreeLeafSlots() {

        // If we'll accept connections from some more leaves, return true
        return getNumFreeLeafSlots() > 0;
    }

    /**
     * Find out if we're connecting or connected to a remote computer at a given IP address.
     * 
     * Returns whether this (probably) has a connection to the given host.  This
     * method is currently implemented by iterating through all connections and
     * comparing addresses but not ports.  (Incoming connections use ephemeral
     * ports.)  As a result, this test may conservatively return true even if
     * this is not connected to host.  Likewise, it may it mistakenly
     * return false if host is a multihomed system.  In the future,
     * additional connection headers may make the test more precise.
     * 
     * @param hostName An IP address, like "12.152.83.138"
     * @return         True if we're trying to connect to or are connected to that remote computer, false if we're not
     */
    boolean isConnectedTo(String hostName) {

        /*
         * A clone of the list of all connections, both initialized and
         * uninitialized, leaves and unrouted.  If Java could be prevented from
         * making certain code transformations, it would be safe to replace the
         * call to "getConnections()" with "_connections", thus avoiding a clone.
         * (Remember that _connections is never mutated.)
         */

        // Loop through our list of all the remote computers we're trying to connect to and are connected to
        List connections = getConnections();
        for (Iterator iter = connections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // If this ManagedConnection has the given address, we've found it, report true
            if (mc.getAddress().equals(hostName)) return true;
        }

        // Not found
        return false;
    }

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

    /**
     * The number of Gnutella connections we have to fellow ultrapeers.
     * If we're an ultrapeer, this is the number of connections we have to fellow ultrapeers. (do)
     * If we're a leaf, this is 0. (do)
     * 
     * Loops through _initializedConnections, counting c.isSupernodeSupernodeConnection().
     * Both we and the remote computer said "X-Ultrapeer: true" in the Gnutella handshake.
     * 
     * @return The number of ultrapeer to ultrapeer connections
     */
    public synchronized int getNumUltrapeerConnections() {

        // Loop through _initializedConnections, counting c.isSupernodeSupernodeConnection()
        return ultrapeerToUltrapeerConnections();
    }

    /**
     * The number of Gnutella connections we have to ultrapeers.
     * If we're an ultrapeer, this is the number of connections we have to fellow ultrapeers. (do)
     * If we're a leaf, this is the number of connections we have up to ultrapeers. (do)
     * 
     * Loops through _initializedConnections, counting c.isSupernodeConnection().
     * These are the remote computers that told us "X-Ultrapeer: true" in the Gnutella handshake.
     * 
     * @return The number of ultrapeer to ultrapeer, or leaf to ultrapeer connections
     */
    public synchronized int getNumOldConnections() {

        // Loop through _initializedConnections, counting c.isSupernodeConnection()
        return oldConnections();
    }

    /**
     * How may free leaf slots we have.
     * We hope this many more leaves will connect to us.
     * As an ultrapper, we'd like to have 30 leaves.
     * Returns 30 minus the number of leaves we have.
     * If we're a leaf, returns 0.
     * 
     * @return The number of additional leaves that can connect to us
     */
    public int getNumFreeLeafSlots() {

        // We're greeting new computers with "X-Ultrapeer: true" or have a connection as an ultrapeer
        if (isSupernode()) {

            // Return the number of additional leaves we hope will connect to us
			return UltrapeerSettings.MAX_LEAVES.getValue() - getNumInitializedClientConnections(); // The number of leaves we want, 30, minus the number of leaves we have

        // We're a leaf
        } else {

            // We don't want any leaves to connect to us
            return 0;
        }
    }

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

    /**
     * How many more LimeWire ultrapeers we need.
     * 
     * @return The number of free ultrapeer slots that LimeWires can connect to
     */
    public int getNumFreeLimeWireNonLeafSlots() {

        /*
         * // Find out how many non-LimeWire ultrapeers we should have, like 0.1 * 32 = 3
         * int nonLimeUltrapeerNeed = (int)(ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections);
         * 
         * // Subtract the number of non-LimeWire ultrapeers we already have to get the number of additional non-LimeWire ultrapeers we need
         * int nonLimeUltrapeerSlots = nonLimeUltrapeerNeed - _nonLimeWirePeers;
         * 
         * // nonLimeUltrapeerSlotsOutsideLanguage is the number of additional non-LimeWire ultrapeers we need, not counting those we need that share our language
         * int nonLimeUltrapeerSlotsOutsideLanguage = nonLimeUltrapeerSlots - getNumLimeWireLocalePrefSlots();
         * nonLimeUltrapeerSlotsOutsideLanguage = Math.max(0, nonLimeUltrapeerSlotsOutsideLanguage); // Make sure it's positive
         * 
         * // Starting with the number of ultrapeer slots, subtract the number of additional non-LimeWire ultrapeers we need, not counting those that share our language
         * int limeUltrapeerSlots = getNumFreeNonLeafSlots() - nonLimeUltrapeerSlotsOutsideLanguage;
         * limeUltrapeerSlots = Math.max(0, limeUltrapeerSlots); // Make sure it's positive
         * 
         * // This is how many more LimeWire ultrapeers we need
         * return limeUltrapeerSlots;
         */

        // Calculate how many more LimeWire ultrapeers we need
        return
            Math.max(0, getNumFreeNonLeafSlots() -
            Math.max(0, (int)(ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections) - _nonLimeWirePeers) - getNumLimeWireLocalePrefSlots());
    }

    /**
     * True if we've made a language-matching connection, or never needed one.
     * Returns true if settings tell us not to worry about matching language with connections, or we have some language matching connections anyway.
     * Returns false if settings tell us to match language with connections, and we don't have any.
     * 
     * @return True if we're connected to at least 1 ultrapeer that has the same language preference as us
     */
    public boolean isLocaleMatched() {

        /*
         * _localeMatchingPeers is the number of ultrapeer connections we have to the same language as us.
         */

        // Return true if we're connected to at least 1 ultrapeer that has the same language preference as us
        return !ConnectionSettings.USE_LOCALE_PREF.getValue() || // If the program is not trying to connect to same-language computers, or
               _localeMatchingPeers != 0;                        // We have some connections that match our language, return true
    }

    /**
     * How many more ultrapeers we want that share our language preference.
     * As an ultrapeer, we might have no ultrapeer slots, but still have a slot reserved for a remote computer that matches our language preference.
     * 
     * @return The number of empty ultrapeer slots we've reserved for remote computers with our language preference
     */
    public int getNumLimeWireLocalePrefSlots() {

        /*
         * _localeMatchingPeers is the number of ultrapeer connections we have to the same language as us.
         */

        // Return the number of empty slots we've reserved for remote computers with our language preference
        return Math.max(0, ConnectionSettings.NUM_LOCALE_PREF.getValue() - _localeMatchingPeers);
    }

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

	/**
     * Returns whether or not we are trying to connect to the Gnutella network right now.
     * 
     * If we haven't started trying to connect yet, returns false.
     * If the user has disconnected the program from the network, returns false.
     * If we have a Gnutella connection, returns false.
     * If we have ConnectionFetcher threads or the connections they've opened, return true.
     * 
     * Parts of the GUI call down to this method to show the program's connecting status to the user.
     * 
     * @return True if we're trying to connect to the Gnutella network right now, false if we're not
	 */
	public boolean isConnecting() {

        /*
         * If connect() hasn't run yet, _disconnectTime will be -1.
         * If disconnect() ran, _disconnectTime will be a large number.
         * If either of these things are true, return false, we're not trying to connect right now.
         */

        // connect() hasn't run yet, or disconnect() has, return false, we're not trying to connect right now
	    if (_disconnectTime != 0) return false;

        // We have at least one open connection that we've done the Gnutella handshake through, return false
	    if (isConnected()) return false;

	    synchronized (this) {

            // If we have ConnectionFetcher threads or the connections they've opened, return true
	        return

                // We've got ConnectionFetcher threads trying to open TCP socket connections, or
                _fetchers.size() != 0 ||

                // We've got connections opened by ConnectionFetcher threads that we haven't done the Gnutella handshake through yet
	            _initializingFetchedConnections.size() != 0;
	    }
	}

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

    /**
     * Look at a remote ultrapeer's handshake headers and how many connections we already have to decide if we want to keep this connection, or refuse and disconnect.
     * Does not include the possiblity of the remote ultrapeer becoming a leaf.
     * 
     * Only initializeExternallyGeneratedConnection(c) calls this.
     * 
     * @param c The ManagedConnection object that represents the remote computer, and contains the handshake headers the remote computer sent us
     * @return  True to keep this connection, false to disconnect
     */
    private boolean allowConnection(ManagedConnection c) {

    	// TODO:kfaaborg receivedHeaders always returns true
        if (!c.receivedHeaders()) return false;

        // Call allowConnection(), telling it the remote ultrapeer can't become a leaf
		return allowConnection(c.headers(), false); // Pass false, the remote computer is an ultrapeer
    }

    /**
     * Look at a remote ultrapeer's handshake headers and how many connections we already have to decide if we want to keep this connection, or refuse and disconnect.
     * Includes the possiblity of telling the remote ultrapeer to become a leaf.
     * 
     * Only UltrapeerHandshakeResponder.reject(hr) calls this.
     * 
     * @param hr The headers from the remote computer
     * @return   True to keep this connection, false to disconnect
     */
    public boolean allowConnectionAsLeaf(HandshakeResponse hr) {

    	// Call allowConnection(), telling it the remote computer is a leaf
		return allowConnection(hr, true); // Pass true, the remote computer is a leaf
    }

    /**
     * Look at a remote computer's handshake headers and how many connections we already have to decide if we want to keep this connection, or refuse and disconnect.
     * 
     * @param hr The headers from the remote computer
     * @return   True to keep this connection, false to disconnect
     */
    public boolean allowConnection(HandshakeResponse hr) {

    	// Call allowConnection(), telling it the remote computer's current ultrapeer or leaf status
        return allowConnection(hr, !hr.isUltrapeer()); // Pass true if the remote computer is a leaf, or false if it's an ultrapeer
    }

    /**
     * Checks if we have an available slot of any kind.
     * If we're a leaf on the network, we don't, leaves don't accept connections at all.
     * If we're an ultrapeer and we don't have our 30 leaves yet, returns true.
     * 
     * @return True if we still have an open slot, false if they are all full or we're not accepting incoming connections
     */
    public boolean allowAnyConnection() {

        // If we have some connections up to ultrapeers, no, we don't have any slots
        if (isShieldedLeaf()) return false;

        // Return true if we need more ultrapeers, or we're an ultrapeer and we don't have our 30 leaves yet
        return

            // We need more ultrapeers, or
            getNumInitializedConnections() < _preferredConnections ||

            // We're an ultrapeer, and we don't have our 30 leaves yet
            (isSupernode() && getNumInitializedClientConnections() < UltrapeerSettings.MAX_LEAVES.getValue());
    }

    /**
     * Look at a remote computer's handshake headers and how many connections we already have to decide if we want to keep this connection, or refuse and disconnect.
     * 
     * At this stage of the process, the following things have happened.
     * We've connected to a remote computer, or the remote computer has connected to us.
     * We've completed the Gnutella handshake, or several stages of it are done.
     * Now, this method will help us decide if we want to keep this connection, or refuse and disconnect.
     * 
     * Splits the situation into 3 possiblities:
     * We're a leaf, and the remote computer is an ultrapeer.
     * We're an ultrapeer, and the remote computer is a leaf.
     * We're both ultrapeers.
     * 
     * Considers the following things about the remote computer.
     * If the remote computer is running LimeWire.
     * If the remote computer's language preference matches our own.
     * 
     * Looks at how many slots we have, how many connections we have, and how many slots are open.
     * Decides to keep the connection, or disconnect.
     * 
     * @param hr   All the headers the remote computer told us during the Gnutella handshake, stored in a HandshakeResponse hash table of strings
     * @param leaf True if the remote computer is a leaf, or we should think of it as though it were one
     * @return     True to keep this connection, false to disconnect it
     */
    public boolean allowConnection(HandshakeResponse hr, boolean leaf) {

        // If we're testing the program with connection preferencing off, return true to allow everything
		if (!ConnectionSettings.PREFERENCING_ACTIVE.getValue()) return true;

        // If the remote computer never told us "X-Ultrapeer: true" or "X-Ultrapeer: false", it's a very old Gnutella program, refuse it
		if (!hr.isLeaf() && !hr.isUltrapeer()) return false;

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

        // 50, if 50 of our connection attempts fail, we'll start accepting non-LimeWire remote computers as ultrapeers
		int limeAttempts = ConnectionSettings.LIME_ATTEMPTS.getValue();

        // If the program is disconnected from the Gnutella network, refuse all connections
        if (!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getValue() && // If settings tell us not to make connections when the program is disconnected, and
            _preferredConnections <= 0) {                              // We don't want to maintain any connections right now

            // Refuse this new connection
            return false;

        // We're a leaf
		} else if (isShieldedLeaf() || !isSupernode()) { // We either have some connections up to ultrapeers, or we aren't and can't be an ultrapeer

            // If the new remote computer is also a leaf, refuse it
		    if (!hr.isUltrapeer()) return false; // Two leaves can't connect on the Gnutella network
		    
		    /*
             * (1) We're a leaf, and the remote computer is an ultrapeer.
             */

            // If the remote ultrapeer doesn't support advanced features, or we're just starting to connect and the remote ultrapeer isn't running LimeWire, refuse it
		    if (!hr.isGoodUltrapeer() || // The remote ultrapeer doesn't support advanced features, or
		        (Sockets.getAttempts() < limeAttempts && !hr.isLimeWire())) { // We've connected less than 50 sockets and the remote ultrapeer isn't limewire

                // Refuse the connection, we want to connect to LimeWire at the start
		        return false;

		    // We have fewer connections up to ultrapeers than we want to have
		    } else if (_shieldedConnections < _preferredConnections) {

                // If this ultrapeer has the same language preference as us, turn _needPref off, we don't need to look for a language match anymore
		        if (checkLocale(hr.getLocalePref())) _needPref = false;

                // If the user has been gone for a half hour, we'll only have 1 connection up to an ultrapeer, we need it to be another LimeWire
                if (isIdle()) return hr.isLimeWire(); // If this ultrapeer is LimeWire, keep it, otherwise don't

                // We need another ultrapeer, keep this connection
                return true;

            // We have enough connections up to ultrapeers
            } else {

                /*
                 * if we were still trying to get a locale connection
                 * and this one matches, allow it, 'cause no one else matches.
                 * (we would have turned _needPref off if someone matched.)
                 */

                // We have enough ultrapeers already, but we need one that matches our language and this one does, keep it
                if (_needPref && checkLocale(hr.getLocalePref())) return true;

                // We have enough ultrapeers already, disconnect from this new one
                return false;
            }

        // The remote computer is a leaf
		} else if (hr.isLeaf() || leaf) { // The remote computer said "X-Ultrapeer: false", or we are thinking of it as a leaf because it could become one

		    // If we're a leaf too, refuse the remote computer
		    if (isShieldedLeaf() || !isSupernode()) return false; // Two leaves can't connect on the Gnutella network

            /*
             * (2) We're an ultrapeer, and the remote computer is a leaf.
             */

            // If the remote computer didn't send an "User-Agent" header or said it's running a Gnutella program we're avoiding, disconnect
            if (!allowUltrapeer2LeafConnection(hr)) return false;

            // Find out how many leaves we have, and how many aren't running LimeWire
            int leaves = getNumInitializedClientConnections();
            int nonLimeWireLeaves = _nonLimeWireLeaves; // How many non-LimeWire leaves we have

            /*
             * Reserve RESERVED_NON_LIMEWIRE_LEAVES slots
             * for non-limewire leaves to ensure that the network
             * is well connected.
             */

            // The remote leaf isn't running LimeWire
            if (!hr.isLimeWire()) {

                // If we have less than 30 leaves and fewer than 2 non-LimeWire leaves
                if (leaves < UltrapeerSettings.MAX_LEAVES.getValue() && nonLimeWireLeaves < RESERVED_NON_LIMEWIRE_LEAVES) {

                    // Accept this non-LimeWire leaf, we need at least 2 of them
                    return true;
                }
            }

            /*
             * At this point, either:
             * This is a LimeWire leaf, or
             * This is a non-LimeWire leaf, but we've already got our required 2 of those.
             */

            // If the remote leaf doesn't support advanced Gnutella features, disconnect from it
            if (!hr.isGoodLeaf()) return false;

            // The remote leaf does support advanced Gnutella features
            if (hr.isGoodLeaf()) {

                // If we need another LimeWire leaf, return true, if we don't, return false
                return

                    // The number of leaves we have, plus the number of non-LimeWire leaves we're looking for
                    (leaves + Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES - nonLimeWireLeaves))

                    // If that's less than 30, return true, otherwise return false
                    < UltrapeerSettings.MAX_LEAVES.getValue();
            }

        // The remote computer is an ultrapeer that supports advanced Gnutella features
        } else if (hr.isGoodUltrapeer()) {

            /*
             * Note that this code is NEVER CALLED when we are a leaf.
             * As a leaf, we will allow however many ultrapeers we happen
             * to connect to.
             * Thus, we only worry about the case we're connecting to
             * another ultrapeer (internally or externally generated)
             */

            /*
             * (3) We're both ultrapeers.
             */

            // Find out how many ultrapeers we're already connected to
            int peers = getNumInitializedConnections(); // The number of ultrapeers we're connected to
            int nonLimeWirePeers = _nonLimeWirePeers;   // The number of ultrapeers we're connected to that aren't running LimeWire
            int locale_num = 0;                         // The number of additional ultrapeers we want that share our language preference

            // If the remote computer didn't send an "User-Agent" header or said it's running a Gnutella program we're avoiding, disconnect
            if (!allowUltrapeer2UltrapeerConnection(hr)) return false;

            // If the program is set to connect to remote computers with the same language preference as us
            if (ConnectionSettings.USE_LOCALE_PREF.getValue()) { // True by default

                // If the remote ultrapeer has our language preference and we have a slot for an ultrapeer that has our language preference, keep this connection
                if (checkLocale(hr.getLocalePref()) &&                                      // The remote ultrapeer has our language preference, and
                    _localeMatchingPeers < ConnectionSettings.NUM_LOCALE_PREF.getValue()) { // We have a slot open for a computer that matches our language

                    // Keep this connection
                    return true;
                }

                /*
                 * this number will be used at the end to figure out
                 * if the connection should be allowed
                 * (the reserved slots is to make sure we have at least
                 * NUM_LOCALE_PREF locale connections but we could have more so
                 * we get the max)
                 */

                // Set locale_num to the number of additional ultrapeers we want that share our language preference
                locale_num = getNumLimeWireLocalePrefSlots();
            }

            /*
             * Reserve RESERVED_NON_LIMEWIRE_PEERS slots
             * for non-limewire peers to ensure that the network
             * is well connected.
             */

            // The remote ultrapeer isn't LimeWire
            if (!hr.isLimeWire()) {

                // Calculate what ratio of our ultrapeers aren't running LimeWire
                double nonLimeRatio = ((double)nonLimeWirePeers) / _preferredConnections;

                /*
                 * We'd like between 80% and 90% of our ultrapeers to be running LimeWire.
                 * We trust LimeWire, so we want most of our ultrapeers to be running it.
                 * However, we don't want to only be connected to other LimeWire computers, because the network gains resiliance through software diversity.
                 */

                // If our non-LimeWire ratio is below 10%, yes, we need this non-LimeWire ultrapeer
                if (nonLimeRatio < ConnectionSettings.MIN_NON_LIME_PEERS.getValue()) return true;

                /*
                 * If our non-LimeWire ratio is below 20%, keep this non-LimeWire ultrapeer.
                 * If our non-LimeWire ratio is above 20%, disconnect from this non-LimeWire ultrapeer.
                 */

                // If our non-LimeWire ratio is below 20%, keep this non-LimeWire ultrapeer, if it's above 20%, disconnect from this non-LimeWire ultrapeer
                return (nonLimeRatio < ConnectionSettings.MAX_NON_LIME_PEERS.getValue());

                // TODO:kfaaborg Here, only MAX_NON_LIME_PEERS changes anything.

            // This is a remote LimeWire ultrapeer we've finished the Gnutella handshake with
            } else {

                // We want to have at least 3 non-LimeWire ultrapeers, which is 10% of 32
                int minNonLime = (int)(ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections);

                /*
                 * // Calculate how many more non-LimeWire ultrapeers we need
                 * int nonLimeUltrapeerNeed = minNonLime - nonLimeWirePeers; // 3 minus the number of non-LimeWire ultrapeers we have
                 * nonLimeUltrapeerNeed = Math.max(0, nonLimeUltrapeerNeed); // Make sure it's positive
                 * 
                 * // To that, add the number of ultrapeers we have, and the number we need that share our language preference
                 * // This is the number of other ultrapeers we're going to have, when we get everything we want
                 * int futureUltrapeers = peers + nonLimeUltrapeerNeed + locale_num;
                 * 
                 * // When we have all those special connections, if we still won't have enough, keep this one
                 * if (futureUltrapeers < _preferredConnections) return true;
                 * else return false; // When we have all those special connections, we'll have reached our maximum, disconnect from this LimeWire ultrapeer
                 */

                // If we won't have room for this LimeWire ultrapeer when we have all the special connections we need, disconnect from it
                return (peers + Math.max(0, minNonLime - nonLimeWirePeers) + locale_num) < _preferredConnections;
            }
        }

        // Somehow, some other combination happened, disconnect from this remote computer
		return false;
    }

    /**
     * Looks at the remote computer's "User-Agent" header to see if we should disconnect.
     * Only allowConnection() above calls this method.
     * 
     * @param hr All the headers the remote computer told us during the Gnutella handshake, stored in a HandshakeResponse hash table of strings
     * @return   True to keep this connection, false to disconnect it
     */
    private static boolean allowUltrapeer2UltrapeerConnection(HandshakeResponse hr) {

        // TODO:kfaaborg The allowUltrapeer2UltrapeerConnection() and allowUltrapeer2LeafConnection() methods are exactly the same.

        // If the remote computer is running LimeWire, keep our connection to it
        if (hr.isLimeWire()) return true;

        // Find the name of the Gnutella program the remote computer is running, the value of the "User-Agent" header it sent us
        String userAgent = hr.getUserAgent();
        if (userAgent == null) return false; // If the remote computer didn't tell us what program it's running, disconnect

        // If the remote computer is running a program we want to avoid, disconnect
        userAgent = userAgent.toLowerCase(); // Make the string lowercase so we can match it with our list of bad programs
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue(); // Get the list of program names like "morpheus" we want to avoid, and loop through them
        for (int i = 0; i < bad.length; i++) {

            // If the "User-Agent" value contains the name of a bad client, refuse it
            if (userAgent.indexOf(bad[i]) != -1) return false;
        }

        // The "User-Agent" text looks OK, stay connected
        return true;
    }

    /**
     * Looks at the remote computer's "User-Agent" header to see if we should disconnect.
     * Only allowConnection() above calls this method.
     * 
     * @param hr All the headers the remote computer told us during the Gnutella handshake, stored in a HandshakeResponse hash table of strings
     * @return   True to keep this connection, false to disconnect it
     */
    private static boolean allowUltrapeer2LeafConnection(HandshakeResponse hr) {

        // TODO:kfaaborg The allowUltrapeer2UltrapeerConnection() and allowUltrapeer2LeafConnection() methods are exactly the same.

        // If the remote computer is running LimeWire, keep our connection to it
        if (hr.isLimeWire()) return true;

        // Find the name of the Gnutella program the remote computer is running, the value of the "User-Agent" header it sent us
        String userAgent = hr.getUserAgent();
        if (userAgent == null) return false; // If the remote computer didn't tell us what program it's running, disconnect

        // If the remote computer is running a program we want to avoid, disconnect
        userAgent = userAgent.toLowerCase(); // Make the string lowercase so we can match it with our list of bad programs
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue(); // Get the list of program names like "morpheus" we want to avoid, and loop through them
        for (int i = 0; i < bad.length; i++) {

            // If the "User-Agent" value contains the name of a bad client, refuse it
            if (userAgent.indexOf(bad[i]) != -1) return false;
        }

        // The "User-Agent" text looks OK, stay connected
        return true;
    }

    /**
     * The number of Gnutella connections we have to fellow ultrapeers.
     * If we're an ultrapeer, this is the number of connections we have to fellow ultrapeers. (do)
     * If we're a leaf, this is 0. (do)
     * 
     * Loops through _initializedConnections, counting c.isSupernodeSupernodeConnection().
     * Both we and the remote computer said "X-Ultrapeer: true" in the Gnutella handshake.
     * 
     * Synchronize on the ConnectionManager object before calling this method.
     * 
     * @return The number of ultrapeer to ultrapeer connections
     */
    private int ultrapeerToUltrapeerConnections() {

        /*
         * TODO3: augment state of this if needed to avoid loop
         */

        // Loop through all the ultrapeers we have Gnutella connections to
        int ret = 0; // Start the count at 0
        for (Iterator iter = _initializedConnections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // If both we and this remote computer are ultrapeers, increment our count
            if (mc.isSupernodeSupernodeConnection()) ret++;
        }

        // Return the total count
        return ret;
    }

    /**
     * The number of Gnutella connections we have to ultrapeers.
     * If we're an ultrapeer, this is the number of connections we have to fellow ultrapeers. (do)
     * If we're a leaf, this is the number of connections we have up to ultrapeers. (do)
     * 
     * Loops through _initializedConnections, counting c.isSupernodeConnection().
     * These are the remote computers that told us "X-Ultrapeer: true" in the Gnutella handshake.
     * 
     * Synchronize on the ConnectionManager object before calling this method.
     * 
     * @return The number of ultrapeer to ultrapeer, or leaf to ultrapeer connections
     */
    private int oldConnections() {

        /*
         * technically, we can allow old connections.
         */

        // Loop through all the ultrapeers we have Gnutella connections to
		int ret = 0; // Start the count at 0
        for (Iterator iter = _initializedConnections.iterator(); iter.hasNext(); ) {
            ManagedConnection mc = (ManagedConnection)iter.next();

            // If both we and this remote computer are ultrapeers, increment our count
            if (!mc.isSupernodeConnection()) ret++;
        }

        // Return the total count
        return ret;
    }

    /**
     * Determines if we think there should be more ultrapeers on the Gnutella network from our view of it.
     * Only use this method if we're an ultrapeer, as only then will we have the information we need to make an informed decision.
     * 
     * As an ultrapeer, we have 30 leaf slots.
     * If those slots are 90% full, we'll have 27 leaves.
     * If those slots are that full or more, supernodeNeeded() returns true.
     * From our perspective, it looks like there are too many leaves on the network and not enough ultrapeers to serve them.
     * 
     * @return True if the network looks like it needs more ultrapeers, false if it looks balanced correctly
     */
    public boolean supernodeNeeded() {

        // If more than 90% of our leaf slots are full, return true, the network needs more ultrapeers
		if (getNumInitializedClientConnections() >=            // If we have more leaves than
            (UltrapeerSettings.MAX_LEAVES.getValue() * 0.9)) { // 90% of our 30 leaf capacity

            // Yes, from here, it looks like the Gnutella network needs more ultrapeers
            return true;

        // We have fewer than 27 leaves filling our 30 leaf slots
        } else {

            // No, from here, it looks like the number of ultrapeers on the Gnutella network is balanced correctly
            return false;
        }
    }

    /**
     * Get the list of the ultrapeers we're connected to.
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

    /**
     * Get a list of the ultrapeers we're connected to that have a given language preference.
     * 
     * @param loc A language preference, like "en" for English
     * @return    A LinkedList of connected ultrapeers that have that language preference
     */
    public List getInitializedConnectionsMatchLocale(String loc) {

        // Loop through the ultrapeers we've finished the Gnutella handshake with
        List matches = new LinkedList(); // Make a new empty list named matches
        for (Iterator itr = _initializedConnections.iterator(); itr.hasNext(); ) {
            Connection conn = (Connection)itr.next();

            // If this ultrapeer's language preference matches the given one, add it to the list
            if (loc.equals(conn.getLocalePref())) matches.add(conn);
        }

        // Return the list we made of connected ultrapeers with the given language preference
        return matches;
    }

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

    /**
     * Get a list of our leaves that have a given language preference.
     * 
     * @param loc A language preference, like "en" for English
     * @return    A LinkedList of connected leaves that have that language preference
     */
    public List getInitializedClientConnectionsMatchLocale(String loc) {

        // Loop through the leaves we've finished the Gnutella handshake with
    	List matches = new LinkedList(); // Make a new empty list named matches
        for (Iterator itr = _initializedClientConnections.iterator(); itr.hasNext(); ) {
            Connection conn = (Connection)itr.next();

            // If this leaf's language preference matches the given one, add it to the list
            if (loc.equals(conn.getLocalePref())) matches.add(conn);
        }

        // Return the list we made of connected leaves with the given language preference
        return matches;
    }

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

    /**
     * Get a list of up to 4 of our push proxies, ultrapeers we're connected up to that sent us a PushProxyAcknowledgement vendor message.
     * 
     * Accessor for the Set of push proxies for this node.  If
     * there are no push proxies available, or if this node is an Ultrapeer,
     * this will return an empty Set.
     * 
     * TODO: should the set of pushproxy UPs be cached and updated as
     * connections are killed and created?
     * 
     * @return A Set of up to 4 ultrapeers we're connected up to that are push proxies
     */
    public Set getPushProxies() {

        // If we're a leaf
        if (isShieldedLeaf()) { // If we have some connections up to ultrapeers

            /*
             * this should be fast since leaves don't maintain a lot of
             * connections and the test for proxy support is cached boolean
             * value
             */

            // Loop through the ultrapeers we're connected up to
            Iterator ultrapeers = getInitializedConnections().iterator();
            Set proxies = new IpPortSet(); // Make a new IpPortSet list that will keep objects that implement the IpPort interface in sorted order with no duplicates
            while (ultrapeers.hasNext() && (proxies.size() < 4)) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();

                // If this ultrapeer sent a PushProxyAcknowledgement vendor message that makes us a push proxy for somebody (do), add it to our list
                if (currMC.isPushProxy()) proxies.add(currMC);
            }

            // Return the list
            return proxies;
        }

        // We're not connected to the Gnutella network as a leaf, return a reference to an empty list
        return Collections.EMPTY_SET;
    }

    /**
     * Send a TCP connect back request to up to 4 of the ultrapeers we're connected to.
     * 
     * @return True if we sent a TCP connect back message to at least 1 ultrapeer, false if we didn't send anything to anyone
     */
    public boolean sendTCPConnectBackRequests() {

        // Count how many TCP connect back requests we send
        int sent = 0;

        // Make a copy of the list of ultrapeers we're connected to, shuffle it into random order, and loop through it
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for (Iterator iter = peers.iterator(); iter.hasNext(); ) {
            ManagedConnection currMC = (ManagedConnection) iter.next();

            // If this ultrapeer doesn't support the TCP connect back redirect vendor message, remove it from the list
            if (currMC.remoteHostSupportsTCPRedirect() < 0) iter.remove();
        }

        // We only found one ultrapeer that supports the TCP connect back message
        if (peers.size() == 1) {

            // Get it
            ManagedConnection myConn = (ManagedConnection)peers.get(0); // It's at index 0, the start of the list

            // Loop 3 times
            for (int i = 0; i < CONNECT_BACK_REDUNDANT_REQUESTS; i++) {

                // Make a new TCP connect back message with the port number the ultrapeer can reach us at, and send it
                Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
                myConn.send(cb);
                sent++; // Count one more sent
            }

        // The list is empty, or contains 2 or more ultrapeers
        } else {

            // Make a new TCP connect back message with the port number the ultrapeer can reach us at
            final Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());

            // Loop through the first 4 ultrapeers we found that can understand this message
            for (Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) {
                ManagedConnection currMC = (ManagedConnection)i.next();

                // Send the ultrapeer the TCP connect back message
                currMC.send(cb);
                sent++; // Count one more sent
            }
        }

        // Return true if we sent the TCP connect back message to at least 1 ultrapeer
        return (sent > 0);
    }

    /**
     * Send a UDP connect back request to up to 4 of the ultrapeers we're connected to.
     * 
     * @return True if we sent a UDP connect back message to at least 1 ultrapeer, false if we didn't send anything to anyone
     */
    public boolean sendUDPConnectBackRequests(GUID cbGuid) {

        // Count how many UDP connect back requests we send
        int sent = 0;

        // Make a UDP Gnutella connect back vendor message with our external listening port number and the given GUID
        final Message cb = new UDPConnectBackVendorMessage(RouterService.getPort(), cbGuid);

        // Make a copy of the list of ultrapeers we're connected to, shuffle it into random order, and loop through it
        List peers = new ArrayList(getInitializedConnections());
        Collections.shuffle(peers);
        for (Iterator i = peers.iterator(); i.hasNext() && sent < 5; ) { // Stop after sending the message to 4 ultrapeers
            ManagedConnection currMC = (ManagedConnection)i.next();

            // If this ultrapeer supports the UDP connect back redirect vendor message
            if (currMC.remoteHostSupportsUDPConnectBack() >= 0) {

                // Send it the UDP connect back message
                currMC.send(cb);
                sent++; // Count one more sent
            }
        }

        // Return true if we sent the UDP connect back message to at least 1 ultrapeer
        return (sent > 0);
    }

    /**
     * Send a given QueryStatusResponse packet to all the ultrapeers we're connected up to that support the leaf guidance vendor message.
     * Only does something if we're a leaf with connections up to ultrapeers.
     * Only sends the QueryStatusResponse packet to the ultrapeers that support the leaf guidance vendor message.
     * 
     * Only code in the SearchResultHandler class calls this method.
     * 
     * @param stat A QueryStatusResponse packet
     */
    public void updateQueryStatus(QueryStatusResponse stat) {

        // Only do something if we're a leaf
        if (isShieldedLeaf()) { // We have some connections up to ultrapeers

            /*
             * this should be fast since leaves don't maintain a lot of
             * connections and the test for query status response is a cached
             * value
             */

            // Loop for each ultrapeer we're connected up to
            Iterator ultrapeers = getInitializedConnections().iterator();
            while (ultrapeers.hasNext()) {
                ManagedConnection currMC = (ManagedConnection)ultrapeers.next();

                // If this remote computer supports the leaf guidance vendor message, send it the given QueryStatusResponse packet
                if (currMC.remoteHostSupportsLeafGuidance() >= 0) currMC.send(stat);
            }
        }
    }

    /**
     * Get the IP address and port number of an ultrapeer we're connected to that supports GUESS.
     * We have a TCP socket connection to this remote computer.
     * 
     * @return The IP address and port number in a new Endpoint object.
     *         null if we aren't connected to any GUESS ultrapeers.
     */
    public Endpoint getConnectedGUESSUltrapeer() {

        // Loop through the ultrapeers we're connected to
        for (Iterator iter = _initializedConnections.iterator(); iter.hasNext(); ) {
            ManagedConnection connection = (ManagedConnection)iter.next();

            // The remote computer said "X-Ultrapeer: true" and "X-GUESS: 0.1" or higher
            if (connection.isSupernodeConnection() && connection.isGUESSUltrapeer()) {

                // Return this remote computer's IP address and port number in a new Endpoint object
                return new Endpoint(connection.getInetAddress().getAddress(), connection.getPort());
            }
        }

        // We're not connected to any ultrapeers that support GUESS
        return null;
    }

    /**
     * Get a list of the ultrapeers we're connected to that support GUESS.
     * We have TCP socket connections to these remote computers.
     * 
     * @return A List of ManagedConnection objects that are GUESS ultrapeers.
     *         If we're not connected to any, returns an empty List, not null.
     */
    public List getConnectedGUESSUltrapeers() {

        // Loop through the ultrapeers we're connected to
        List retList = new ArrayList(); // We'll return this list
        for (Iterator iter = _initializedConnections.iterator(); iter.hasNext(); ) {
            ManagedConnection connection = (ManagedConnection)iter.next();

            // The remote computer said "X-Ultrapeer: true" and "X-GUESS: 0.1" or higher
            if (connection.isSupernodeConnection() && connection.isGUESSUltrapeer()) {

                // Add this GUESS ultrapeer to our list
                retList.add(connection);
            }
        }

        // Return the list we made of all the GUESS ultrapeers we're connected to
        return retList;
    }

    /**
     * Add c to the _connections list.
     * c is a ManagedConnection object you just made from an IP address and port number.
     * After calling connectionInitializing, you'll have it try to open a TCP socket connection to the remote computer.
     * 
     * The _connections list is just for ultrapeers.
     * c must be an ultrapeer, not a leaf.
     * 
     * This method isn't synchronized, call it when you are already inside a synchronized block.
     * 
     * @param c A ManagedConnection object to add to the _connections list
     */
    private void connectionInitializing(Connection c) {

        // Add c to the _connections list
        List newConnections = new ArrayList(_connections);           // Copy the list
        newConnections.add(c);                                       // Add c to the copy
        _connections = Collections.unmodifiableList(newConnections); // Replace the list with the new copy
    }

    /**
     * Add c to the _connections list.
     * c is a ManagedConnection object for an incoming connection.
     * It's initialized, and the socket inside it is already connected.
     * 
     * The _connections list is just for ultrapeers.
     * c must be an ultrapeer, not a leaf.
     * 
     * This method isn't synchronized, call it when you are already inside a synchronized block.
     * 
     * @param c A ManagedConnection object to add to the _connections list
     */
    private void connectionInitializingIncoming(ManagedConnection c) {

        // Add c to the _connections list
        connectionInitializing(c);
    }

    /**
     * Add a newly connected computer to _initializedConnections or _initializedClientConnections, and send it a ping packet.
     * 
     * Makes sure we have a slot for this kind of remote computer.
     * Adds the ManagedConnection object to _initializedConnections if it's an ultrapeer or _initializedClientConnections if it's a leaf.
     * Sends the remote computer vendor specific packets and a ping.
     * 
     * Only ConnectionManager.completeConnectionInitialization() calls this.
     * Call from a thread that is synchronized on this ConnectionManager object.
     * 
     * @param c The ManagedConnection object that represents a remote computer we've finished the Gnutella handshake with
     * @return  True if we decided to keep it and sent it a ping packet, false if we chose to disconnect
     */
    private boolean connectionInitialized(ManagedConnection c) {

        // Only do something if the given ManagedConnection is already in the _connections list
        if (_connections.contains(c)) {

            /*
             * Double-check that we haven't improperly allowed
             * this connection.  It is possible that, because of race-conditions,
             * we may have allowed both a 'Peer' and an 'Ultrapeer', or an 'Ultrapeer'
             * and a leaf.  That'd 'cause undefined results if we allowed it.
             */

            // If we don't have a slot for this kind of remote computer
            if (!allowInitializedConnection(c)) {

                // Close the connection and remove it from our lists
                removeInternal(c);
                return false;
            }

            /*
             * Update the appropriate list of connections
             */

            // We're a leaf and the remote computer is an ultrapeer, or we're both ultrapeers
            if (!c.isSupernodeClientConnection()) {

                // Add c to _initializedConnections, our list of all the ultrapeers we've finished the Gnutella handshake with
                List newConnections = new ArrayList(_initializedConnections);
                newConnections.add(c);
                _initializedConnections = Collections.unmodifiableList(newConnections);

                // The remote computer is an ultrapeer and we are just a leaf
                if (c.isClientSupernodeConnection()) {

                    // Disconnect from any ultrapeers that we connected to as a fellow ultrapeer
                	killPeerConnections(); // Without doing this, we'd be a leaf to some computers and an ultrapeer to others, which is not allowed

                	// Count that now we have one more connection up to an ultrapeer as a leaf
                    _shieldedConnections++;
                }

                // Add to our counts
                if (!c.isLimeWire()) _nonLimeWirePeers++; // Count we're connected to one more ultrapeer that isn't running LimeWire
                if (checkLocale(c.getLocalePref())) _localeMatchingPeers++; // Count we're connected to one more ultrapeer with the same language preference as us

            // We're an ultrapeer and the remote computer is a leaf
            } else {

                // Add c to _initializedClientConnections, the list of our leaves we've finished the Gnutella handshake with
                List newConnections = new ArrayList(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections = Collections.unmodifiableList(newConnections);

                // Add to our counts
                if (!c.isLimeWire()) _nonLimeWireLeaves++; // Count that we have another leaf that isn't running LimeWire
            }

            // Send the remote computer vendor-specific messages that list which other vendor-specifc messages we understand
	        c.postInit();

	        // Send the remote computer a Gnutella ping packet
    		sendInitialPingRequest(c);
            
            // We kept the remote computer and sent it some packets
            return true;
        }

        // The given connection isn't in the _connections list
        return false;
    }

    /**
     * Look at a remote computer's handshake headers and how many connections we already have to decide if we want to keep this connection, or refuse and disconnect.
     * 
     * Calls allowConnection() to look at the headers and slots and choose keep or disconnect.
     * Before that, adds an additional check.
     * If we're a leaf but we didn't tell the remote computer that, disconnect.
     * 
     * @param c A ManagedConnection we've finished the Gnutella handshake with
     * @return  True to keep this connection, false to disconnect
     */
    private boolean allowInitializedConnection(Connection c) {

        // If we're a leaf but we didn't tell the remote computer that, disconnect
    	if (

            // We have some connections up to ultrapeers, or we're not an ultrapeer nor are we trying to become one
            (isShieldedLeaf() || !isSupernode()) &&

            // And, the remote computer is an ultrapeer and we're just a leaf
    		!c.isClientSupernodeConnection())

            // No, disconnect from this remote computer
    		return false;

        // Look at the remote computer's handshake headers and consider how many slots we have open to decide if we want to disconnect
    	return allowConnection(c.headers());
    }

    /**
     * Remove any ultrapeer-to-ultrapeer connections that we may have right now.
     * Disconnect from the ultrapeers we've connected to as a fellow ultrapeer ourselves.
     * 
     * Only connectionInitialized() calls this.
     * The remote computer is an ultrapeer, and we are just a leaf.
     * As a leaf, it's important that we don't have any ultrapeer-to-ultrapeer connections.
     * If we had any, we'd be an ultrapeer to some computers and a leaf to others.
     * This is not allowed on the Gnutella network.
     */
    private void killPeerConnections() {

        // Loop through _initializedConnections, our list of all the ultrapeers we've finished the Gnutella handshake with
    	List conns = _initializedConnections;
    	for (Iterator iter = conns.iterator(); iter.hasNext();) {
			ManagedConnection con = (ManagedConnection) iter.next();

            // If we connected to this ultrapeer as an ultrapeer
			if (con.isSupernodeSupernodeConnection()) {

			    // Close the connection and remove it from our lists
			    removeInternal(con);
            }
		}
    }

    /**
     * Sends our current CapabiltiesVM vendor message to all the Gnutella computers we're connected to.
     * Loops through or list of connected ultrapeers, and then our list of leaves.
     * Use after updating our CapabilitiesVM message.
     */
    public void sendUpdatedCapabilities() {

        // Loop for each ultrapeer we're connected to
        for (Iterator iter = getInitializedConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();

            // Send the ultrapeer our current CapabilitiesVM vendor message
            c.sendUpdatedCapabilities();
        }

        // Loop for each of our leaves
        for (Iterator iter = getInitializedClientConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();

            // Send the leaf our current CapabilitiesVM vendor message
            c.sendUpdatedCapabilities();
        }
    }

    /**
     * Disconnect the program from the Gnutella network.
     * Closes all connections in the _connections list and sets the number of connections we want to 0.
     */
    public synchronized void disconnect() {

        // Record the time now that we're disconnecting
        _disconnectTime = System.currentTimeMillis();
        _connectTime = Long.MAX_VALUE; // Clear the time we tried to connect

        // With _preferredConnections set to 0, adjustConnectionFetchers() will stop all the ConnectionFetcher threads and close the sockets they've opened
        _preferredConnections = 0;
        adjustConnectionFetchers(); // Closes the connections we've opened but haven't done the Gnutella handshake through yet

        // Loop for each ultrapeer connection in the _connections list
        for (Iterator iter = getConnections().iterator(); iter.hasNext(); ) {
            ManagedConnection c = (ManagedConnection)iter.next();

            // Close the connection and remove it from our lists
            remove(c);

            // If this computer was an ultrapeer we were connected to
            if (c.isSupernodeConnection()) {

                // Give its IP address to the HostCatcher
                _catcher.add(
                    new Endpoint(c.getInetAddress().getHostAddress(), c.getPort()), // Make a new Endpoint from the IP address and port number
                    true,                                                           // List with GOOD_PRIORITY because this is an ultrapeer
                    c.getLocalePref());                                             // Include the remote computer's language preference in the listing
            }
        }

        // Have the Sockets class reset its count of connection attempts back to 0
        Sockets.clearAttempts();
    }

    /**
     * Connect the program to the Gnutella network.
     * 
     * Sets _preferredConnections, the number of ultrapers we'll try to get.
     * Has the HostCatcher start sending UDP Gnutella ping packets to the IP addresses of PCs in its list.
     */
    public synchronized void connect() {

        // Clear the time we disconnected, and set the time we connected to now
        _disconnectTime = 0;
        _connectTime = System.currentTimeMillis();

        // If we already have a Gnutella connection or the HostCatcher hasn't been setup yet, leave now
        if (isConnected() || _catcher == null) return;

        // Start counts at 0
        _connectionAttempts    = 0; // Italic in Eclipse because it is static
        _lastConnectionCheck   = 0;
        _lastSuccessfulConnect = 0;

        // Have the HostCatcher read in the gnutella.net file
        _catcher.expire();

        // Set the number of connections we want to maintain
        setPreferredConnections();

        // Have the HostCatcher start sending UDP Gnutella ping packets to the IP addresses of PCs in its list
        _catcher.sendUDPPings();
    }

    /**
     * Send the newly connected computer a Gnutella ping packet.
     * If the computer said "Pong-Caching: 0.1" in the handshake, doesn't send anything.
     * 
     * If we don't need any more connections, we'll send the ping with a TTL of 1.
     * If we do need more connections, we'll set the TTL to 4.
     * 
     * Only connectionInitialize() calls this.
     * 
     * @param connection The ManagedConnection object for a remote computer we've just finished the Gnutella handshake with
     */
    private void sendInitialPingRequest(ManagedConnection connection) {

        // If the remote computer said "Pong-Caching: 0.1" or later, leave now
        if (connection.supportsPongCaching()) return; // With pong caching, we don't need pings anymore

        /*
         * We need to compare how many connections we have to the keep alive to
         * determine whether to send a broadcast ping or a handshake ping,
         * initially.  However, in this case, we can't check the number of
         * connection fetchers currently operating, as that would always then
         * send a handshake ping, since we're always adjusting the connection
         * fetchers to have the difference between keep alive and num of
         * connections.
         */

        // We have all the ultrapeer connections we need
        PingRequest pr;
        if (getNumInitializedConnections() >= _preferredConnections) {

            // Prepare a ping packet with a TTL of 1
            pr = new PingRequest((byte)1); // Handshake ping

        // We are still looking for more ultrapeers to connect to
        } else {

            // Prepare a ping packet with a TTL of 4
            pr = new PingRequest((byte)4); // Broadcast ping, it will travel further so we'll find out about more ultrapeers to connect to
        }

        // Send this remote computer the PingRequest packet
        connection.send(pr);
        try { connection.flush(); } catch (IOException e) {} // Actually send it out now
    }

    /**
     * Close a ManagedConnection object and remove it from the lists the ConnectionManager keeps.
     * 
     * Removes c from _initializedConnections if it's an ultrapeer, or _initializedClientConnections if it's a leaf.
     * Removes c from _connections.
     * Closes the TCP socket connection that c holds.
     * Removes c from the MessageRouter, the listener, and the unicaster.
     * 
     * This remove method isn't synchronized, use it when you're already in a synchronized block.
     * This remove method doesn't kick off connection fetchers, only the public version of remove does that.
     * 
     * @param c The ManagedConnection object to remove
     */
    private void removeInternal(ManagedConnection c) {

        /*
         * 1a) Remove from the initialized connections list and clean up the
         * stuff associated with initialized connections.  For efficiency
         * reasons, this must be done before (2) so packets are not forwarded
         * to dead connections (which results in lots of thrown exceptions).
         */

        // If we're both ultrapeers, or we're a leaf and c is an ultrapeer
        if (!c.isSupernodeClientConnection()) {

            // We can find c in _initializedConnections, the list of all the ultrapeers we're connected to
            int i = _initializedConnections.indexOf(c);
            if (i != -1) {

                // Remove c from the _initializedConnections list
                List newConnections = new ArrayList();                                  // Make a new, empty ArrayList called newConnections
                newConnections.addAll(_initializedConnections);                         // Copy all the references from _initializedConnections into it
                newConnections.remove(c);                                               // Remove the given ManagedConnection reference from it
                _initializedConnections = Collections.unmodifiableList(newConnections); // Replace _initializedConnections with an unmodifiable view of the new list

                // Subtract 1 from the counts that c was a part of
                if (c.isClientSupernodeConnection()) _shieldedConnections--; // Count that we now have one less connection up to an ultrapeer
                if (!c.isLimeWire())                 _nonLimeWirePeers--;    // Count that we're connected to one less ultrapeer that isn't running LimeWire
                if (checkLocale(c.getLocalePref()))  _localeMatchingPeers--; // Count that we're connected to one less ultrapeer with the same language preference as us
            }

        // We're an ultrapeer, and c is a leaf
        } else {

            // We can find c in _initializedClientConnections, the list of all the leaves we're connected to
            int i = _initializedClientConnections.indexOf(c);
            if (i != -1) {

                // Remove c from the _initializedClientConnections list
                List newConnections=new ArrayList();                                          // Make a new, empty ArrayList called newConnections
                newConnections.addAll(_initializedClientConnections);                         // Copy all the references from _initializedClientConnections into it
                newConnections.remove(c);                                                     // Remove the given ManagedConnection reference from it
                _initializedClientConnections = Collections.unmodifiableList(newConnections); // Replace the old list with an unmodifiable view of the new one

                // Subtract 1 from the counts that c was a part of
                if (!c.isLimeWire()) _nonLimeWireLeaves--; // We've got one fewer leaf that isn't running LimeWire
            }
        }

        /*
         * 1b) Remove from the all connections list and clean up the
         * stuff associated all connections
         */

        // Remove c from _connections, the list of all the ultrapeers we're trying to connect to or are connected to
        int i = _connections.indexOf(c);
        if (i != -1) {

            // Replace the _connections list with one just like it, but without c
            List newConnections = new ArrayList(_connections);           // Copy the references from the _connections list into a new ArrayList called newConnections
            newConnections.remove(c);                                    // Remove c from the new list
            _connections = Collections.unmodifiableList(newConnections); // Replace _connections with an unmodifiable view of the new list
        }

        /*
         * 2) Ensure that the connection is closed.  This must be done before
         * step (3) to ensure that dead connections are not added to the route
         * table, resulting in dangling references.
         */

        // Close our TCP socket connection to this remote computer
        c.close();

        /*
         * 3) Clean up route tables.
         */

        // Have the MessageRouter stop sending Gnutella packets to c
        RouterService.getMessageRouter().removeConnection(c);

        /*
         * 4) Notify the listener.
         */

        // (do)
        RouterService.getCallback().connectionClosed(c);

        /*
         * 5) Clean up Unicaster.
         */

        // (do)
        QueryUnicaster.instance().purgeQuery(c);
    }

    /**
     * If we have more connections than we need, stabilizeConnections() removes extra ones.
     * 
     * Disconnects from ultrapeers in the _initializedConnections list until we have just _preferredConnections number of them.
     * First, disconnects from any ultrapeers not running LimeWire.
     * If we still have too many, disconnects from the ultrapeers we connected to most recently.
     * 
     * setPreferredConnections() changes the value of _preferredConnections, and then calls this method.
     */
    private synchronized void stabilizeConnections() {
        
        // Loop while we're connected to more ultrapeers than we need to be
        while (getNumInitializedConnections() > _preferredConnections) {

            // We'll point newest at the connection in 
            
            ManagedConnection newest = null;

            // Loop through each ManagedConnection in the _initializedConnections list of ultrapeers we've finished the Gnutella handshake with
            for (Iterator i = _initializedConnections.iterator(); i.hasNext(); ) {
                ManagedConnection c = (ManagedConnection)i.next();

                /*
                 * first see if this is a non-limewire connection and cut it off
                 * unless it is our only connection left
                 */

                // This remote computer is running something other than LimeWire
                if (!c.isLimeWire()) {

                    // Pick it and leave the for loop
                    newest = c;
                    break;
                }

                /*
                 * The ManagedConnection c is running LimeWire.
                 */

                // If we don't have a winner yet, or c has a more recent connection time than our current winner, pick c as newest
                if (newest == null || c.getConnectionTime() > newest.getConnectionTime()) newest = c;
            }

            // If we found a non-LimeWire or most recently added ultrapeer, close the connection and remove it from our lists
            if (newest != null) remove(newest);
        }

        // If we need another connection, start a thread that will get one
        adjustConnectionFetchers();
    }

    /**
     * Starts or stops connection fetchers so there is one for each additional connection we need.
     * 
     * This maintains the following equation which should always be true as the program runs:
     * number of connections + number of connection fetchers >= _preferredConnections
     * 
     * _preferredConnections - number of connections - number of connection fetchers is called the need.
     * Call this method whenever the need changes.
     * 
     * The following things can happen that make the need change.
     * 
     * (1) setPreferredConnections() changes the _preferredConnections value
     * (2) remove(c) makes the number of connections drop
     * (3) initializeExternallyGeneratedConnection() makes the number of connections rise
     * (4) There's an initialization error in initializeFetchedConnection(), and the number of connections drops when removeInternal() runs
     * 
     * This method isn't called when a connection is fetched successfully from the host catcher.
     * The number of connections rises, but the number of connection fetchers drops, so the need number doesn't change.
     * 
     * The _dedicatedPrefFetcher is a ConnectionFetcher thread that will find us an ultrapeer that matches our language preference.
     * If we're a leaf that has a connection up to an ultrapeer, but none that speak our language yet, we start _dedicatedPrefFetcher.
     * It's created with ConnectionFetcher(true), which causes the HandshakeResponder to refuse foreign language computers.
     * 
     * Code here figures out how many more connections we need.
     * Each ConnectionFetcher thread will try to open one new connection.
     * If we need more connections, the method makes more ConnectionFetcher threads.
     * 
     * If we have too many connections, the method stops ConnectionFetcher threads.
     * If we still have too many connections, the method closes TCP sockets the ConnectionFetcher threads have opened.
     * These are connections we've opened, but we haven't done the Gnutella handshake through them yet.
     * 
     * This method isn't synchronized, only call it within a synchronized block.
     */
    private void adjustConnectionFetchers() {

        // True, we want to connect to remote computers with the same language preference as us
        if (ConnectionSettings.USE_LOCALE_PREF.getValue()) {
            
            /*
             * if it's a leaf and locale preferencing is on
             * we will create a dedicated preference fetcher
             * that tries to fetch a connection that matches the
             * clients locale
             */
            
            // If we're a leaf with at least one connection up to an ultrapeer, but none that speak our language yet
            if (RouterService.isShieldedLeaf() && // We're a leaf with at least one connection up to an ultrapeer, and
                _needPref &&                      // We're still looking for a connection that matches our language preference, and
                !_needPrefInterrupterScheduled && // The program doesn't have a preference fetcher interrupter scheduled yet, and
                _dedicatedPrefFetcher == null) {  // The fetcher dedicated to helping us as a leaf find an ultrapeer that matches our language isn't set up yet

                // Make the one dedicated language preference ConnectionFetcher
                _dedicatedPrefFetcher = new ConnectionFetcher(true); // True to have it refuse foreign language computers in the Gnutella handshake

                // Define a new object named interrupted that implements the Runnable interface right here
                Runnable interrupted = new Runnable() {

                    // Stop the dedicated preference fetcher in 15 seconds
                    public void run() {

                        synchronized (ConnectionManager.this) {

                            // Record that we no longer need a connection that matches our language preference
                            _needPref = false;

                            // If the dedicated preference fetcher isn't running, we're done
                            if (_dedicatedPrefFetcher == null) return;

                            // The dedicated preference fetcher is running, stop it
                            _dedicatedPrefFetcher.interrupt(); // Have it throw an exception
                            _dedicatedPrefFetcher = null;      // Remove our reference to it
                        }
                    }
                };

                // Record that now we do have the code scheduled to stop the fetcher
                _needPrefInterrupterScheduled = true;

                // Have the RouterService call the run() method above one time, 15 seconds from now
                RouterService.schedule(interrupted, 15 * 1000, 0);
            }
        }

        // Find out how many ultrapeers we're connected to, and how many more we need
        int goodConnections = getNumInitializedConnections();            // The number of ultrapeers we're connected to
        int neededConnections = _preferredConnections - goodConnections; // The number of additional ultrapeer connections we need

        /*
         * Now how many fetchers do we need?  To increase parallelism, we
         * allocate 3 fetchers per connection, but no more than 10 fetchers.
         * (Too much parallelism increases chance of simultaneous connects,
         * resulting in too many connections.)  Note that we assume that all
         * connections being fetched right now will become ultrapeers.
         * 
         * The end result of the following logic, assuming _preferredConnections is 32 for Ultrapeers, is:
         * When we have 22 active peer connections,             we fetch (27 - current) * 1 connections.
         * All other times, for Ultrapeers,                we will fetch (32 - current) * 3, up to a maximum of 20.
         * For leaves, assuming they maintin 4 Ultrapeers, we will fetch ( 4 - current) * 2 connections.
         */

        // We're firewalled and don't have any ultrapeer-to-ultrapeer connections
        int multiple;
        if (!RouterService.acceptedIncomingConnection() && !isActiveSupernode()) {

            /*
             * If we have not accepted incoming, fetch 3 times as many connections as we need.
             * We must also check if we're actively being a Ultrapeer because
             * it's possible we may have turned off acceptedIncoming while
             * being an Ultrapeer.
             */

            // Fetch 3 times as many connections as we need
            multiple = 3;

        // We're not an ultrapeer or can't become one, or we have no ultrapeer-to-ultrapeer connections
        } else if (!isSupernode() || getNumUltrapeerConnections() == 0) {
            
            /*
             * Otherwise, if we're not ultrapeer capable,
             * or have not become an Ultrapeer to anyone,
             * also fetch 3 times as many connections as we need.
             * It is critical that active ultrapeers do not use a multiple of 3
             * without reducing neededConnections, otherwise LimeWire would
             * continue connecting and rejecting connections forever.
             */

            // Fetch 3 times as many connections as we need
            multiple = 3;

        // We are an ultrapeer, and we need more than 10 connections
        } else if (neededConnections > 10) {

            /*
             * Otherwise (we are actively Ultrapeering to someone)
             * If we needed more than connections, still fetch
             * 2 times as many connections as we need.
             * It is critical that 10 is greater than RESERVED_NON_LIMEWIRE_PEERS,
             * else LimeWire would try connecting and rejecting connections forever.
             */

            // Fetch twice as many connections as we need
            multiple = 2;

        // We're an ultrapeer and we need less than 10 connections
        } else {

            /*
             * Otherwise, if we need less than 10 connections (and we're an Ultrapeer), 
             * decrement the amount of connections we need by 5,
             * leaving 5 slots open for newcomers to use,
             * and decrease the rate at which we fetch to
             * 1 times the amount of connections.
             */

            // Fetch just as many connections as we need
            multiple = 1;

            // Lower the number of connections we're seeking by 5 and the minimum number of non-LimeWire connections we want to have
            neededConnections -= 5 + ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections;
        }

        // Calculate how many connection fetchers we need, the need number can be negative
        int need = Math.min(10, multiple * neededConnections) - _fetchers.size() - _initializingFetchedConnections.size();

        // Stay under the Windows XP Service Pack 2 limit for half-open TCP socket connections
        need = Math.min(need, Sockets.getNumAllowedSockets());

        // If need is positive, make that many more connection fetchers
        while (need > 0) {

            // Make a new ConnectionFetcher object, which starts the thread, and add it to the _fetchers list
            _fetchers.add(new ConnectionFetcher());
            need--;
        }

        /*
         * Stop ConnectionFetchers as necessary, but it's possible there
         * aren't enough fetchers to stop.  In this case, close some of the
         * connections started by ConnectionFetchers.
         */

        // Find out how many ConnectionFetcher threads we have running right now
        int lastFetcherIndex = _fetchers.size(); // We'll subtract 1 before using the size as an index

        // If need is negative, stop that many connection fetchers
        while ((need < 0) && (lastFetcherIndex > 0)) {

            /*
             * ConnectionFetcher extends Thread, so a ConnectionFetcher object is a Thread.
             * Call fetcher.interrupt() to stop the thread.
             * If the thread is blocking on a call to wait(), it will throw an InterruptedException.
             * If it's just running code, it's status will change so now calling thread.isInterrupted() will return true.
             * initializeFetchedConnection() checks for this.
             */

            // Remove a ConnectionFetcher object from _fetchers, and stop the thread
            ConnectionFetcher fetcher = (ConnectionFetcher)_fetchers.remove(--lastFetcherIndex); // Subtract before resolving
            fetcher.interrupt();
            need++;
        }

        // Find out how many connections our ConnectionFetcher threads have opened
        int lastInitializingConnectionIndex = _initializingFetchedConnections.size(); // These are open TCP sockets that we haven't done the Gnutella handshake through yet

        // If need is still negative, close connections
        while ((need < 0) && (lastInitializingConnectionIndex > 0)) {

            // Get a connection a ConnectionFetcher thread opened but that we haven't done the Gnutella handshake through yet
            ManagedConnection connection = (ManagedConnection)_initializingFetchedConnections.remove(--lastInitializingConnectionIndex);
            
            // Close the connection and remove it from our lists
            removeInternal(connection);
            need++;
        }
    }

    /**
     * Connect to the remote computer, do the Gnutella handshake, and add it to the right list.
     * 
     * Only ConnectionFetcher.managedRun() calls this.
     * When it does, we're at the following stage of the process of getting a new Gnutella connection.
     * We've gotten an IP address from the HostCatcher.
     * We've made a ManagedConnection object with it.
     * That's it, now this method will take things from there.
     * 
     * Adds the ManagedConnection object to the _initializingFetchedConnections and _connections lists.
     * Removes the ConnectionFetcher thread from the _fetchers list.
     * Calls mc.initialize() to connect to the remote computer, do the Gnutella handshake, and setup compression on the connection.
     * Looks at the headers from the remote computer to save its listening port and grab IP addresses for the HostCatcher.
     * Adds mc to the right list and sends it a ping packet.
     * 
     * @param mc      A new ManagedConnection object we just made from an IP address from the HostCatcher
     * @param fetcher The ConnectionFetcher thread that is in charge of opening this connection
     * 
     * @exception IOException           We couldn't connect a TCP socket to the remote computer
     * @exception NoGnutellaOkException The remote computer rejected us in the Gnutella handshake
     * @exception BadHandshakeException The remote computer responded with HTTP, closed the connection during the handshake, or something else
     */
    private void initializeFetchedConnection(ManagedConnection mc, ConnectionFetcher fetcher) throws NoGnutellaOkException, BadHandshakeException, IOException {

        synchronized (this) {

            // If another thread called interrupt() on the ConnectionFetcher thread
            if (fetcher.isInterrupted()) {

                /*
                 * Externally generated interrupt.
                 * The interrupting thread has recorded the
                 * death of the fetcher, so throw IOException.
                 * (This prevents fetcher from continuing!)
                 */

                // Throw an exception to prevent the ConnectionFetcher from continuing
                throw new IOException("connection fetcher");
            }

            // Add the ManagedConnection object to our list of connections that have ConnectionFetcher threads connecting them
            _initializingFetchedConnections.add(mc);

            /*
             * Remove references to the ConnectionFetcher from the ConnectionManager object.
             */

            // If this is the ConnectionFetcher dedicated to finding us as a leaf at least one ultrapeer that matches our language preference
            if (fetcher == _dedicatedPrefFetcher) _dedicatedPrefFetcher = null; // It is, remove our reference to it
            else                                  _fetchers.remove(fetcher);    // It's not, remove it from our list of ConnectionFetcher threads

            // Add the ManagedConnection object to the _connections list
            connectionInitializing(mc);

            /*
             * No need to adjust connection fetchers here.  We haven't changed
             * the need for connections; we've just replaced a ConnectionFetcher
             * with a Connection.
             */
        }

        // Tell the GUI that we're connecting to the remote computer
        RouterService.getCallback().connectionInitializing(mc);

        try {

            // Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection
            mc.initialize();

        // We were unable to open a TCP socket connection to that IP address and port number
        } catch (IOException e) {

            synchronized (ConnectionManager.this) {

                // Remove mc from our list of connections that have ConnectionFetcher threads connecting them
                _initializingFetchedConnections.remove(mc);

                // Close the connection and remove it from our lists
                removeInternal(mc);

                /*
                 * We've removed a connection, so the need for connections went
                 * up.  We may need to launch a fetcher.
                 */

                // If we need another connection, start a thread that will try to get one
                adjustConnectionFetchers();
            }

            // Throw the exception so the next part of the program will have to catch it
            throw e;

        // Even if there's an exception, we still want to get the IP addresses from the headers
        } finally {

            // Look at the headers the remote computer sent us to save its listening port and grab IP addresses for the HostCatcher
            processConnectionHeaders(mc);
        }

        // Add mc to the right list and send it a ping packet
        completeConnectionInitialization(mc, true); // A ConnectionFetcher thread got us this connection, remove it from the _initializingFetchedConnections list
    }

    /**
     * Look at the headers the remote computer sent us to save its listening port and grab IP addresses for the HostCatcher
     * 
     * Reads "X-Try-Ultrapeers", giving the IP addresses and port numbers to the HostCatcher.
     * If the remote computer connected to us, reads it's listening port number from its "Listen-IP" header.
     * We save that port number in the ManagedConnection object, overwriting our record of the epehmeral port the remote computer connected to us from.
     * 
     * @param connection The ManagedConnection object for a remote computer with which we just finished the Gnutella handshake
     */
    private void processConnectionHeaders(Connection connection) {

    	// TODO:kfaaborg receivedHeaders always returns true
        if (!connection.receivedHeaders()) return;

        // Get all the Gnutella handshake headers the remote computer sent us in a Java Properties hash table of strings
        Properties headers = connection.headers().props(); // Returns a HandshakeResponse object, which extends Properties
        if (headers == null) return;

        // Read the "X-Try-Ultrapeers" header the remote computer sent us, and give the IP addresses and port numbers to the HostCatcher
        updateHostCache(connection.headers());

        // Get the remote computer's IP address, the value of the "Listen-IP" or older "X-My-Address" header
        String remoteAddress = headers.getProperty(HeaderNames.LISTEN_IP);
        if (remoteAddress == null) remoteAddress = headers.getProperty(HeaderNames.X_MY_ADDRESS);

        /*
         * set the remote port if not outgoing connection (as for the outgoing
         * connection, we already know the port at which remote host listens)
         */

        // If the remote computer said "Listen-IP" and it connected to us
        if ((remoteAddress != null) && (!connection.isOutgoing())) {

            // Get the index of the port number just beyond the colon in text like "12.152.83.138:6346"
            int colonIndex = remoteAddress.indexOf(':');
            if (colonIndex == -1) return;
            colonIndex++;
            if (colonIndex > remoteAddress.length()) return;

            try {

                // Read the text beyond the colon as a number, the port number
                int port = Integer.parseInt(remoteAddress.substring(colonIndex).trim());

                // It's 1 through 65535
                if (NetworkUtils.isValidPort(port)) {

                    /*
                     * for incoming connections, set the port based on what it's
                     * connection headers say the listening port is
                     */

                    // Save the remote computer's listening port, overwriting the separate ephemeral port it connected to us from
                    connection.setListeningPort(port);
                }

            // The remote computer sent us text we couldn't parse, ignore it
            } catch (NumberFormatException e) {}
        }
    }

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

    /**
     * Read the "X-Try-Ultrapeers" header a remote computer sent us, giving the IP addresses and port numbers to the HostCatcher.
     * 
     * @param headers A HandshakeResponse object that holds all the Gnutella handshake headers the remote computer sent us
     */
    private void updateHostCache(HandshakeResponse headers) {

        // Get the value of the "X-Try-Ultrapeers" header the remote computer sent us
        if (!headers.hasXTryUltrapeers()) return;           // Make sure the remote computer sent us an "X-Try-Ultrapeers" header
        String hostAddresses = headers.getXTryUltrapeers(); // Get the value, which is text like "12.152.83.138:6346,63.252.232.216:6346,69.178.195.177:6346"

        // Split the text around commas to get individual parts like "12.152.83.138:6346" and "63.252.232.216:6346", and loop for each one
        StringTokenizer st = new StringTokenizer(hostAddresses, Constants.ENTRY_SEPARATOR); // A StringTokenizer to hold each part of text
        List hosts = new ArrayList(st.countTokens()); // An ArrayList to hold the Endpoint objects we turn each part of text into
        while (st.hasMoreTokens()) {

            // Remove any spaces from before or after this IP address and port number text
            String address = st.nextToken().trim();

            try {

                // Make a new Endpoint to hold the IP address and port number
                Endpoint e = new Endpoint(address);
                hosts.add(e);

            // If reading that text as numbers didn't work, just loop to try the next one
            } catch (IllegalArgumentException iae) { continue; }
        }

        // Have the HostCatcher add all those IP addresses to the list it keeps
        _catcher.add(hosts); // hosts is an ArrayList with Endpoint objects in it
    }

    /**
     * Connects to the remote computer, do the Gnutella handshake, setup compression, and send the remote computer a ping packet.
     * 
     * Makes sure we have a slot for this kind of computer.
     * Updates the GUI to show we're connected.
     * 
     * If the new connection is outgoing, we made c from an IP address and port number and haven't connected yet.
     * If the new connection is incoming, we made c from a socket that's connected.
     * 
     * @param c A new ManagedConnection object
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c) throws IOException {

        /*
         * For outgoing connections add it to the GUI and the fetcher lists now.
         * For incoming, we'll do this below after checking incoming connection
         * slots.  This keeps reject connections from appearing in the GUI, as
         * well as improving performance slightly.
         */

        // We just made the given ManagedConnection object from an IP address and port number
        if (c.isOutgoing()) {

            synchronized (this) {

                // Add c to the _connections list
                connectionInitializing(c);

                /*
                 * We've added a connection, so our need for connections went down.
                 */

                // If we have too many ConnectionFetcher threads trying to connect to remote computers, cancel one
                adjustConnectionFetchers();
            }

            // Have the GUI show this outgoing connection is initializing
            RouterService.getCallback().connectionInitializing(c);
        }

        try {

            // Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection
            c.initialize();

        // There was a problem connecting to the computer
        } catch (IOException e) {

            // Close the connection and remove it from our lists, and throw the exception upwards
            remove(c);
            throw e;

        // Run this code if there was no exception or before leaving if there was one 
        } finally {

            // Look at the headers the remote computer sent us to save its listening port and grab IP addresses for the HostCatcher
            processConnectionHeaders(c);
        }

        /*
         * If there's not space for the connection, destroy it.
         * It really should have been destroyed earlier, but this is just in case.
         */

        // If the remote computer connected to us, and we don't have a slot for this type of remote computer
        if (!c.isOutgoing() && !allowConnection(c)) {

            /*
             * No need to remove, since it hasn't been added to any lists.
             */

            // Throw an exception because we don't have a free slot for this
            throw new IOException("No space for connection");
        }

        /*
         * For incoming connections, add it to the GUI.  For outgoing connections
         * this was done at the top of the method.  See note there.
         */

        // If the remote computer connected to us
        if (!c.isOutgoing()) {

            synchronized (this) {

                // Add c to the _connections list
                connectionInitializingIncoming(c);

                /*
                 * We've added a connection, so our need for connections has gone down.
                 */

                // If we have too many ConnectionFetcher threads trying to connect to remote computers, cancel one
                adjustConnectionFetchers();
            }

            // Have the GUI show this outgoing connection is initializing
            RouterService.getCallback().connectionInitializing(c);
        }

        // Add c to the right list and send it a ping packet
        completeConnectionInitialization(c, false); // False, the remote computer connected to us
    }

    /**
     * Adds a given ManagedConnection to the right list and sends it a ping packet.
     * 
     * If a ConnectionFetcher got us this connection, removes it from the _initializingFetchedConnections list.
     * 
     * Calls connectionInitialized(mc), which:
     * Makes sure we have a slot for mc and disconnects if we don't.
     * adds mc to _initializedConnections or _initializedClientConnections.
     * Sends it a ping packet.
     * 
     * Tells the GUI to show this connection as initialized.
     * If we have too many ultrapeer connections, disconnects from some of them.
     * 
     * @param mc      A ManagedConnection we've finished the Gnutella handshake with.
     * @param fetched True if one of our ConnectionFetcher threads connected to this remote computer.
     *                False if the remote computer connected to us.
     */
    private void completeConnectionInitialization(ManagedConnection mc, boolean fetched) {

        synchronized (this) {

            // If a ConnectionFetcher thread got us this connection, remove it from the list
            if (fetched) _initializingFetchedConnections.remove(mc);

            /*
             * If the connection was killed while initializing, we shouldn't announce its initialization.
             */

            // Add mc to _initializedConnections or _initializedClientConnections and send it a ping packet
            boolean connectionOpen = connectionInitialized(mc); // Returns false if mc wasn't in _connections or we don't have a slot for it

            // connectionInitialized(mc) didn't disconnect us from mc
            if (connectionOpen) {

                // Have the GUI show the connection as initialized
                RouterService.getCallback().connectionInitialized(mc);

                // Set _preferredConnections to 32 if we're an ultrapeer, or 3 if we're a leaf, and disconnect from extra ultrapeers
                setPreferredConnections();
            }
        }
    }

    /**
     * The number of ultrapeers we should have, 32 if we're an ultrapeer or 3 if we're a leaf.
     * If we have fewer connections than this number, we have threads to get more.
     * If we have more connections than this number, we're disconnecting from some to take the number down.
     * 
     * @return The value of _preferredConnections
     */
    public int getPreferredConnectionCount() {

        // Return the value of _preferredConnections
        return _preferredConnections;
    }

    /**
     * Determines if we're attempting to maintain the idle connection count.
     * When the user leaves the computer for a half hour, we as a leaf will drop down from 3 ultrapeer connections to just 1.
     * 
     * @return True if _preferredConnections is only 1 right now
     */
    public boolean isConnectionIdle() {

        // Return true if _perferredConnections is only 1 right now
        return _preferredConnections == ConnectionSettings.IDLE_CONNECTIONS.getValue();
    }

    /**
     * Sets the number of ultrapeer connections we'll maintain, and disconnect from some if we have too many.
     * Sets _preferredConnections to 32 if we're an ultrapeer, or 3 if we're a leaf.
     * 
     * This method is private, and called 3 places from within the ConnectionManager class:
     * ConnectionManager.completeConnectionInitialization() calls this after we've connected a new remote computer.
     * ConnectionManager.initialize() sets up a thread that calls this every second.
     * ConnectionManager.connect() calls this when the program starts trying to connect to the network.
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

        // If we changed the number of connections we should have, call stabilizeConnections() to disconnect from some
        if (oldPreferred != _preferredConnections) stabilizeConnections();
    }

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

    /*
     * End connection list management functions.
     * Begin connection launching thread inner classes.
     */

    /**
     * Have a thread run a OutgoingConnector object to connect a ManagedConnection object, and get it ready for Gnutella packet exchange.
     * 
     * createConnectionBlocking() and createConnectionAsynchronously() make new OutgoingConnector objects with this constructor.
     * createConnectionBlcoking() passes false, it has already connected the ManagedConnection object.
     * createConnectionAsynchronously() passes true, we'll have to connect the ManagedConnection object.
     */
    private class OutgoingConnector implements Runnable {

        /** The ManagedConnection object we'll connect and setup for reading. */
        private final ManagedConnection _connection;

        /** True if we need to connect _connection, false if it's already connected and we just need to make the chain of readers. */
        private final boolean _doInitialization;

        /**
         * Make a new OutgoingConnector object that will connect a given ManagedConnection object and set it up for reading.
         * 
         * createConnectionBlocking() and createConnectionAsynchronously() make new OutgoingConnector objects with this constructor.
         * createConnectionBlcoking() passes false, it has already connected the ManagedConnection object.
         * createConnectionAsynchronously() passes true, we'll have to connect the ManagedConnection object.
		 * 
		 * @param connection The ManagedConnection object
         * @param initialize True if this OutgoingConnector will have to connect the ManagedConnection before setting up the chain of readers
         */
        public OutgoingConnector(ManagedConnection connection, boolean initialize) {

            // Save the given values
            _connection = connection;
            _doInitialization = initialize;
        }

        /**
         * Connects to the remote computer, does the Gnutella handshake, and makes the chain of readers.
         * 
         * When we give a thread an OutgoingConnector object, it calls this run() method.
         */
        public void run() {

            try {

                // If createConnectionAsynchronously() made this OutgoingConnector, connect to the remote computer
				if (_doInitialization) initializeExternallyGeneratedConnection(_connection);

                // Rename this thread "MessageLoopingThread" and make the chain of readers
				startConnection(_connection);

            } catch (IOException ignored) {}
        }
    }

	/**
     * Renames this thread "MessageLoopingThread" and makes the chain of readers.
	 * 
	 * @param c A ManagedConnection that we've connected, done the handshake, and added to our lists
     * 
	 * @throws IOException If our connection to the remote computer drops
	 */
	private void startConnection(ManagedConnection c) throws IOException {

        // Change the name of this thread to "MessageLoopingThread"
	    Thread.currentThread().setName("MessageLoopingThread");

        // The remote computer supports GUESS, the way of giving rare searches longer TTLs
		if (c.isGUESSUltrapeer()) {

            // Give its IP address and port number to the QueryUnicaster (do)
			QueryUnicaster.instance().addUnicastEndpoint(c.getInetAddress(), c.getPort());
		}

        // Make the chain of objects the program will use to read data from the remote computer
		c.loopForMessages(); // Throws IOException if our connection to the remote computer drops
	}

    /**
     * To find and connect to a new Gnutella computer, make a ConnectionFetcher thread.
     * It will get an IP address from the HostCatcher, connect to it, do the Gnutella handshake, and get ready to exchange Gnutella packets.
     * 
     * The managedRun() method performs these steps:
     * Gets an IP address and port number from the HostCatcher.
     * Makes a new ManagedConnection object from it called connection.
     * Has initializeFetchedConnection() connect, do the Gnutella handshake, and setup compression.
     * Catches IOException if we couldn't connect.
     * Catches NoGnutellaOkException if we connected, but then the remote computer refused us in the Gnutella handshake.
     * Renames this thread from "ConnectionFetcher" to "MessageLoopingThread".
     * Makes the chain of readers that will get the data the remote computer sends, decompress it, and slice it into Gnutella packets.
     * 
     * ConnectionFetcher extends ManagedThread, which extends Thread.
     * So, ConnectionFetcher is a Thread.
     * 
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
    private class ConnectionFetcher extends ManagedThread {

        /** False by default, true to have the HandshakeResponder refuse a foreign language computer in the Gnutella handshake. */
        private boolean _pref = false;

        /**
         * Make a ConnectionFetcher thread.
         * It will get an IP address from the HostCatcher, make a ManagedConnection object for it, and open a TCP socket connection to it.
         * 
         * The adjustConnectionFetchers() method makes a new ConnectionFetcher thread and keeps it in the _fetchers list.
         * 
         * Only call this from a thread that is synchronized on the ConnectionManager.
         * This constructor is only called from adjustConnectionFetchers(), which has the same locking requirement.
         */
        public ConnectionFetcher() {

            // Call the next constructor
            this(false); // False to not refuse a foreign language computer in the Gnutella handshake
        }

        /**
         * Make a ConnectionFetcher thread.
         * It will get an IP address from the HostCatcher, make a ManagedConnection object for it, and open a TCP socket connection to it.
         * 
         * The adjustConnectionFetchers() method makes a new ConnectionFetcher thread and keeps it in the _fetchers list.
         * 
         * Only call this from a thread that is synchronized on the ConnectionManager.
         * This constructor is only called from adjustConnectionFetchers(), which has the same locking requirement.
         * 
         * @param pref True to have this ConnectionFetcher thread refuse a foreign language computer in the Gnutella handshake
         */
        public ConnectionFetcher(boolean pref) {

            // Name this thread "ConnectionFetcher"
            setName("ConnectionFetcher");

            // Save the language preferencing option
            _pref = pref;

            // Have the thread call the managedRun() method below
            setDaemon(true); // Let Java close the program even if this thread is still running
            start();         // Have Java begin the thread by calling ManagedThread.run(), which calls managedRun() below
        }

        /**
         * Find and connect to a new computer on the Internet running Gnutella software, and get ready to exchange Gnutella packets.
         * 
         * Gets an IP address and port number from the HostCatcher.
         * Makes a new ManagedConnection object from it called connection.
         * Has initializeFetchedConnection() connect, do the Gnutella handshake, and setup compression.
         * Catches IOException if we couldn't connect.
         * Catches NoGnutellaOkException if we connected, but then the remote computer refused us in the Gnutella handshake.
         * Renames this thread from "ConnectionFetcher" to "MessageLoopingThread".
         * Makes the chain of readers that will get the data the remote computer sends, decompress it, and slice it into Gnutella packets.
         * 
         * When adjustConnectionFetchers() makes a new ConnectionFetcher thread, the constructor above calls start().
         * This calls ManagedThread.run(), which calls this method, managedRun().
         */
        public void managedRun() {

            try {

                // Loop, waiting for the HostCatcher to give us an IP address to try, and making sure we get a good one
                Endpoint endpoint = null;
                do {

                    // Get an IP address and port number we can try connecting to from the HostCatcher object
                    endpoint = _catcher.getAnEndpoint();

                } while (

                    // If the IP address the HostCatcher gave us isn't on our block list and we're not already connected to it, leave the loop
                    !IPFilter.instance().allow(endpoint.getAddress()) || // That address is on our list of institutional addresses to avoid, or
                    isConnectedTo(endpoint.getAddress())                 // We're already connected to that address, loop again to get another one
                );

                // Make sure we got one
                Assert.that(endpoint != null);

                // Record this as another attempt to connect to a remote computer
                _connectionAttempts++;

                // Make a new ManagedConnection object from the IP address and port number the HostCatcher gave us
                ManagedConnection connection = new ManagedConnection(endpoint.getAddress(), endpoint.getPort());

                // If _pref is true, configure the HandshakeResponder to refuse foreign language computers
                connection.setLocalePreferencing(_pref);

                /*
                 * If we've been trying to connect for a while, check to make
                 * sure the user's internet connection is live.  We only do
                 * this if we're not already connected, have not made any
                 * successful connections recently, and have not checked the
                 * user's connection in the last little while or have very
                 * few hosts left to try.
                 */

                // If we've been having a lot of trouble connecting, start the ConnectionChecker
                long curTime = System.currentTimeMillis();
                if (!isConnected() &&                                      // If we don't have any Gnutella connections, and
                    _connectionAttempts > 40 &&                            // We've tried to connect to more than 40 remote computers, and
                    ((curTime - _lastSuccessfulConnect) > 4000) &&         // Our last successful TCP connection happened more than 4 seconds ago, and
                    ((curTime - _lastConnectionCheck) > 60 * 60 * 1000)) { // We last checked our Internet connection more than an hour ago

                    // Reset our count of connection attempts back down to 0
                    _connectionAttempts = 0;

                    // Start a thread that will try popular Web sites to see if our Internet connection is dead
                    _lastConnectionCheck = curTime;
                    LOG.debug("checking for live connection");
                    ConnectionChecker.checkForLiveConnection();
                }

                /*
                 * Try to connect, recording success or failure so HostCatcher
                 * can update connection history.  Note that we declare
                 * success if we were able to establish the TCP connection
                 * but couldn't handshake (NoGnutellaOkException).
                 */

                try {

                    // Connect to the remote computer, do the Gnutella handshake, and add it to the right list 
                    initializeFetchedConnection(connection, this); // Give it a reference to this ConnectionFetcher thread

                    // That worked without an exception, so we're connected and done with the Gnutella handshake now
                    _lastSuccessfulConnect = System.currentTimeMillis(); // Record we successfully initiated a TCP connection now
                    _catcher.doneWithConnect(endpoint, true);            // Have the host catcher record another success with this listing

                    // If we were refusing foreign language computers, this one must have matched, record that we don't have to do that anymore
                    if (_pref) _needPref = false;

                // The remote computer responded with something other than "GNUTELLA/0.6 200 OK"
                } catch (NoGnutellaOkException e) {

                    // Record we successfully initiated a TCP connection now
                    _lastSuccessfulConnect = System.currentTimeMillis();

                    // The remote computer said 577, it refused us because we don't have the same language preference as it does
                    if (e.getCode() == HandshakeResponse.LOCALE_NO_MATCH) {
                        
                        /*
                         * if it failed because of a locale matching issue
                         * readd to hostcatcher??
                         */

                        // Add it to the HostCatcher now that we know its language preference
                        _catcher.add(endpoint, true, connection.getLocalePref());

                    // The remote computer refused us in the Gnutella handshake for some other reason
                    } else {

                        // Have the HostCatcher record that we were able to establish a TCP connection, but then the computer refused us in the Gnutella handshake
                        _catcher.doneWithConnect(endpoint, true); // True, we established the TCP connection
                        _catcher.putHostOnProbation(endpoint);    // List the computer with others that are online, but not accepting Gnutella connections right now
                    }

                    // NoGnutellaOkException extends IOException, so the catch block in this method one level up for IOException will catch it
                    throw e;

                // We couldn't connect a TCP socket to the remote computer
                } catch (IOException e) {

                    // Have the HostCatcher record that we weren't able to establish a TCP connection
                    _catcher.doneWithConnect(endpoint, false); // False, we weren't able to establish the TCP connection
                    _catcher.expireHost(endpoint);             // List the computer with others that we weren't able to connect to

                    // Throw the IOException to the catch block one level up
                    throw e;
                }

                // Rename the thread from "ConnectionFetcher" to "MessageLoopingThread", and make the chain of readers
				startConnection(connection);

            // Connecting to the remote computer caused a NoGnutellaOkException or an IOException
            } catch (IOException e) {

                // Just keep going

            // Another thread called interrupt() on this ConnectionFetcher thread to stop it from trying to connect to a remote computer
            } catch (InterruptedException e) {

                /*
                 * Externally generated interrupt.
                 * The interrupting thread has recorded the
                 * death of the fetcher, so just return.
                 */

                // Return, which will exit from this thread's run() method and end the thread
                return;

            // Some other exception happened
            } catch (Throwable e) {

                // Give it to the ErrorService
                ErrorService.error(e);
            }
        }

        /**
         * Express this ConnectionFetcher thread as a String.
         * 
         * @return The text "ConnectionFetcher"
         */
        public String toString() {

            // Just return the word "ConnectionFetcher"
            return "ConnectionFetcher";
        }
	}

    /**
     * Disconnects from the Gnutella network, and has some code run every 2 minutes to try to connect.
     * 
     * ConnectionChecker.run() calls this when it looks like we have no Internet connection.
     * Tells the user they have no Internet connection, and the program will keep looking for one.
     * Disconnects the program from the Gnutella network, closing all connections and setting the number we want to 0.
     * Has the RouterService call some code here every 2 minutes that tries to connect.
     */
    public void noInternetConnection() {

        // If we've already told the user about their connection and started the thread that tries later, don't do it again
        if (_automaticallyConnecting) return;

        // Tell the user that they have no Internet connection, and the program will keep looking for one
        MessageService.showError("NO_INTERNET_RETRYING", QuestionsHandler.NO_INTERNET_RETRYING);

        // Disconnect the program from the Gnutella network, closing all connections and setting the number we want to 0
        disconnect(); // Kills all the ConnectionFetcher threads that we have running

        // Try to reconnect in 10 seconds, and every 2 minutes after that
        RouterService.schedule(     // Have the RouterService run some code here later
            new Runnable() {        // Define a new class right here that implements the Runnable interface
                public void run() { // This is the run() method that the RouterService will have a thread call later

                    /*
                     * If the last time the user disconnected is more recent
                     * than when we started automatically connecting, just
                     * return without trying to connect.  Note that the
                     * disconnect time is reset if the user selects to connect.
                     */

                    // If the user has disconnected since we started trying to automatically reconnect, don't try to connect
                    if (_automaticConnectTime < _disconnectTime) return;

                    // If we don't have any Gnutella connections
                    if (!RouterService.isConnected()) {

                        /*
                         * Try to re-connect.  Note this call resets the time
                         * for our last check for a live connection, so we may
                         * hit web servers again to check for a live connection.
                         */

                        // Set _preferredConnections to 32 or 3, and have the HostCatcher send UDP Gnutella ping packets
                        connect();
                    }
                }
            },

            // Run this 10 seconds from now
            10 * 1000,

            // And every 2 minutes after that
            2 * 60 * 1000
        );

        // Record that we started trying to automatically connect right now
        _automaticConnectTime = System.currentTimeMillis();
        _automaticallyConnecting = true;

        // If the HostCatcher has less than 100 IP addresses in its list, clear the list and read in the gnutella.net file again
        recoverHosts();
    }

    /**
     * If the HostCatcher has less than 100 IP addresses in its list, clear the list and read in the gnutella.net file again.
     */
    private void recoverHosts() {

        /*
         * Notify the HostCatcher that it should keep any hosts it has already
         * used instead of discarding them.
         * The HostCatcher can be null in testing.
         */

        // If the HostCatcher has less than 100 IP adresses in its list
        if (_catcher != null && _catcher.getNumHosts() < 100) {

            // Clear the list and read in the gnutella.net file again
            _catcher.recoverHosts();
        }
    }

    /**
     * True if the given language preference matches our language preference.
     * 
     * @param loc A remote computer's language preference, like "en" for English, or null if it didn't send a "X-Locale-Pref" header.
     * @return    True if it matches our language preference.
     *            False if it doesn't match.
     *            True if it didn't say.
     */
    private boolean checkLocale(String loc) {

        // If the remote computer didn't tell us its language preference with a header like "X-Locale-Pref: en", imagine it did and said our language preference
        if (loc == null) loc = ApplicationSettings.DEFAULT_LOCALE.getValue();

        // Return true if the remote computer said or language preference or didn't say, false if it doesn't match
        return ApplicationSettings.LANGUAGE.getValue().equals(loc);
    }
}
