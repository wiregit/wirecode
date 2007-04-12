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
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntSet;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.util.SystemUtils;

import com.limegroup.gnutella.connection.ConnectionChecker;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent.EventType;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutella.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.EventDispatcher;
import com.limegroup.gnutella.util.Sockets;

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
public class ConnectionManager implements ConnectionAcceptor, 
EventDispatcher<ConnectionLifecycleEvent, ConnectionLifecycleListener>{

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

    /**
     * The number of connections leaves should maintain to Ultrapeers.
     */
    public static final int PREFERRED_CONNECTIONS_FOR_LEAF = 3;

    /**
     * How many connect back requests to send if we have a single connection
     */
    public static final int CONNECT_BACK_REDUNDANT_REQUESTS = 3;

    /**
     * The minimum amount of idle time before we switch to using 1 connection.
     */
    private static final int MINIMUM_IDLE_TIME = 30 * 60 * 1000; // 30 minutes

    /**
     * The number of leaf connections reserved for non LimeWire clients.
     * This is done to ensure that the network is not solely LimeWire centric.
     */
    public static final int RESERVED_NON_LIMEWIRE_LEAVES = 2;

    /**
     * The current number of connections we want to maintain.
     */
    private volatile int _preferredConnections = -1;

    /**
     * Reference to the <tt>HostCatcher</tt> for retrieving host data as well
     * as adding host data.
     */
    private HostCatcher _catcher;

    /** Threads trying to maintain the NUM_CONNECTIONS.
     *  LOCKING: obtain this. */
    private final List<ConnectionFetcher> _fetchers =
        new ArrayList<ConnectionFetcher>();
    /** 
     * Set of class C networks that we are connecting or connected to
     * LOCKING: this 
     */
    private final IntSet _classCNetworks = new IntSet(); 
    /** Connections that have been fetched but not initialized.  I don't
     *  know the relation between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtain this. */
    private final List<ManagedConnection> _initializingFetchedConnections =
        new ArrayList<ManagedConnection>();

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
    //TODO:: why not use sets here??
    private volatile List<ManagedConnection>
        _connections = Collections.emptyList();
    private volatile List<ManagedConnection>
        _initializedConnections = Collections.emptyList();
    private volatile List<ManagedConnection>
        _initializedClientConnections = Collections.emptyList();

    private volatile int _shieldedConnections = 0;
    private volatile int _nonLimeWireLeaves = 0;
    private volatile int _nonLimeWirePeers = 0;
    /** number of peers that matches the local locale pref. */
    private volatile int _localeMatchingPeers = 0;

	/**
	 * Variable for the number of times since we attempted to force ourselves
	 * to become an Ultrapeer that we were told to become leaves.  If this
	 * number is too great, we give up and become a leaf.
	 */
	private volatile int _leafTries;

	/**
	 * The number of demotions to ignore before allowing ourselves to become
	 * a leaf -- this number depends on how good this potential Ultrapeer seems
	 * to be.
	 */
	private volatile int _demotionLimit = 0;

    /**
     * The current measured upstream bandwidth in kbytes/sec.
     */
    private volatile float _measuredUpstreamBandwidth = 0.f;

    /**
     * The current measured downstream bandwidth in kbytes/sec.
     */
    private volatile float _measuredDownstreamBandwidth = 0.f;
    
    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final CopyOnWriteArrayList<ConnectionLifecycleListener> connectionLifeCycleListeners = 
        new CopyOnWriteArrayList<ConnectionLifecycleListener>();

    /**
     * Constructs a ConnectionManager.  Must call initialize before using
     * other methods of this class.
     */
    public ConnectionManager() { }

    /**
     * Links the ConnectionManager up with the other back end pieces and
     * launches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initialize() {
        _catcher = RouterService.getHostCatcher();

        RouterService.getConnectionDispatcher().
        addConnectionAcceptor(this,
        		new String[]{ConnectionSettings.CONNECT_STRING_FIRST_WORD,"LIMEWIRE"},
        		false,
        		false);
        
        // schedule the Runnable that will allow us to change
        // the number of connections we're shooting for if
        // we're idle.
        if(SystemUtils.supportsIdleTime()) {
            RouterService.schedule(new Runnable() {
                public void run() {
                    setPreferredConnections();
                }
            }, 1000, 1000);
        }
        
        // send new capabilities when simpp updates.
        SimppManager.instance().addListener(new SimppListener() {
            public void simppUpdated(int newVersion) {
                CapabilitiesVM.reconstructInstance();
                sendUpdatedCapabilities();
            }
        });
    }


    /**
     * Create a new connection, blocking until it's initialized, but launching
     * a new thread to do the message loop.
     */
    public ManagedConnection createConnectionBlocking(String hostname, int portnum) throws IOException {
        ManagedConnection c = new ManagedConnection(hostname, portnum);

        // Initialize synchronously
        initializeExternallyGeneratedConnection(c, null);
        // Kick off a thread for the message loop.
        ThreadExecutor.startThread(new OutgoingConnector(c, false), "OutgoingConnector");
        return c;
    }

    /**
     * Create a new connection, allowing it to initialize and loop for messages on a new thread.
     */
    public void createConnectionAsynchronously(
            String hostname, int portnum) {

		Runnable outgoingRunner =
			new OutgoingConnector(new ManagedConnection(hostname, portnum),
								  true);
        // Initialize and loop for messages on another thread.

		ThreadExecutor.startThread(outgoingRunner, "OutgoingConnectionThread");
    }


    public void acceptConnection(String word, Socket socket) {
    	if (word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) 
    		HTTPStat.GNUTELLA_REQUESTS.incrementStat();
    	else if (ConnectionSettings.CONNECT_STRING.isDefault() &&
    			word.equals("LIMEWIRE") ) 
    		HTTPStat.GNUTELLA_LIMEWIRE_REQUESTS.incrementStat();
    	else
    		return; //drop it.
    	
    	acceptConnection(socket);
    }
    
    /**
     * Create an incoming connection.
     * 
     * If the connection can support asynchronous messaging, this method will return
     * immediately.  Otherwise, this will block forever while the connection handshakes
     * and then loops for messages (it will return when the connection dies).
     */
     void acceptConnection(Socket socket) {
         ManagedConnection connection = new ManagedConnection(socket);
         
         GnetConnectObserver listener = null;
         
         if(connection.isAsynchronous())
             listener = new IncomingGNetObserver(connection);
         else
             Thread.currentThread().setName("IncomingConnectionThread");
         
         try {
             initializeExternallyGeneratedConnection(connection, listener);
         } catch (IOException e) {
			 connection.close();
             return;
         }
         
         // Otherwise, the listener will be notified when to start.
         if(listener == null) {
             try {
    			 startConnection(connection);
             } catch(IOException e) {
                 // the blocking connection died.
             }
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
		// removal may be disabled for tests
		if(!ConnectionSettings.REMOVE_ENABLED.getValue()) return;
        removeInternal(mc);

        adjustConnectionFetchers();
    }

    /**
     * True if this is currently or wants to be a supernode,
     * otherwise false.
     */
    public boolean isSupernode() {
        return isActiveSupernode() || isSupernodeCapable();
    }
    
    /** Return true if we are not a private address, have been ultrapeer capable
     *  in the past, and are not being shielded by anybody, we don't have UP
     *  mode disabled AND we are not exclusively a DHT node.
     */
    public boolean isSupernodeCapable() {
        return !NetworkUtils.isPrivate() &&
               UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.getValue() &&
               !isShieldedLeaf() &&
               !UltrapeerSettings.DISABLE_ULTRAPEER_MODE.getValue() &&
               !isBehindProxy() &&
               minConnectTimePassed();
    }
    
    /**
     * @return whether the minimum time since we started trying to connect has passed
     */
    private boolean minConnectTimePassed() {
        if (!UltrapeerSettings.NEED_MIN_CONNECT_TIME.getValue()) {
            return true;
        }
        
        return Math.max(0,(System.currentTimeMillis() - _connectTime)) / 1000 
                >= UltrapeerSettings.MIN_CONNECT_TIME.getValue();
    }
    /**
     * @return if we are currently using a http or socks4/5 proxy to connect.
     */
    public boolean isBehindProxy() {
        return ConnectionSettings.CONNECTION_METHOD.getValue() != 
            ConnectionSettings.C_NO_PROXY;
    }
    
    /**
     * Tells whether or not we're actively being a supernode to anyone.
     */
    public boolean isActiveSupernode() {
        return !isShieldedLeaf() &&
               (_initializedClientConnections.size() > 0 ||
                _initializedConnections.size() > 0);
    }

    /**
     * Returns true if this is a leaf node with a connection to a ultrapeer.  It
     * is not required that the ultrapeer support query routing, though that is
     * generally the case.
     */
    public boolean isShieldedLeaf() {
        return _shieldedConnections != 0;
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
    boolean isConnectedTo(String hostName) {
        //A list of all connections, both initialized and
        //uninitialized, leaves and unrouted. 
        for(ManagedConnection mc : getConnections()) {
            if (mc.getAddress().equals(hostName))
                return true;
        }
        return false;
    }
    
    /**
     * Returns true if we're currently attempting to connect to a particular host.
     * This checks both the Ip & Port.
     */
    boolean isConnectingTo(IpPort host) {
        synchronized(this) {
            for(ConnectionFetcher next : _fetchers) {
                IpPort them = next.getIpPort();
                if(them != null && host.getAddress().equals(them.getAddress()) && host.getPort() == them.getPort())
                    return true;
            }
            for(ManagedConnection next : _initializingFetchedConnections) {
                if(host.getAddress().equals(next.getAddress()) && host.getPort() == next.getPort())
                    return true;
            }
            return false;
        }
    }

    /**
     * @return if there is already a connection established or establishing
     * to a host in the same class C network as the provided host.
     */
    private boolean isConnectingToClassC(IpPort host) {
        return ConnectionSettings.FILTER_CLASS_C.getValue() && 
        _classCNetworks.contains(NetworkUtils.getClassC(host.getInetAddress()));
    }
    
    /**
     * @return the number of connections, which is greater than or equal
     *  to the number of initialized connections.
     */
    public int getNumConnections() {
        return _connections.size();
    }

    /**
     * @return the number of initialized connections, which is less than or
     *  equals to the number of connections.
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
    public int getNumFreeLeafSlots() {
        if (isSupernode())
			return UltrapeerSettings.MAX_LEAVES.getValue() -
				getNumInitializedClientConnections();
        else
            return 0;
    }

    /**
     * @return the number of free leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireLeafSlots() {
        return Math.max(0,
                 getNumFreeLeafSlots() -
                 Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES - _nonLimeWireLeaves)
               );
    }


    /**
     * @return the number of free non-leaf slots.
     */
    public int getNumFreeNonLeafSlots() {
        return _preferredConnections - getNumInitializedConnections();
    }

    /**
     * @return the number of free non-leaf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireNonLeafSlots() {
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
     */
    public boolean isLocaleMatched() {
        return !ConnectionSettings.USE_LOCALE_PREF.getValue() ||
               _localeMatchingPeers != 0;
    }

    /**
     * @return the number of locale reserved slots to be filled
     *
     * An ultrapeer may not have Free LimeWire Non Leaf Slots but may still
     * have free slots that are reserved for locales
     */
    public int getNumLimeWireLocalePrefSlots() {
        return Math.max(0, ConnectionSettings.NUM_LOCALE_PREF.getValue()
                        - _localeMatchingPeers);
    }
    
    /**
     * Determines if we've reached our maximum number of preferred connections.
     */
    public boolean isFullyConnected() {
        return _initializedConnections.size() >= _preferredConnections;
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

    /**
     * Takes a snapshot of the upstream and downstream bandwidth since the last
     * call to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public void measureBandwidth() {
        float upstream=0.f;
        float downstream=0.f;
        for(ManagedConnection mc : getInitializedConnections()) {
            mc.measureBandwidth();
            upstream+=mc.getMeasuredUpstreamBandwidth();
            downstream+=mc.getMeasuredDownstreamBandwidth();
        }
        _measuredUpstreamBandwidth=upstream;
        _measuredDownstreamBandwidth=downstream;
    }

    /**
     * Returns the upstream bandwidth in kbytes/sec between the last two calls
     * to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredUpstreamBandwidth() {
        return _measuredUpstreamBandwidth;
    }

    /**
     * Returns the downstream bandwidth in kbytes/sec between the last two calls
     * to measureBandwidth.
     * @see BandwidthTracker#measureBandwidth
     */
    public float getMeasuredDownstreamBandwidth() {
        return _measuredDownstreamBandwidth;
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
    private HandshakeStatus allowConnection(ManagedConnection c) {
        if(!c.receivedHeaders())
            return HandshakeStatus.NO_HEADERS;
        
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
    public HandshakeStatus allowConnectionAsLeaf(HandshakeResponse hr) {
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
     public HandshakeStatus allowConnection(HandshakeResponse hr) {
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
        if (isShieldedLeaf())
            return false;

        //Do we have normal or leaf slots?
        return getNumInitializedConnections() < _preferredConnections
            || (isSupernode()
				&& getNumInitializedClientConnections() <
                UltrapeerSettings.MAX_LEAVES.getValue());
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
    public HandshakeStatus allowConnection(HandshakeResponse hr, boolean leaf) {
		// preferencing may not be active for testing purposes --
		// just return if it's not
		if(!ConnectionSettings.PREFERENCING_ACTIVE.getValue())
            return HandshakeStatus.OK;
		
		// If it has not said whether or not it's an Ultrapeer or a Leaf
		// (meaning it's an old-style connection), don't allow it.
		if(!hr.isLeaf() && !hr.isUltrapeer())
		    return HandshakeStatus.NO_X_ULTRAPEER;

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

		int limeAttempts = ConnectionSettings.LIME_ATTEMPTS.getValue();
		
        // Don't allow anything if disconnected.
        if (!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getValue() && _preferredConnections <= 0) {
            return HandshakeStatus.DISCONNECTED;
            // If a leaf (shielded or not), check rules as such.
        } else if (isShieldedLeaf() || !isSupernode()) {
            
            // require ultrapeer.
		    if(!hr.isUltrapeer()) {
                return HandshakeStatus.WE_ARE_LEAVES;
            }
		    
		    // If it's not good, or it's the first few attempts & not a LimeWire, 
		    // never allow it.
		    if(!hr.isGoodUltrapeer()) {
                return HandshakeStatus.NOT_GOOD_UP;
            } else if (_connectionAttempts < limeAttempts && !hr.isLimeWire()) {
                return HandshakeStatus.STARTING_LIMEWIRE;
		    // if we have slots, allow it.
		    } else if (_shieldedConnections < _preferredConnections) {                
		        // if it matched our preference, we don't need to preference
		        // anymore.
		        if(checkLocale(hr.getLocalePref()))
		            _needPref = false;

                // while idle, only allow LimeWire connections.
                if (isIdle()) {
                    if(hr.isLimeWire())
                        return HandshakeStatus.OK;
                    else
                        return HandshakeStatus.IDLE_LIMEWIRE;
                }

                return HandshakeStatus.OK;
            } else {
                // if we were still trying to get a locale connection
                // and this one matches, allow it, 'cause no one else matches.
                // (we would have turned _needPref off if someone matched.)
                if(_needPref && checkLocale(hr.getLocalePref())) {
                    _needPref = false;
                    return HandshakeStatus.OK;
                }
                
                // don't allow it.
                return HandshakeStatus.TOO_MANY_UPS;
            }
		} else if (hr.isLeaf() || leaf) {
		    // no leaf connections if we're a leaf.
		    if(isShieldedLeaf() || !isSupernode()) {
                return HandshakeStatus.WE_ARE_LEAVES;
            }

            if(!allowUltrapeer2LeafConnection(hr)) {
                return HandshakeStatus.NOT_ALLOWED_LEAF;
            }

            int leaves = getNumInitializedClientConnections();
            int nonLimeWireLeaves = _nonLimeWireLeaves;

            // Reserve RESERVED_NON_LIMEWIRE_LEAVES slots
            // for non-limewire leaves to ensure that the network
            // is well connected.
            if(!hr.isLimeWire()) {
                if( leaves < UltrapeerSettings.MAX_LEAVES.getValue() &&
                    nonLimeWireLeaves < RESERVED_NON_LIMEWIRE_LEAVES ) {
                    return HandshakeStatus.OK;
                }
            }
            
            // Only allow good guys.
            if(!hr.isGoodLeaf()) {
                return HandshakeStatus.NOT_GOOD_LEAF;
            }
                
            // if we have slots, allow it.
            if((leaves +
                Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES - nonLimeWireLeaves)
               ) < UltrapeerSettings.MAX_LEAVES.getValue()) {
                return HandshakeStatus.OK;
            } else {
                return HandshakeStatus.TOO_MANY_LEAF;
            }
        } else if (hr.isUltrapeer()) {
            // Note that this code is NEVER CALLED when we are a leaf.
            // As a leaf, we will allow however many ultrapeers we happen
            // to connect to.
            // Thus, we only worry about the case we're connecting to
            // another ultrapeer (internally or externally generated)
            
            int peers = getNumInitializedConnections();
            int nonLimeWirePeers = _nonLimeWirePeers;
            int locale_num = 0;
            
            if(!allowUltrapeer2UltrapeerConnection(hr)) {
                return HandshakeStatus.NOT_ALLOWED_UP;
            }
            
            if(ConnectionSettings.USE_LOCALE_PREF.getValue()) {
                //if locale matches and we haven't satisfied the
                //locale reservation then we force return a true
                if(checkLocale(hr.getLocalePref()) &&
                   _localeMatchingPeers
                   < ConnectionSettings.NUM_LOCALE_PREF.getValue()) {
                    return HandshakeStatus.OK;
                }

                //this number will be used at the end to figure out
                //if the connection should be allowed
                //(the reserved slots is to make sure we have at least
                // NUM_LOCALE_PREF locale connections but we could have more so
                // we get the max)
                locale_num = getNumLimeWireLocalePrefSlots();
            }

            // If it's not a LimeWire, we'll allow it up to the ratio of
            // MIN_NON_LIME_PEERS.  If we've exceeded that ratio, we'll allow it
            // if it's good and is up to MAX_NON_LIME_PEERS.
            // If it is a LimeWire, only allow it if we still have space left for
            // non-limes and it's good.
            if(!hr.isLimeWire()) {
                double nonLimeRatio = ((double)nonLimeWirePeers) / _preferredConnections;
                if (nonLimeRatio < ConnectionSettings.MIN_NON_LIME_PEERS.getValue())
                    return HandshakeStatus.OK;
                if(!hr.isGoodUltrapeer()) {
                    return HandshakeStatus.NOT_GOOD_UP;
                } else if(nonLimeRatio < ConnectionSettings.MAX_NON_LIME_PEERS.getValue()) {
                    return HandshakeStatus.OK;
                } else {
                    return HandshakeStatus.NON_LIME_RATIO;
                }
            } else {
                int minNonLime = (int)(ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections);
                if(!hr.isGoodUltrapeer()) {
                    return HandshakeStatus.NOT_GOOD_UP;
                } else if((peers + 
                           Math.max(0,minNonLime - nonLimeWirePeers) + 
                           locale_num
                          ) < _preferredConnections) {
                    return HandshakeStatus.OK;
                } else {
                    return HandshakeStatus.NO_LIME_SLOTS;
                }
            }
        }
		return HandshakeStatus.UNKNOWN;
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
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue();
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
        String[] bad = ConnectionSettings.EVIL_HOSTS.getValue();
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
        for(ManagedConnection mc : _initializedConnections) {
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
        for(ManagedConnection mc : _initializedConnections) {
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
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    public List<ManagedConnection> getInitializedConnections() {
        return _initializedConnections;
    }

    /**
     * return a list of initialized connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List<ManagedConnection> getInitializedConnectionsMatchLocale(String loc) {
        List<ManagedConnection> matches = new LinkedList<ManagedConnection>();
        for(ManagedConnection conn : _initializedConnections) {
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }

    /**
     * @requires returned value not modified
     * @effects returns a list of this' initialized connections.  <b>This
     *  exposes the representation of this, but is needed in some cases
     *  as an optimization.</b>  All lookup values in the returned value
     *  are guaranteed to run in linear time.
     */
    public List<ManagedConnection> getInitializedClientConnections() {
        return _initializedClientConnections;
    }

    /**
     * return a list of initialized client connection that matches the parameter
     * String loc.
     * create a new linkedlist to return.
     */
    public List<ManagedConnection> getInitializedClientConnectionsMatchLocale(String loc) {
    	List<ManagedConnection>  matches = new LinkedList<ManagedConnection>();
        for(ManagedConnection conn : _initializedClientConnections) {
            if(loc.equals(conn.getLocalePref()))
                matches.add(conn);
        }
        return matches;
    }

    /**
     * @return all of this' connections.
     */
    public List<ManagedConnection> getConnections() {
        return _connections;
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
    public Set<IpPort> getPushProxies() {
        if (isShieldedLeaf()) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for proxy support is cached boolean
            // value
            Set<IpPort> proxies = new IpPortSet();
            for(ManagedConnection currMC : getInitializedConnections()) {
                if(proxies.size() >= 4)
                    break;
                if (currMC.isPushProxy())
                    proxies.add(currMC);
            }
            return proxies;
        }

        return Collections.emptySet();
    }

    /**
     * Sends a TCPConnectBack request to (up to) 2 connected Ultrapeers.
     * @returns false if no requests were sent, otherwise true.
     */
    public boolean sendTCPConnectBackRequests() {
        int sent = 0;
        
        List<ManagedConnection> peers = new ArrayList<ManagedConnection>(getInitializedConnections());
        Collections.shuffle(peers);
        for (Iterator<ManagedConnection> iter = peers.iterator(); iter.hasNext();) {
            ManagedConnection currMC = iter.next();
            if (currMC.remoteHostSupportsTCPRedirect() < 0)
                iter.remove();
        }
        
        if (peers.size() == 1) {
            ManagedConnection myConn = peers.get(0);
            for (int i = 0; i < CONNECT_BACK_REDUNDANT_REQUESTS; i++) {
                // This is inside to generate a different GUID for each request.
                Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
                myConn.send(cb);
                sent++;
            }
        } else {
            final Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
            for(ManagedConnection currMC : peers) {
                if(sent >= 5)
                    break;
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
        final Message cb = new UDPConnectBackVendorMessage(RouterService.getPort(), cbGuid);
        List<ManagedConnection> peers = new ArrayList<ManagedConnection>(getInitializedConnections());
        Collections.shuffle(peers);
        for(ManagedConnection currMC : peers) {
            if(sent >= 5)
                break;
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
        if (isShieldedLeaf()) {
            // this should be fast since leaves don't maintain a lot of
            // connections and the test for query status response is a cached
            // value
            for(ManagedConnection currMC : getInitializedConnections()) {
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
        for(ManagedConnection connection : _initializedConnections) {
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
	public List<ManagedConnection> getConnectedGUESSUltrapeers() {
        List<ManagedConnection> retList = new ArrayList<ManagedConnection>();
        for(ManagedConnection connection : _initializedConnections) {
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
    private void connectionInitializing(ManagedConnection c) {
        //REPLACE _connections with the list _connections+[c]
        List<ManagedConnection> newConnections=new ArrayList<ManagedConnection>(_connections);
        newConnections.add(c);
        _connections = Collections.unmodifiableList(newConnections);
        _classCNetworks.add(NetworkUtils.getClassC(c.getInetAddress()));
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
                List<ManagedConnection> newConnections=new ArrayList<ManagedConnection>(_initializedConnections);
                newConnections.add(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                
                if(c.isClientSupernodeConnection()) {
                	killPeerConnections(); // clean up any extraneus peer conns.
                    _shieldedConnections++;
                }
                if(!c.isLimeWire())
                    _nonLimeWirePeers++;
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers++;
            } else {
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections+[c]
                List<ManagedConnection> newConnections
                    =new ArrayList<ManagedConnection>(_initializedClientConnections);
                newConnections.add(c);
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves++;
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
     * Like allowConnection, but more more strict.
     * In addition to allowConnection, this checks to see if the connection
     * is a leaf, and if so only allows it if we said we're it's supernode.
     * It also makes sure that we don't have any duplicate connections to this
     * particular host.  (Duplicate connections are checked by IP address and
     * 'listen port', if a listen port was specified.)
     * 
     * @return whether the connection should be allowed 
     */
    private boolean allowInitializedConnection(ManagedConnection c) {
    	if ((isShieldedLeaf() || !isSupernode()) && !c.isClientSupernodeConnection())
    		return false;
        
        List<ManagedConnection> connections = getConnections();
        int listenPort = c.getListeningPort();
        String addr = c.getAddress();
        
        for(int i = 0; i < connections.size(); i++ ) {
            ManagedConnection mc = connections.get(i);
            if(mc == c)
                continue;
            if(!ConnectionSettings.ALLOW_DUPLICATE.getValue() &&
            		addr.equals(mc.getAddress())) {
                int mcLP = mc.getListeningPort();
                // If either side didn't advertise a listening port,
                // or both did and they're the same, then because the
                // addresses are also equal, disallow it.
                // -1 == unknown, 0 == connecting w/o NIOSocket
                if(listenPort == -1 || listenPort == 0 ||
                   mcLP == -1 || mcLP == 0 || mcLP == listenPort)
                    return false;
            }
        }
        
    	return allowConnection(c.headers()).isAcceptable();
    }
    
    /**
     * removes any supernode->supernode connections
     */
    private void killPeerConnections() {
    	List<ManagedConnection> conns = _initializedConnections;
        for(ManagedConnection con : conns) {
			if (con.isSupernodeSupernodeConnection()) 
				removeInternal(con);
		}
    }
    
    /**
     * Iterates over all the connections and sends the updated CapabilitiesVM
     * down every one of them.
     */
    public void sendUpdatedCapabilities() {        
        for(Connection c : getInitializedConnections())
            c.sendUpdatedCapabilities();
        for(Connection c : getInitializedClientConnections())
            c.sendUpdatedCapabilities();
    }

    /**
     * Disconnects from the network.  Closes all connections and sets
     * the number of connections to zero.
     * 
     * @param willTryToReconnect Whether or not this is only a temporary disconnection
     */
    public synchronized void disconnect(boolean willTryToReconnect) {
        if(_disconnectTime == 0) {
            long averageUptime = getCurrentAverageUptime();
            int totalConnections = Math.max(1, ApplicationSettings.TOTAL_CONNECTIONS.getValue() + 1);
            long totalConnectTime = averageUptime * totalConnections; 
            ApplicationSettings.TOTAL_CONNECTION_TIME.setValue(totalConnectTime);
            ApplicationSettings.TOTAL_CONNECTIONS.setValue(totalConnections);
            ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(averageUptime);
        }
        
        _disconnectTime = System.currentTimeMillis();
        _connectTime = Long.MAX_VALUE;
        _preferredConnections = 0;
        adjustConnectionFetchers(); // kill them all
        
        //2. Remove all connections.
        for(ManagedConnection c : getConnections()) {
            remove(c);
            //add the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                //add to catcher with the locale info.
                _catcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                          c.getPort()), true, c.getLocalePref());
            }
        }
        
        if(!willTryToReconnect) {
            dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                    ConnectionLifecycleEvent.EventType.DISCONNECTED, null));
        }
    }
    
    /**
     * Returns this node's average connection time - in ms - including the current session.
     * 
     */
    public synchronized long getCurrentAverageUptime() {
        long currentAverage = 0;
        long now = System.currentTimeMillis();
        long sessionTime = Math.max(0,now - _connectTime); //in ms
        int totalConnections = ApplicationSettings.TOTAL_CONNECTIONS.getValue();

        if(sessionTime != 0) { //else don't count current session
            totalConnections+=1;
        }
        
        long totalConnectTime = Math.max(0, 
                ApplicationSettings.TOTAL_CONNECTION_TIME.getValue() + sessionTime);
        
        currentAverage = totalConnectTime/Math.max(1,totalConnections);
        return currentAverage;
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
                List<ManagedConnection> newConnections=new ArrayList<ManagedConnection>();
                newConnections.addAll(_initializedConnections);
                newConnections.remove(c);
                _initializedConnections =
                    Collections.unmodifiableList(newConnections);
                //maintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections--;
                if(!c.isLimeWire())
                    _nonLimeWirePeers--;
                if(checkLocale(c.getLocalePref()))
                    _localeMatchingPeers--;
            }
        }else{
            //check in _initializedClientConnections
            int i=_initializedClientConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initializedClientConnections with the list
                //_initializedClientConnections-[c]
                List<ManagedConnection> newConnections=new ArrayList<ManagedConnection>();
                newConnections.addAll(_initializedClientConnections);
                newConnections.remove(c);
                _initializedClientConnections =
                    Collections.unmodifiableList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLeaves--;
            }
        }

        // 1b) Remove from the all connections list and clean up the
        // stuff associated all connections
        int i=_connections.indexOf(c);
        if (i != -1) {
            //REPLACE _connections with the list _connections-[c]
            List<ManagedConnection> newConnections=new ArrayList<ManagedConnection>(_connections);
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
        dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this, 
                EventType.CONNECTION_CLOSED,
                c));

        // 5) Clean up Unicaster
        QueryUnicaster.instance().purgeQuery(c);
        
        // 6) Remove from list of class C networks
        _classCNetworks.remove(NetworkUtils.getClassC(c.getInetAddress()));
    }
    
    /**
     * Stabilizes connections by removing extraneous ones.
     *
     * This will remove the connections that we've been connected to
     * for the shortest amount of time.
     */
    private synchronized void stabilizeConnections() {
        while(getNumInitializedConnections() > _preferredConnections) {
            ManagedConnection newest = null;
            for(ManagedConnection c : _initializedConnections) {
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
     * Starts or stops connection fetchers to maintain the invariant that numConnections + numFetchers >=
     * _preferredConnections
     * 
     * _preferredConnections - numConnections - numFetchers is called the need.
     * This method is called whenever the need changes:
     *  1. setPreferredConnections() -- _preferredConnections changes
     *  2. remove(Connection) -- numConnections drops.
     *  3. initializeExternallyGeneratedConnection() -- numConnections rises.
     *  4. initialization error in initializeFetchedConnection() -- numConnections drops when removeInternal is called.
     * Note that adjustConnectionFetchers is not called when a connection is successfully fetched from the host catcher.
     * numConnections rises, but numFetchers drops, so need is unchanged.
     * 
     * Only call this method when the monitor is held.
     */
    private void adjustConnectionFetchers() {
        if(ConnectionSettings.USE_LOCALE_PREF.getValue()) {
            startDedicatedLocaleFetcher();
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
                ConnectionSettings.MIN_NON_LIME_PEERS.getValue() * _preferredConnections;
        }

        int need = Math.min(10, multiple*neededConnections)
                 - _fetchers.size()
                 - _initializingFetchedConnections.size();

        // do not open more sockets than we can
        need = Math.min(need, Sockets.getNumAllowedSockets());
        
        // Build up lists of what we need to connect to & remove from connecting.
        
        // Start connection fetchers as necessary
        List<ConnectionFetcher> fetchers = Collections.emptyList();
        if (need > 0) {
            fetchers = new ArrayList<ConnectionFetcher>(need);
            while (need > 0) {
                // This kicks off the thread for the fetcher
                ConnectionFetcher fetcher = new ConnectionFetcher();
                fetchers.add(fetcher);
                need--;
            }
            _fetchers.addAll(fetchers);
            dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                    EventType.CONNECTING, null));
        } 

        // Stop ConnectionFetchers as necessary, but it's possible there
        // aren't enough fetchers to stop.  In this case, close some of the
        // connections started by ConnectionFetchers.
        int lastFetcherIndex = _fetchers.size();
        List<Object> extras = new ArrayList<Object>();
        while((need < 0) && (lastFetcherIndex > 0)) {
            ConnectionFetcher fetcher = _fetchers.remove(--lastFetcherIndex);
            need++;
            extras.add(fetcher);
        }
        int lastInitializingConnectionIndex = _initializingFetchedConnections.size();
        while((need < 0) && (lastInitializingConnectionIndex > 0)) {
            ManagedConnection connection = 
                _initializingFetchedConnections.remove(--lastInitializingConnectionIndex);
            need++;
            extras.add(connection);
        }
        
        // Now connect'm.
        for(int i = fetchers.size() - 1; i >= 0; i--) {
            ConnectionFetcher fetcher = fetchers.remove(i);
            fetcher.connect();
        }
        
        // And delete extras.
        for(int i = extras.size() - 1; i >= 0; i--) {
            Object next = extras.remove(i);
            if(next instanceof ConnectionFetcher) {
               ((ConnectionFetcher)next).stopConnecting(); 
            } else {
                removeInternal((ManagedConnection)next);
            }
        }
    }
    
    /** Starts the dedicated locale ConnectionFetcher, if necessary */
    private void startDedicatedLocaleFetcher() {
        // if it's a leaf and locale preferencing is on
        // we will create a dedicated preference fetcher
        // that tries to fetch a connection that matches the
        // clients locale
        if (RouterService.isShieldedLeaf() && _needPref && !_needPrefInterrupterScheduled
                && _dedicatedPrefFetcher == null) {
            _dedicatedPrefFetcher = new ConnectionFetcher(true);
            _dedicatedPrefFetcher.connect();
            Runnable interrupted = new Runnable() {
                public void run() {
                    synchronized (ConnectionManager.this) {
                        // always finish once this runs.
                        _needPref = false;

                        if (_dedicatedPrefFetcher == null)
                            return;
                        _dedicatedPrefFetcher.stopConnecting();
                        _dedicatedPrefFetcher = null;
                    }
                }
            };
            _needPrefInterrupterScheduled = true;
            // shut off this guy if he didn't have any luck
            RouterService.schedule(interrupted, 15 * 1000, 0);
        }
    }    

    /**
     * Begins initializing fetched connections.
     * This will maintain all class invariants and then wait to hear
     * back from the ConnectionFetcher after connecting succeeds or fails.
     */
    private void initializeFetchedConnection(ManagedConnection mc,
                                             ConnectionFetcher fetcher) {
        
        synchronized(this) {
            if(fetcher.isPrematurelyStopped()) {
                fetcher.finish();
                return;
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
        dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                EventType.CONNECTION_INITIALIZING,
                mc));
     
        try {
            mc.initialize(fetcher);
        } catch(IOException e) {
            cleanupBrokenFetchedConnection(mc);
        }
    }
    
    /**
     * Cleans up references to the given connection.
     * 
     * This removes the now-dead connection from the list of initializing fetched
     * connections, ensures that the connection is closed and nothing can
     * be routed to it, adjusts connection fetchers so as to spark a new fetcher
     * if needed, and processes the headers of the connection for addition to the local
     * hostcache.
     * @param mc
     */
    private void cleanupBrokenFetchedConnection(ManagedConnection mc) {
        synchronized (this) {
            _initializingFetchedConnections.remove(mc);
            removeInternal(mc);
            // We've removed a connection, so the need for connections went
            // up. We may need to launch a fetcher.
            adjustConnectionFetchers();
        }
        processConnectionHeaders(mc);
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
        if(headers == null)
            return;
        //update the addresses in the host cache (in case we received some
        //in the headers)
        updateHostCache(connection.headers());

        //get remote address.  If the more modern "Listen-IP" header is
        //not included, try the old-fashioned "X-My-Address".
        String remoteAddress = headers.getProperty(HeaderNames.LISTEN_IP);
        if (remoteAddress==null)
            remoteAddress = headers.getProperty(HeaderNames.X_MY_ADDRESS);

        //set the remote port if not outgoing connection (as for the outgoing
        //connection, we already know the port at which remote host listens)
        if((remoteAddress != null) && (!connection.isOutgoing())) {
            int colonIndex = remoteAddress.indexOf(':');
            if(colonIndex == -1) 
                return;
            colonIndex++;
            if(colonIndex > remoteAddress.length())
                return;
            try {
                int port = Integer.parseInt(remoteAddress.substring(colonIndex).trim());
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

        if (UltrapeerSettings.FORCE_ULTRAPEER_MODE.getValue() || isActiveSupernode())
            return false;
        else if(NodeAssigner.isTooGoodUltrapeerToPassUp() && _leafTries < _demotionLimit)
			return false;
        else
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
		disconnect(true);
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

        List<Endpoint> hosts = new ArrayList<Endpoint>(st.countTokens());
        while(st.hasMoreTokens()){
            String address = st.nextToken().trim();
            try {
                hosts.add(new Endpoint(address));
            } catch(IllegalArgumentException iae){
                continue;
            }
        }
        _catcher.add(ConnectionSettings.FILTER_CLASS_C.getValue() ? 
                NetworkUtils.filterOnePerClassC(hosts) :
                    hosts);        
    }



    /**
     * Initializes an outgoing connection created by createConnection or any
     * incomingConnection.  If this is an incoming connection and there are no
     * slots available, rejects it and throws IOException.
     *
     * @throws IOException on failure.  No cleanup is necessary if this happens.
     */
    private void initializeExternallyGeneratedConnection(ManagedConnection c, GnetConnectObserver observer)
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
            dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                    EventType.CONNECTION_INITIALIZING,
                    c));
        }

        try {
            c.initialize(observer);
        } catch(IOException e) {
            cleanupBrokenExternallyGeneratedConnection(c);
            throw e;
        }

        // If observer is null, we blocked while initializing.
        // Otherwise, the observer will be notified on completion and do their own thing.
        if(observer == null)
            completeInitializeExternallyGeneratedConnection(c);
    }
    
    /**
     * Cleans up a connection that couldn't be initialized.
     * @param c
     */
    private void cleanupBrokenExternallyGeneratedConnection(ManagedConnection c) {
        remove(c);
        processConnectionHeaders(c);
    }
    
    /**
     * Completes the process of initializing an externally generated connection.
     * 
     * @param c
     * @throws IOException
     * 
     * @return true if the connection should continue, false if it was closed
     */
    private boolean completeInitializeExternallyGeneratedConnection(ManagedConnection c) throws IOException {
        processConnectionHeaders(c);
        
        // If there's not space for the connection, destroy it.
        // It really should have been destroyed earlier, but this is just in case.
        if (!c.isOutgoing() && !allowConnection(c).isAcceptable()) {
            // No need to remove, since it hasn't been added to any lists.
            throw new IOException("No space for connection");
        }

        // For incoming connections, add it to the GUI. For outgoing connections
        // this was done at the top of the method. See note there.
        if (!c.isOutgoing()) {
            synchronized (this) {
                connectionInitializingIncoming(c);
                // We've added a connection, so the need for connections went
                // down.
                adjustConnectionFetchers();
            }
            dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                    EventType.CONNECTION_INITIALIZING,
                    c));
        }

        return completeConnectionInitialization(c, false);
    }

    /**
     * Performs the steps necessary to complete connection initialization.
     * 
     * @param mc the <tt>ManagedConnection</tt> to finish initializing
     * @param fetched Specifies whether or not this connection is was fetched by a connection fetcher. If so, this
     *            removes that connection from the list of fetched connections being initialized, keeping the connection
     *            fetcher data in sync
     *            
     * @return true if the connection should continue, false if it was closed
     */
    private boolean completeConnectionInitialization(ManagedConnection mc,
                                                  boolean fetched) {
        synchronized(this) {
            if(fetched) {
                _initializingFetchedConnections.remove(mc);
            }
            // If the connection was killed while initializing, we shouldn't
            // announce its initialization
            boolean connectionOpen = connectionInitialized(mc);
            if(connectionOpen) {
                dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                        EventType.CONNECTION_INITIALIZED,
                        mc));
                setPreferredConnections();
                if (_initializedConnections.size() >= getPreferredConnectionCount())
                    dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                            EventType.CONNECTED,
                            mc));
                    
            }
            return connectionOpen;
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
        return
         _preferredConnections == ConnectionSettings.IDLE_CONNECTIONS.getValue();
    }

    /**
     * Sets the maximum number of connections we'll maintain.
    */
    private void setPreferredConnections() {
        // if we're disconnected, do nothing.
        if(!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getValue() &&
           _disconnectTime != 0)
            return;

        int oldPreferred = _preferredConnections;

        if(isSupernode())
            _preferredConnections = ConnectionSettings.NUM_CONNECTIONS.getValue();
        else if(isIdle())
            _preferredConnections = ConnectionSettings.IDLE_CONNECTIONS.getValue();
        else
            _preferredConnections = PREFERRED_CONNECTIONS_FOR_LEAF;

        if(oldPreferred != _preferredConnections)
            stabilizeConnections();
    }

    /**
     * Determines if we're idle long enough to change the number of connections.
     */
    private boolean isIdle() {
        return SystemUtils.getIdleTime() >= MINIMUM_IDLE_TIME;
    }


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
				if(_doInitialization)
					initializeExternallyGeneratedConnection(_connection, null);
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
		if(conn.isGUESSUltrapeer())
			QueryUnicaster.instance().addUnicastEndpoint(conn.getInetAddress(), conn.getPort());

		// this can throw IOException
		conn.loopForMessages();
	}
    
    /**
     * Asynchronous GnetConnectObserver for externally generated connections.
     * Not as robust as ConnectionFetcher because less accounting is needed.
     */
    private class IncomingGNetObserver implements GnetConnectObserver {
        private ManagedConnection connection;
        IncomingGNetObserver(ManagedConnection connection) {
            this.connection = connection;
        }

        public void handleConnect() {
            try {
                if(completeInitializeExternallyGeneratedConnection(connection))
                    startConnection(connection);
            } catch(IOException ignored) {
                LOG.warn("Failed to complete initialization", ignored);
            }
        }
        
        public void handleBadHandshake() {
            shutdown();
        }

        public void handleNoGnutellaOk(int code, String msg) {
            shutdown();
        }

        public void shutdown() {
            cleanupBrokenExternallyGeneratedConnection(connection);
        }
        
    }

    /**
     * Asynchronously fetches a connection from hostcatcher, then does
     * then initialization and message loop.
     *
     * The ConnectionFetcher is responsible for recording its instantiation
     * by adding itself to the fetchers list.  It is responsible  for recording
     * its death by removing itself from the fetchers list only if fetching proceeded
     * beyond the 'connect' stage.  That is, if a connect attempt is performed and failed
     * for any reason, this must clean itself up.
     */
    private class ConnectionFetcher implements GnetConnectObserver, HostCatcher.EndpointObserver {
        // set if this connectionfetcher is a preferencing fetcher
        private boolean _pref = false;
        private ManagedConnection connection;
        private Endpoint endpoint;
        private volatile boolean stoppedEarly = false;

        public ConnectionFetcher() {
            this(false);
        }

        public ConnectionFetcher(boolean pref) {
            _pref = pref;
        }
        
        IpPort getIpPort() {
            if(connection != null)
                return connection;
            else if(endpoint != null)
                return endpoint;
            else
                return null;
        }

        /** Starts the process of connecting to an arbitary endpoint. */
        public void connect() {
            _catcher.getAnEndpoint(this);
        }
        
        /** 
         * Marks this fetcher as not wanting to connect.
         * It is entirely possible that this fetcher has proceeded to connect
         * already.  If that's the case, this call essentially does nothing.
         */
        public void stopConnecting() {
            stoppedEarly = true;
            _catcher.removeEndpointObserver(this);
        }
        
        /** Returns whether or not we were told to stop early. */
        public boolean isPrematurelyStopped() {
            return stoppedEarly;
        }
        
        /** Does nothing right now. */
        public void finish() {
        }

        /** Returns true if we are able to make a connection attempt to this host. */
        private boolean isConnectableHost(IpPort host) {
            return RouterService.getIpFilter().allow(host)
                && !isConnectedTo(host.getAddress()) 
                && !isConnectingTo(host)
                && (!isShieldedLeaf() || !isConnectingToClassC(host));
        }
        
        /** Callback that an endpoint is available for connecting. */
        public void handleEndpoint(Endpoint endpoint) {
            Assert.that(endpoint != null);
            
            // If this was an invalid endpoint, try again.
            while(!isConnectableHost(endpoint)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Ignoring unconnectable host: " + endpoint);
                endpoint = _catcher.getAnEndpointImmediate(this);
                if(endpoint == null) {
                    LOG.debug("No hosts available, waiting on a new one...");
                    return; // if we didn't get one immediate, the callback is scheduled
                }
            }

            this.endpoint = endpoint;
            connection = new ManagedConnection(endpoint.getAddress(), endpoint.getPort());
            connection.setLocalePreferencing(_pref);
            doConnectionCheck();
            _connectionAttempts++;
            initializeFetchedConnection(connection, this);
        }
        
        /** Callback that handshaking has succeeded and we're all connected and ready. */
        public void handleConnect() {
            boolean stillOpen = completeConnectionInitialization(connection, true);
            processConnectionHeaders(connection);
            _lastSuccessfulConnect = System.currentTimeMillis();
            _catcher.doneWithConnect(endpoint, true);
            if(_pref)
                _needPref = false;
            
            try {
                if(stillOpen)
                    startConnection(connection);
            } catch(IOException ignored) {}
        }
        
        /** Callback that a connect failed. */
        public void shutdown() {
            cleanupBrokenFetchedConnection(connection);            
            _catcher.doneWithConnect(endpoint, false);
            _catcher.expireHost(endpoint);
        }
        
        /** Callback that handshaking failed. */
        public void handleBadHandshake() {
            shutdown();
        }
        
        /** Callback that connecting worked, but we got something other than a Gnutella OK */
        public void handleNoGnutellaOk(int code, String msg) {
            cleanupBrokenFetchedConnection(connection);
            _lastSuccessfulConnect = System.currentTimeMillis();
            if (code == HandshakeResponse.LOCALE_NO_MATCH) {
                // Failures because of locale aren't really a failure.
                _catcher.add(endpoint, true, connection.getLocalePref());
            } else {
                _catcher.doneWithConnect(endpoint, true);
                _catcher.putHostOnProbation(endpoint);
            }
        }

        /** Checks to see if we need to check for a live connection. */
        private void doConnectionCheck() {
            // If we've been trying to connect for awhile, check to make
            // sure the user's internet connection is live. We only do
            // this if we're not already connected, have not made any
            // successful connections recently, and have not checked the
            // user's connection in the last little while or have very
            // few hosts left to try.
            long curTime = System.currentTimeMillis();
            if (!isConnected() && _connectionAttempts > 40
                    && ((curTime - _lastSuccessfulConnect) > 4000)
                    && ((curTime - _lastConnectionCheck) > 60 * 60 * 1000)) {
                _connectionAttempts = 0;
                _lastConnectionCheck = curTime;
                LOG.debug("checking for live connection");
                ConnectionChecker.checkForLiveConnection();
            }
        }
    }

    /**
     * This method notifies the connection manager that the user does not have
     * a live connection to the Internet to the best of our determination.
     * In this case, we notify the user with a message and maintain any
     * Gnutella hosts we have already tried instead of discarding them.
     */
    public void noInternetConnection() {

        
        // Notify the user that they have no internet connection and that
        // we will automatically retry
        dispatchEvent(new ConnectionLifecycleEvent(ConnectionManager.this,
                EventType.NO_INTERNET));

        if(_automaticallyConnecting) {
            // We've already notified the user about their connection and we're
            // alread retrying automatically, so just return.
            return;
        }

        // Kill all of the ConnectionFetchers.
        disconnect(false);
        
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
    private synchronized void recoverHosts() {
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
    
    
    /**
     * Registers a listener for ConnectionLifeCycleEvents
     */
    public void addEventListener(ConnectionLifecycleListener listener) {
        if(!connectionLifeCycleListeners.addIfAbsent(listener)) {
            throw new IllegalArgumentException("Listener "+listener+" already registered");
        }
    }

    /**
     * Dispatches a ConnectionLifecycleEvent to any registered listeners 
     */
    public void dispatchEvent(ConnectionLifecycleEvent event) {
        for(ConnectionLifecycleListener listener : connectionLifeCycleListeners) {
            listener.handleConnectionLifecycleEvent(event);
        }
    }

    /**
     * unregisters a listener for ConnectionLifeCycleEvents
     */
    public void removeEventListener(ConnectionLifecycleListener listener) {
        connectionLifeCycleListeners.remove(listener);
    }

    /**
     * Count how many connections have already received N messages
     */
    public int countConnectionsWithNMessages(int messageThreshold) {
        int count = 0;
        int msgs; 

        // Count the messages on initialized connections
        for(ManagedConnection c : getInitializedConnections()) {
            msgs = c.getNumMessagesSent();
            msgs += c.getNumMessagesReceived();
            if ( msgs > messageThreshold )
                count++;
        }
        return count;
    }
    
    /**
     * Count up all the messages on active connections
     */
    public int getActiveConnectionMessages() {
        int count = 0;

        // Count the messages on initialized connections
        for(ManagedConnection c : getInitializedConnections()) {
            count += c.getNumMessagesSent();
            count += c.getNumMessagesReceived();
        }
        return count;
    }
    
}
