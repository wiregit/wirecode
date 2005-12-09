pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.net.Socket;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.List;
import jbva.util.Properties;
import jbva.util.Set;
import jbva.util.StringTokenizer;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.connection.ConnectionChecker;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.handshaking.BadHandshakeException;
import com.limegroup.gnutellb.handshaking.HandshakeResponse;
import com.limegroup.gnutellb.handshaking.HeaderNames;
import com.limegroup.gnutellb.handshaking.NoGnutellaOkException;
import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingRequest;
import com.limegroup.gnutellb.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutellb.messages.vendor.TCPConnectBackVendorMessage;
import com.limegroup.gnutellb.messages.vendor.UDPConnectBackVendorMessage;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.QuestionsHandler;
import com.limegroup.gnutellb.settings.UltrapeerSettings;
import com.limegroup.gnutellb.util.IpPortSet;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.Sockets;
import com.limegroup.gnutellb.util.SystemUtils;

/**
 * The list of bll ManagedConnection's.  Provides a factory method for creating
 * user-requested outgoing connections, bccepts incoming connections, and
 * fetches "butomatic" outgoing connections as needed.  Creates threads for
 * hbndling these connections when appropriate.
 *
 * Becbuse this is the only list of all connections, it plays an important role
 * in messbge broadcasting.  For this reason, the code is highly tuned to avoid
 * locking in the getInitiblizedConnections() methods.  Adding and removing
 * connections is b slower operation.<p>
 *
 * LimeWire follows the following connection strbtegy:<br>
 * As b leaf, LimeWire will ONLY connect to 'good' Ultrapeers.  The definition
 * of good is constbntly changing.  For a current view of 'good', review
 * HbndshakeResponse.isGoodUltrapeer().  LimeWire leaves will NOT deny
 * b connection to an ultrapeer even if they've reached their maximum
 * desired number of connections (currently 4).  This mebns that if 5
 * connections resolve simultbneously, the leaf will remain connected to all 5.
 * <br>
 * As bn Ultrapeer, LimeWire will seek outgoing connections for 5 less than
 * the number of it's desired peer slots.  This is done so thbt newcomers
 * on the network hbve a better chance of finding an ultrapeer with a slot
 * open.  LimeWire ultrbpeers will allow ANY other ultrapeer to connect to it,
 * bnd to ensure that the network does not become too LimeWire-centric, it
 * reserves 3 slots for non-LimeWire peers.  LimeWire ultrbpeers will allow
 * ANY lebf to connect, so long as there are atleast 15 slots open.  Beyond
 * thbt number, LimeWire will only allow 'good' leaves.  To see what consitutes
 * b good leave, view HandshakeResponse.isGoodLeaf().  To ensure that the
 * network does not rembin too LimeWire-centric, it reserves 3 slots for
 * non-LimeWire lebves.<p>
 *
 * ConnectionMbnager has methods to get up and downstream bandwidth, but it
 * doesn't quite fit the BbndwidthTracker interface.
 */
public clbss ConnectionManager {

    /**
     * Timestbmp for the last time the user selected to disconnect.
     */
    privbte volatile long _disconnectTime = -1;
    
    /**
     * Timestbmp for the last time we started trying to connect
     */
    privbte volatile long _connectTime = Long.MAX_VALUE;

    /**
     * Timestbmp for the time we began automatically connecting.  We stop
     * trying to butomatically connect if the user has disconnected since that
     * time.
     */
    privbte volatile long _automaticConnectTime = 0;

    /**
     * Flbg for whether or not the auto-connection process is in effect.
     */
    privbte volatile boolean _automaticallyConnecting;

    /**
     * Timestbmp of our last successful connection.
     */
    privbte volatile long _lastSuccessfulConnect = 0;

    /**
     * Timestbmp of the last time we checked to verify that the user has a live
     * Internet connection.
     */
    privbte volatile long _lastConnectionCheck = 0;


    /**
     * Counter for the number of connection bttempts we've made.
     */
    privbte volatile static int _connectionAttempts;

    privbte static final Log LOG = LogFactory.getLog(ConnectionManager.class);

    /**
     * The number of connections lebves should maintain to Ultrapeers.
     */
    public stbtic final int PREFERRED_CONNECTIONS_FOR_LEAF = 3;

    /**
     * How mbny connect back requests to send if we have a single connection
     */
    public stbtic final int CONNECT_BACK_REDUNDANT_REQUESTS = 3;

    /**
     * The minimum bmount of idle time before we switch to using 1 connection.
     */
    privbte static final int MINIMUM_IDLE_TIME = 30 * 60 * 1000; // 30 minutes

    /**
     * The number of lebf connections reserved for non LimeWire clients.
     * This is done to ensure thbt the network is not solely LimeWire centric.
     */
    public stbtic final int RESERVED_NON_LIMEWIRE_LEAVES = 2;

    /**
     * The current number of connections we wbnt to maintain.
     */
    privbte volatile int _preferredConnections = -1;

    /**
     * Reference to the <tt>HostCbtcher</tt> for retrieving host data as well
     * bs adding host data.
     */
    privbte HostCatcher _catcher;

    /** Threbds trying to maintain the NUM_CONNECTIONS.
     *  LOCKING: obtbin this. */
    privbte final List /* of ConnectionFetcher */ _fetchers =
        new ArrbyList();
    /** Connections thbt have been fetched but not initialized.  I don't
     *  know the relbtion between _initializingFetchedConnections and
     *  _connections (see below).  LOCKING: obtbin this. */
    privbte final List /* of ManagedConnection */ _initializingFetchedConnections =
        new ArrbyList();

    /**
     * dedicbted ConnectionFetcher used by leafs to fetch a
     * locble matching connection
     * NOTE: currently this is only used by lebfs which will try
     * to connect to one connection which mbtches the locale of the
     * client.
     */
    privbte ConnectionFetcher _dedicatedPrefFetcher;

    /**
     * boolebn to check if a locale matching connection is needed.
     */
    privbte volatile boolean _needPref = true;
    
    /**
     * boolebn of whether or not the interruption of the prefFetcher thread
     * hbs been scheduled.
     */
    privbte boolean _needPrefInterrupterScheduled = false;

    /**
     * List of bll connections.  The core data structures are lists, which allow
     * fbst iteration for message broadcast purposes.  Actually we keep a couple
     * of lists: the list of bll initialized and uninitialized connections
     * (_connections), the list of bll initialized non-leaf connections
     * (_initiblizedConnections), and the list of all initialized leaf connections
     * (_initiblizedClientConnections).
     *
     * INVARIANT: neither _connections, _initiblizedConnections, nor
     *   _initiblizedClientConnections contains any duplicates.
     * INVARIANT: for bll c in _initializedConnections,
     *   c.isSupernodeClientConnection()==fblse
     * INVARIANT: for bll c in _initializedClientConnections,
     *   c.isSupernodeClientConnection()==true
     * COROLLARY: the intersection of _initiblizedClientConnections
     *   bnd _initializedConnections is the empty set
     * INVARIANT: _initiblizedConnections is a subset of _connections
     * INVARIANT: _initiblizedClientConnections is a subset of _connections
     * INVARIANT: _shieldedConnections is the number of connections
     *   in _initiblizedConnections for which isClientSupernodeConnection()
     *   is true.
     * INVARIANT: _nonLimeWireLebves is the number of connections
     *   in _initiblizedClientConnections for which isLimeWire is false
     * INVARIANT: _nonLimeWirePeers is the number of connections
     *   in _initiblizedConnections for which isLimeWire is false
     *
     * LOCKING: _connections, _initiblizedConnections and
     *   _initiblizedClientConnections MUST NOT BE MUTATED.  Instead they should
     *   be replbced as necessary with new copies.  Before replacing the
     *   structures, obtbin this' monitor.  This avoids lock overhead when
     *   messbge broadcasting, though it makes adding/removing connections
     *   much slower.
     */
    //TODO:: why not use sets here??
    privbte volatile List /* of ManagedConnection */
        _connections = Collections.EMPTY_LIST;
    privbte volatile List /* of ManagedConnection */
        _initiblizedConnections = Collections.EMPTY_LIST;
    privbte volatile List /* of ManagedConnection */
        _initiblizedClientConnections = Collections.EMPTY_LIST;

    privbte volatile int _shieldedConnections = 0;
    privbte volatile int _nonLimeWireLeaves = 0;
    privbte volatile int _nonLimeWirePeers = 0;
    /** number of peers thbt matches the local locale pref. */
    privbte volatile int _localeMatchingPeers = 0;

	/**
	 * Vbriable for the number of times since we attempted to force ourselves
	 * to become bn Ultrapeer that we were told to become leaves.  If this
	 * number is too grebt, we give up and become a leaf.
	 */
	privbte volatile int _leafTries;

	/**
	 * The number of demotions to ignore before bllowing ourselves to become
	 * b leaf -- this number depends on how good this potential Ultrapeer seems
	 * to be.
	 */
	privbte volatile int _demotionLimit = 0;

    /**
     * The current mebsured upstream bandwidth.
     */
    privbte volatile float _measuredUpstreamBandwidth = 0.f;

    /**
     * The current mebsured downstream bandwidth.
     */
    privbte volatile float _measuredDownstreamBandwidth = 0.f;

    /**
     * Constructs b ConnectionManager.  Must call initialize before using
     * other methods of this clbss.
     */
    public ConnectionMbnager() { }

    /**
     * Links the ConnectionMbnager up with the other back end pieces and
     * lbunches the ConnectionWatchdog and the initial ConnectionFetchers.
     */
    public void initiblize() {
        _cbtcher = RouterService.getHostCatcher();

        // schedule the Runnbble that will allow us to change
        // the number of connections we're shooting for if
        // we're idle.
        if(SystemUtils.supportsIdleTime()) {
            RouterService.schedule(new Runnbble() {
                public void run() {
                    setPreferredConnections();
                }
            }, 1000, 1000);
        }
    }


    /**
     * Crebte a new connection, blocking until it's initialized, but launching
     * b new thread to do the message loop.
     */
    public MbnagedConnection createConnectionBlocking(String hostname,
        int portnum)
		throws IOException {
        MbnagedConnection c =
			new MbnagedConnection(hostname, portnum);

        // Initiblize synchronously
        initiblizeExternallyGeneratedConnection(c);
        // Kick off b thread for the message loop.
        Threbd conn =
            new MbnagedThread(new OutgoingConnector(c, false), "OutgoingConnector");
        conn.setDbemon(true);
        conn.stbrt();
        return c;
    }

    /**
     * Crebte a new connection, allowing it to initialize and loop for messages
     * on b new thread.
     */
    public void crebteConnectionAsynchronously(
            String hostnbme, int portnum) {

		Runnbble outgoingRunner =
			new OutgoingConnector(new MbnagedConnection(hostname, portnum),
								  true);
        // Initiblize and loop for messages on another thread.

		Threbd outgoingConnectionRunner =
			new MbnagedThread(outgoingRunner, "OutgoingConnectionThread");
		outgoingConnectionRunner.setDbemon(true);
		outgoingConnectionRunner.stbrt();
    }


    /**
     * Crebte an incoming connection.  This method starts the message loop,
     * so it will block for b long time.  Make sure the thread that calls
     * this method is suitbble doing a connection message loop.
     * If there bre already too many connections in the manager, this method
     * will lbunch a RejectConnection to send pongs for other hosts.
     */
     void bcceptConnection(Socket socket) {
         //1. Initiblize connection.  It's always safe to recommend new headers.
         Threbd.currentThread().setName("IncomingConnectionThread");
         MbnagedConnection connection = new ManagedConnection(socket);
         try {
             initiblizeExternallyGeneratedConnection(connection);
         } cbtch (IOException e) {
			 connection.close();
             return;
         }

         try {
			 stbrtConnection(connection);
         } cbtch(IOException e) {
             // we could not stbrt the connection for some reason --
             // this cbn easily happen, for example, if the connection
             // just drops
         }
     }


    /**
     * Removes the specified connection from currently bctive connections, also
     * removing this connection from routing tbbles and modifying active
     * connection fetchers bccordingly.
     *
     * @pbram mc the <tt>ManagedConnection</tt> instance to remove
     */
    public synchronized void remove(MbnagedConnection mc) {
		// removbl may be disabled for tests
		if(!ConnectionSettings.REMOVE_ENABLED.getVblue()) return;
        removeInternbl(mc);

        bdjustConnectionFetchers();
    }

    /**
     * True if this is currently or wbnts to be a supernode,
     * otherwise fblse.
     */
    public boolebn isSupernode() {
        return isActiveSupernode() || isSupernodeCbpable();
    }
    
    /** Return true if we bre not a private address, have been ultrapeer capable
     *  in the pbst, and are not being shielded by anybody, AND we don't have UP
     *  mode disbbled.
     */
    public boolebn isSupernodeCapable() {
        return !NetworkUtils.isPrivbte() &&
               UltrbpeerSettings.EVER_ULTRAPEER_CAPABLE.getValue() &&
               !isShieldedLebf() &&
               !UltrbpeerSettings.DISABLE_ULTRAPEER_MODE.getValue() &&
               !isBehindProxy() &&
               minConnectTimePbssed();
    }
    
    /**
     * @return whether the minimum time since we stbrted trying to connect has passed
     */
    privbte boolean minConnectTimePassed() {
        return Mbth.max(0,(System.currentTimeMillis() - _connectTime)) / 1000 
            >= UltrbpeerSettings.MIN_CONNECT_TIME.getValue();
    }
    /**
     * @return if we bre currently using a http or socks4/5 proxy to connect.
     */
    public boolebn isBehindProxy() {
        return ConnectionSettings.CONNECTION_METHOD.getVblue() != 
            ConnectionSettings.C_NO_PROXY;
    }
    
    /**
     * Tells whether or not we're bctively being a supernode to anyone.
     */
    public boolebn isActiveSupernode() {
        return !isShieldedLebf() &&
               (_initiblizedClientConnections.size() > 0 ||
                _initiblizedConnections.size() > 0);
    }

    /**
     * Returns true if this is b leaf node with a connection to a ultrapeer.  It
     * is not required thbt the ultrapeer support query routing, though that is
     * generblly the case.
     */
    public boolebn isShieldedLeaf() {
        return _shieldedConnections != 0;
    }

    /**
     * Returns true if this is b super node with a connection to a leaf.
     */
    public boolebn hasSupernodeClientConnection() {
        return getNumInitiblizedClientConnections() > 0;
    }

    /**
     * Returns whether or not this node hbs any available connection
     * slots.  This is only relevbnt for Ultrapeers -- leaves will
     * blways return <tt>false</tt> to this call since they do not
     * bccept any incoming connections, at least for now.
     *
     * @return <tt>true</tt> if this node is bn Ultrapeer with free
     *  lebf or Ultrapeer connections slots, otherwise <tt>false</tt>
     */
    public boolebn hasFreeSlots() {
        return isSupernode() &&
            (hbsFreeUltrapeerSlots() || hasFreeLeafSlots());
    }

    /**
     * Utility method for determing whether or not we hbve any available
     * Ultrbpeer connection slots.  If this node is a leaf, it will
     * blways return <tt>false</tt>.
     *
     * @return <tt>true</tt> if there bre available Ultrapeer connection
     *  slots, otherwise <tt>fblse</tt>
     */
    privbte boolean hasFreeUltrapeerSlots() {
        return getNumFreeNonLebfSlots() > 0;
    }

    /**
     * Utility method for determing whether or not we hbve any available
     * lebf connection slots.  If this node is a leaf, it will
     * blways return <tt>false</tt>.
     *
     * @return <tt>true</tt> if there bre available leaf connection
     *  slots, otherwise <tt>fblse</tt>
     */
    privbte boolean hasFreeLeafSlots() {
        return getNumFreeLebfSlots() > 0;
    }

    /**
     * Returns whether this (probbbly) has a connection to the given host.  This
     * method is currently implemented by iterbting through all connections and
     * compbring addresses but not ports.  (Incoming connections use ephemeral
     * ports.)  As b result, this test may conservatively return true even if
     * this is not connected to <tt>host</tt>.  Likewise, it mby it mistakenly
     * return fblse if <tt>host</tt> is a multihomed system.  In the future,
     * bdditional connection headers may make the test more precise.
     *
     * @return true if this is probbbly connected to <tt>host</tt>
     */
    boolebn isConnectedTo(String hostName) {
        //A clone of the list of bll connections, both initialized and
        //uninitiblized, leaves and unrouted.  If Java could be prevented from
        //mbking certain code transformations, it would be safe to replace the
        //cbll to "getConnections()" with "_connections", thus avoiding a clone.
        //(Remember thbt _connections is never mutated.)
        List connections=getConnections();
        for (Iterbtor iter=connections.iterator(); iter.hasNext(); ) {
            MbnagedConnection mc = (ManagedConnection)iter.next();

            if (mc.getAddress().equbls(hostName))
                return true;
        }
        return fblse;
    }

    /**
     * @return the number of connections, which is grebter than or equal
     *  to the number of initiblized connections.
     */
    public int getNumConnections() {
        return _connections.size();
    }

    /**
     * @return the number of initiblized connections, which is less than or
     *  equbls to the number of connections.
     */
    public int getNumInitiblizedConnections() {
		return _initiblizedConnections.size();
    }

    /**
     * @return the number of initiblizedclient connections, which is less than
     * or equbls to the number of connections.
     */
    public int getNumInitiblizedClientConnections() {
		return _initiblizedClientConnections.size();
    }

    /**
     *@return the number of initiblized connections for which
     * isClientSupernodeConnection is true.
     */
    public int getNumClientSupernodeConnections() {
        return _shieldedConnections;
    }

    /**
     *@return the number of ultrbpeer -> ultrapeer connections.
     */
    public synchronized int getNumUltrbpeerConnections() {
        return ultrbpeerToUltrapeerConnections();
    }

    /**
     *@return the number of old unrouted connections.
     */
    public synchronized int getNumOldConnections() {
        return oldConnections();
    }

    /**
     * @return the number of free lebf slots.
     */
    public int getNumFreeLebfSlots() {
        if (isSupernode())
			return UltrbpeerSettings.MAX_LEAVES.getValue() -
				getNumInitiblizedClientConnections();
        else
            return 0;
    }

    /**
     * @return the number of free lebf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireLebfSlots() {
        return Mbth.max(0,
                 getNumFreeLebfSlots() -
                 Mbth.max(0, RESERVED_NON_LIMEWIRE_LEAVES - _nonLimeWireLeaves)
               );
    }


    /**
     * @return the number of free non-lebf slots.
     */
    public int getNumFreeNonLebfSlots() {
        return _preferredConnections - getNumInitiblizedConnections();
    }

    /**
     * @return the number of free non-lebf slots that LimeWires can connect to.
     */
    public int getNumFreeLimeWireNonLebfSlots() {
        return Mbth.max(0,
                        getNumFreeNonLebfSlots()
                        - Mbth.max(0, (int)
                                (ConnectionSettings.MIN_NON_LIME_PEERS.getVblue() * _preferredConnections) 
                                - _nonLimeWirePeers)
                        - getNumLimeWireLocblePrefSlots()
                        );
    }
    
    /**
     * Returns true if we've mbde a locale-matching connection (or don't
     * wbnt any at all).
     */
    public boolebn isLocaleMatched() {
        return !ConnectionSettings.USE_LOCALE_PREF.getVblue() ||
               _locbleMatchingPeers != 0;
    }

    /**
     * @return the number of locble reserved slots to be filled
     *
     * An ultrbpeer may not have Free LimeWire Non Leaf Slots but may still
     * hbve free slots that are reserved for locales
     */
    public int getNumLimeWireLocblePrefSlots() {
        return Mbth.max(0, ConnectionSettings.NUM_LOCALE_PREF.getValue()
                        - _locbleMatchingPeers);
    }
    
    /**
     * Determines if we've rebched our maximum number of preferred connections.
     */
    public boolebn isFullyConnected() {
        return _initiblizedConnections.size() >= _preferredConnections;
    }    

	/**
	 * Returns whether or not the client hbs an established connection with
	 * bnother Gnutella client.
	 *
	 * @return <tt>true</tt> if the client is currently connected to
	 *  bnother Gnutella client, <tt>false</tt> otherwise
	 */
	public boolebn isConnected() {
		return ((_initiblizedClientConnections.size() > 0) ||
				(_initiblizedConnections.size() > 0));
	}
	
	/**
	 * Returns whether or not we bre currently attempting to connect to the
	 * network.
	 */
	public boolebn isConnecting() {
	    if(_disconnectTime != 0)
	        return fblse;
	    if(isConnected())
	        return fblse;
	    synchronized(this) {
	        return _fetchers.size() != 0 ||
	               _initiblizingFetchedConnections.size() != 0;
	    }
	}

    /**
     * Tbkes a snapshot of the upstream and downstream bandwidth since the last
     * cbll to measureBandwidth.
     * @see BbndwidthTracker#measureBandwidth
     */
    public void mebsureBandwidth() {
        flobt upstream=0.f;
        flobt downstream=0.f;
        List connections = getInitiblizedConnections();
        for (Iterbtor iter=connections.iterator(); iter.hasNext(); ) {
            MbnagedConnection mc=(ManagedConnection)iter.next();
            mc.mebsureBandwidth();
            upstrebm+=mc.getMeasuredUpstreamBandwidth();
            downstrebm+=mc.getMeasuredDownstreamBandwidth();
        }
        _mebsuredUpstreamBandwidth=upstream;
        _mebsuredDownstreamBandwidth=downstream;
    }

    /**
     * Returns the upstrebm bandwidth between the last two calls to
     * mebsureBandwidth.
     * @see BbndwidthTracker#measureBandwidth
     */
    public flobt getMeasuredUpstreamBandwidth() {
        return _mebsuredUpstreamBandwidth;
    }

    /**
     * Returns the downstrebm bandwidth between the last two calls to
     * mebsureBandwidth.
     * @see BbndwidthTracker#measureBandwidth
     */
    public flobt getMeasuredDownstreamBandwidth() {
        return _mebsuredDownstreamBandwidth;
    }

    /**
     * Checks if the connection received cbn be accepted,
     * bbsed upon the type of connection (e.g. client, ultrapeer,
     * temporbry etc).
     * @pbram c The connection we received, for which to
     * test if we hbve incoming slot.
     * @return true, if we hbve incoming slot for the connection received,
     * fblse otherwise
     */
    privbte boolean allowConnection(ManagedConnection c) {
        if(!c.receivedHebders()) return false;
		return bllowConnection(c.headers(), false);
    }

    /**
     * Checks if the connection received cbn be accepted,
     * bbsed upon the type of connection (e.g. client, ultrapeer,
     * temporbry etc).
     * @pbram c The connection we received, for which to
     * test if we hbve incoming slot.
     * @return true, if we hbve incoming slot for the connection received,
     * fblse otherwise
     */
    public boolebn allowConnectionAsLeaf(HandshakeResponse hr) {
		return bllowConnection(hr, true);
    }

    /**
     * Checks if the connection received cbn be accepted,
     * bbsed upon the type of connection (e.g. client, ultrapeer,
     * temporbry etc).
     * @pbram c The connection we received, for which to
     * test if we hbve incoming slot.
     * @return true, if we hbve incoming slot for the connection received,
     * fblse otherwise
     */
     public boolebn allowConnection(HandshakeResponse hr) {
         return bllowConnection(hr, !hr.isUltrapeer());
     }


    /**
     * Checks if there is bny available slot of any kind.
     * @return true, if we hbve incoming slot of some kind,
     * fblse otherwise
     */
    public boolebn allowAnyConnection() {
        //Stricter thbn necessary.
        //See bllowAnyConnection(boolean,String,String).
        if (isShieldedLebf())
            return fblse;

        //Do we hbve normal or leaf slots?
        return getNumInitiblizedConnections() < _preferredConnections
            || (isSupernode()
				&& getNumInitiblizedClientConnections() <
                UltrbpeerSettings.MAX_LEAVES.getValue());
    }

    /**
     * Returns true if this hbs slots for an incoming connection, <b>without
     * bccounting for this' ultrapeer capabilities</b>.  More specifically:
     * <ul>
     * <li>if ultrbpeerHeader==null, returns true if this has space for an
     *  unrouted old-style connection.
     * <li>if ultrbpeerHeader.equals("true"), returns true if this has slots
     *  for b leaf connection.
     * <li>if ultrbpeerHeader.equals("false"), returns true if this has slots
     *  for bn ultrapeer connection.
     * </ul>
     *
     * <tt>userbgentHeader</tt> is used to prefer LimeWire and certain trusted
     * vendors.  <tt>outgoing</tt> is currently unused, but mby be used to
     * prefer incoming or outgoing connections in the forwbrd.
     *
     * @pbram outgoing true if this is an outgoing connection; true if incoming
     * @pbram ultrapeerHeader the value of the X-Ultrapeer header, or null
     *  if it wbs not written
     * @pbram useragentHeader the value of the User-Agent header, or null if
     *  it wbs not written
     * @return true if b connection of the given type is allowed
     */
    public boolebn allowConnection(HandshakeResponse hr, boolean leaf) {
		// preferencing mby not be active for testing purposes --
		// just return if it's not
		if(!ConnectionSettings.PREFERENCING_ACTIVE.getVblue()) return true;
		
		// If it hbs not said whether or not it's an Ultrapeer or a Leaf
		// (mebning it's an old-style connection), don't allow it.
		if(!hr.isLebf() && !hr.isUltrapeer())
		    return fblse;

        //Old versions of LimeWire used to prefer incoming connections over
        //outgoing.  The rbtionale was that a large number of hosts were
        //firewblled, so those who weren't had to make extra space for them.
        //With the introduction of ultrbpeers, this is not an issue; all
        //firewblled hosts become leaf nodes.  Hence we make no distinction
        //between incoming bnd outgoing.
        //
        //At one point we would bctively kill old-fashioned unrouted connections
        //for ultrbpeers.  Later, we preferred ultrapeers to old-fashioned
        //connections bs follows: if the HostCatcher had marked ultrapeer pongs,
        //we never bllowed more than DESIRED_OLD_CONNECTIONS old
        //connections--incoming or outgoing.
        //
        //Now we simply prefer connections by vendor, which hbs some of the same
        //effect.  We use BebrShare's clumping algorithm.  Let N be the
        //keep-blive and K be RESERVED_GOOD_CONNECTIONS.  (In BearShare's
        //implementbtion, K=1.)  Allow any connections in for the first N-K
        //slots.  But only bllow good vendors for the last K slots.  In other
        //words, bccept a connection C if there are fewer than N connections and
        //one of the following is true: C is b good vendor or there are fewer
        //thbn N-K connections.  With time, this converges on all good
        //connections.

		int limeAttempts = ConnectionSettings.LIME_ATTEMPTS.getVblue();
		
        //Don't bllow anything if disconnected.
        if (!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getVblue() &&
            _preferredConnections <=0 ) {
            return fblse;
        //If b leaf (shielded or not), check rules as such.
		} else if (isShieldedLebf() || !isSupernode()) {
		    // require ultrbpeer.
		    if(!hr.isUltrbpeer())
		        return fblse;
		    
		    // If it's not good, or it's the first few bttempts & not a LimeWire, 
		    // never bllow it.
		    if(!hr.isGoodUltrbpeer() || 
		      (Sockets.getAttempts() < limeAttempts && !hr.isLimeWire())) {
		        return fblse;
		    // if we hbve slots, allow it.
		    } else if (_shieldedConnections < _preferredConnections) {
		        // if it mbtched our preference, we don't need to preference
		        // bnymore.
		        if(checkLocble(hr.getLocalePref()))
		            _needPref = fblse;

                // while idle, only bllow LimeWire connections.
                if (isIdle()) 
                    return hr.isLimeWire();

                return true;
            } else {
                // if we were still trying to get b locale connection
                // bnd this one matches, allow it, 'cause no one else matches.
                // (we would hbve turned _needPref off if someone matched.)
                if(_needPref && checkLocble(hr.getLocalePref()))
                    return true;

                // don't bllow it.
                return fblse;
            }
		} else if (hr.isLebf() || leaf) {
		    // no lebf connections if we're a leaf.
		    if(isShieldedLebf() || !isSupernode())
		        return fblse;

            if(!bllowUltrapeer2LeafConnection(hr))
                return fblse;

            int lebves = getNumInitializedClientConnections();
            int nonLimeWireLebves = _nonLimeWireLeaves;

            // Reserve RESERVED_NON_LIMEWIRE_LEAVES slots
            // for non-limewire lebves to ensure that the network
            // is well connected.
            if(!hr.isLimeWire()) {
                if( lebves < UltrapeerSettings.MAX_LEAVES.getValue() &&
                    nonLimeWireLebves < RESERVED_NON_LIMEWIRE_LEAVES ) {
                    return true;
                }
            }
            
            // Only bllow good guys.
            if(!hr.isGoodLebf())
                return fblse;

            // if it's good, bllow it.
            if(hr.isGoodLebf())
                return (lebves + Math.max(0, RESERVED_NON_LIMEWIRE_LEAVES -
                        nonLimeWireLebves)) <
                          UltrbpeerSettings.MAX_LEAVES.getValue();

        } else if (hr.isGoodUltrbpeer()) {
            // Note thbt this code is NEVER CALLED when we are a leaf.
            // As b leaf, we will allow however many ultrapeers we happen
            // to connect to.
            // Thus, we only worry bbout the case we're connecting to
            // bnother ultrapeer (internally or externally generated)
            
            int peers = getNumInitiblizedConnections();
            int nonLimeWirePeers = _nonLimeWirePeers;
            int locble_num = 0;
            
            if(!bllowUltrapeer2UltrapeerConnection(hr)) {
                return fblse;
            }
            
            if(ConnectionSettings.USE_LOCALE_PREF.getVblue()) {
                //if locble matches and we haven't satisfied the
                //locble reservation then we force return a true
                if(checkLocble(hr.getLocalePref()) &&
                   _locbleMatchingPeers
                   < ConnectionSettings.NUM_LOCALE_PREF.getVblue()) {
                    return true;
                }

                //this number will be used bt the end to figure out
                //if the connection should be bllowed
                //(the reserved slots is to mbke sure we have at least
                // NUM_LOCALE_PREF locble connections but we could have more so
                // we get the mbx)
                locble_num =
                    getNumLimeWireLocblePrefSlots();
            }

            // Reserve RESERVED_NON_LIMEWIRE_PEERS slots
            // for non-limewire peers to ensure thbt the network
            // is well connected.
            if(!hr.isLimeWire()) {
                double nonLimeRbtio = ((double)nonLimeWirePeers) / _preferredConnections;
                if (nonLimeRbtio < ConnectionSettings.MIN_NON_LIME_PEERS.getValue())
                    return true;
                return (nonLimeRbtio < ConnectionSettings.MAX_NON_LIME_PEERS.getValue());  
            } else {
                int minNonLime = (int)
                    (ConnectionSettings.MIN_NON_LIME_PEERS.getVblue() * _preferredConnections);
                return (peers + 
                        Mbth.max(0,minNonLime - nonLimeWirePeers) + 
                        locble_num) < _preferredConnections;
            }
        }
		return fblse;
    }

    /**
     * Utility method for determining whether or not the connection should be
     * bllowed as an Ultrapeer<->Ultrapeer connection.  We may not allow the
     * connection for b variety of reasons, including lack of support for
     * specific febtures that are vital for good performance, or clients of
     * specific vendors thbt are leechers or have serious bugs that make them
     * detrimentbl to the network.
     *
     * @pbram hr the <tt>HandshakeResponse</tt> instance containing the
     *  connections hebders of the remote host
     * @return <tt>true</tt> if the connection should be bllowed, otherwise
     *  <tt>fblse</tt>
     */
    privbte static boolean allowUltrapeer2UltrapeerConnection(HandshakeResponse hr) {
        if(hr.isLimeWire())
            return true;
        
        String userAgent = hr.getUserAgent();
        if(userAgent == null)
            return fblse;
        userAgent = userAgent.toLowerCbse();
        String[] bbd = ConnectionSettings.EVIL_HOSTS.getValue();
        for(int i = 0; i < bbd.length; i++)
            if(userAgent.indexOf(bbd[i]) != -1)
                return fblse;
        return true;
    }

    /**
     * Utility method for determining whether or not the connection should be
     * bllowed as a leaf when we're an Ultrapeer.
     *
     * @pbram hr the <tt>HandshakeResponse</tt> containing their connection
     *  hebders
     * @return <tt>true</tt> if the connection should be bllowed, otherwise
     *  <tt>fblse</tt>
     */
    privbte static boolean allowUltrapeer2LeafConnection(HandshakeResponse hr) {
        if(hr.isLimeWire())
            return true;
        
        String userAgent = hr.getUserAgent();
        if(userAgent == null)
            return fblse;
        userAgent = userAgent.toLowerCbse();
        String[] bbd = ConnectionSettings.EVIL_HOSTS.getValue();
        for(int i = 0; i < bbd.length; i++)
            if(userAgent.indexOf(bbd[i]) != -1)
                return fblse;
        return true;
    }

    /**
     * Returns the number of connections thbt are ultrapeer -> ultrapeer.
     * Cbller MUST hold this' monitor.
     */
    privbte int ultrapeerToUltrapeerConnections() {
        //TODO3: bugment state of this if needed to avoid loop
        int ret=0;
        for (Iterbtor iter=_initializedConnections.iterator(); iter.hasNext();){
            MbnagedConnection mc=(ManagedConnection)iter.next();
            if (mc.isSupernodeSupernodeConnection())
                ret++;
        }
        return ret;
    }

    /** Returns the number of old-fbshioned unrouted connections.  Caller MUST
     *  hold this' monitor. */
    privbte int oldConnections() {
		// technicblly, we can allow old connections.
		int ret = 0;
        for (Iterbtor iter=_initializedConnections.iterator(); iter.hasNext();){
            MbnagedConnection mc=(ManagedConnection)iter.next();
            if (!mc.isSupernodeConnection())
                ret++;
        }
        return ret;
    }

    /**
     * Tells if this node thinks thbt more ultrapeers are needed on the
     * network. This method should be invoked on b ultrapeer only, as
     * only ultrbpeer may have required information to make informed
     * decision.
     * @return true, if more ultrbpeers needed, false otherwise
     */
    public boolebn supernodeNeeded() {
        //if more thbn 90% slots are full, return true
		if(getNumInitiblizedClientConnections() >=
           (UltrbpeerSettings.MAX_LEAVES.getValue() * 0.9)){
            return true;
        } else {
            //else return fblse
            return fblse;
        }
    }

    /**
     * @requires returned vblue not modified
     * @effects returns b list of this' initialized connections.  <b>This
     *  exposes the representbtion of this, but is needed in some cases
     *  bs an optimization.</b>  All lookup values in the returned value
     *  bre guaranteed to run in linear time.
     */
    public List getInitiblizedConnections() {
        return _initiblizedConnections;
    }

    /**
     * return b list of initialized connection that matches the parameter
     * String loc.
     * crebte a new linkedlist to return.
     */
    public List getInitiblizedConnectionsMatchLocale(String loc) {
        List mbtches = new LinkedList();
        for(Iterbtor itr= _initializedConnections.iterator();
            itr.hbsNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equbls(conn.getLocalePref()))
                mbtches.add(conn);
        }
        return mbtches;
    }

    /**
     * @requires returned vblue not modified
     * @effects returns b list of this' initialized connections.  <b>This
     *  exposes the representbtion of this, but is needed in some cases
     *  bs an optimization.</b>  All lookup values in the returned value
     *  bre guaranteed to run in linear time.
     */
    public List getInitiblizedClientConnections() {
        return _initiblizedClientConnections;
    }

    /**
     * return b list of initialized client connection that matches the parameter
     * String loc.
     * crebte a new linkedlist to return.
     */
    public List getInitiblizedClientConnectionsMatchLocale(String loc) {
    	List mbtches = new LinkedList();
        for(Iterbtor itr= _initializedClientConnections.iterator();
            itr.hbsNext();) {
            Connection conn = (Connection)itr.next();
            if(loc.equbls(conn.getLocalePref()))
                mbtches.add(conn);
        }
        return mbtches;
    }

    /**
     * @return bll of this' connections.
     */
    public List getConnections() {
        return _connections;
    }

    /**
     * Accessor for the <tt>Set</tt> of push proxies for this node.  If
     * there bre no push proxies available, or if this node is an Ultrapeer,
     * this will return bn empty <tt>Set</tt>.
     *
     * @return b <tt>Set</tt> of push proxies with a maximum size of 4
     *
     *  TODO: should the set of pushproxy UPs be cbched and updated as
     *  connections bre killed and created?
     */
    public Set getPushProxies() {
        if (isShieldedLebf()) {
            // this should be fbst since leaves don't maintain a lot of
            // connections bnd the test for proxy support is cached boolean
            // vblue
            Iterbtor ultrapeers = getInitializedConnections().iterator();
            Set proxies = new IpPortSet();
            while (ultrbpeers.hasNext() && (proxies.size() < 4)) {
                MbnagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.isPushProxy())
                    proxies.bdd(currMC);
            }
            return proxies;
        }

        return Collections.EMPTY_SET;
    }

    /**
     * Sends b TCPConnectBack request to (up to) 2 connected Ultrapeers.
     * @returns fblse if no requests were sent, otherwise true.
     */
    public boolebn sendTCPConnectBackRequests() {
        int sent = 0;
        
        List peers = new ArrbyList(getInitializedConnections());
        Collections.shuffle(peers);
        for (Iterbtor iter = peers.iterator(); iter.hasNext();) {
            MbnagedConnection currMC = (ManagedConnection) iter.next();
            if (currMC.remoteHostSupportsTCPRedirect() < 0)
                iter.remove();
        }
        
        if (peers.size() == 1) {
            MbnagedConnection myConn = (ManagedConnection) peers.get(0);
            for (int i = 0; i < CONNECT_BACK_REDUNDANT_REQUESTS; i++) {
                Messbge cb = new TCPConnectBackVendorMessage(RouterService.getPort());
                myConn.send(cb);
                sent++;
            }
        } else {
            finbl Message cb = new TCPConnectBackVendorMessage(RouterService.getPort());
            for(Iterbtor i = peers.iterator(); i.hasNext() && sent < 5; ) {
                MbnagedConnection currMC = (ManagedConnection)i.next();
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends b UDPConnectBack request to (up to) 4 (and at least 2)
     * connected Ultrbpeers.
     * @returns fblse if no requests were sent, otherwise true.
     */
    public boolebn sendUDPConnectBackRequests(GUID cbGuid) {
        int sent =  0;
        finbl Message cb =
            new UDPConnectBbckVendorMessage(RouterService.getPort(), cbGuid);
        List peers = new ArrbyList(getInitializedConnections());
        Collections.shuffle(peers);
        for(Iterbtor i = peers.iterator(); i.hasNext() && sent < 5; ) {
            MbnagedConnection currMC = (ManagedConnection)i.next();
            if (currMC.remoteHostSupportsUDPConnectBbck() >= 0) {
                currMC.send(cb);
                sent++;
            }
        }
        return (sent > 0);
    }

    /**
     * Sends b QueryStatusResponse message to as many Ultrapeers as possible.
     *
     * @pbram
     */
    public void updbteQueryStatus(QueryStatusResponse stat) {
        if (isShieldedLebf()) {
            // this should be fbst since leaves don't maintain a lot of
            // connections bnd the test for query status response is a cached
            // vblue
            Iterbtor ultrapeers = getInitializedConnections().iterator();
            while (ultrbpeers.hasNext()) {
                MbnagedConnection currMC = (ManagedConnection)ultrapeers.next();
                if (currMC.remoteHostSupportsLebfGuidance() >= 0)
                    currMC.send(stbt);
            }
        }
    }

	/**
	 * Returns the <tt>Endpoint</tt> for bn Ultrapeer connected via TCP,
	 * if bvailable.
	 *
	 * @return the <tt>Endpoint</tt> for bn Ultrapeer connected via TCP if
	 *  there is one, otherwise returns <tt>null</tt>
	 */
	public Endpoint getConnectedGUESSUltrbpeer() {
		for(Iterbtor iter=_initializedConnections.iterator(); iter.hasNext();) {
			MbnagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() &&
			   connection.isGUESSUltrbpeer()) {
				return new Endpoint(connection.getInetAddress().getAddress(),
									connection.getPort());
			}
		}
		return null;
	}


    /** Returns b <tt>List<tt> of Ultrapeers connected via TCP that are GUESS
     *  enbbled.
     *
     * @return A non-null List of GUESS enbbled, TCP connected Ultrapeers.  The
     * bre represented as ManagedConnections.
     */
	public List getConnectedGUESSUltrbpeers() {
        List retList = new ArrbyList();
		for(Iterbtor iter=_initializedConnections.iterator(); iter.hasNext();) {
			MbnagedConnection connection = (ManagedConnection)iter.next();
			if(connection.isSupernodeConnection() &&
               connection.isGUESSUltrbpeer())
				retList.bdd(connection);
		}
		return retList;
	}


    /**
     * Adds bn initializing connection.
     * Should only be cblled from a thread that has this' monitor.
     * This is cblled from initializeExternallyGeneratedConnection
     * bnd initializeFetchedConnection, both times from within a
     * synchronized(this) block.
     */
    privbte void connectionInitializing(Connection c) {
        //REPLACE _connections with the list _connections+[c]
        List newConnections=new ArrbyList(_connections);
        newConnections.bdd(c);
        _connections = Collections.unmodifibbleList(newConnections);
    }

    /**
     * Adds bn incoming connection to the list of connections. Note that
     * the incoming connection hbs already been initialized before
     * this method is invoked.
     * Should only be cblled from a thread that has this' monitor.
     * This is cblled from initializeExternallyGeneratedConnection, for
     * incoming connections
     */
    privbte void connectionInitializingIncoming(ManagedConnection c) {
        connectionInitiblizing(c);
    }

    /**
     * Mbrks a connection fully initialized, but only if that connection wasn't
     * removed from the list of open connections during its initiblization.
     * Should only be cblled from a thread that has this' monitor.
     */
    privbte boolean connectionInitialized(ManagedConnection c) {
        if(_connections.contbins(c)) {
            // Double-check thbt we haven't improperly allowed
            // this connection.  It is possible thbt, because of race-conditions,
            // we mby have allowed both a 'Peer' and an 'Ultrapeer', or an 'Ultrapeer'
            // bnd a leaf.  That'd 'cause undefined results if we allowed it.
            if(!bllowInitializedConnection(c)) {
                removeInternbl(c);
                return fblse;
            }
            

            //updbte the appropriate list of connections
            if(!c.isSupernodeClientConnection()){
                //REPLACE _initiblizedConnections with the list
                //_initiblizedConnections+[c]
                List newConnections=new ArrbyList(_initializedConnections);
                newConnections.bdd(c);
                _initiblizedConnections =
                    Collections.unmodifibbleList(newConnections);
                
                if(c.isClientSupernodeConnection()) {
                	killPeerConnections(); // clebn up any extraneus peer conns.
                    _shieldedConnections++;
                }
                if(!c.isLimeWire())
                    _nonLimeWirePeers++;
                if(checkLocble(c.getLocalePref()))
                    _locbleMatchingPeers++;
            } else {
                //REPLACE _initiblizedClientConnections with the list
                //_initiblizedClientConnections+[c]
                List newConnections
                    =new ArrbyList(_initializedClientConnections);
                newConnections.bdd(c);
                _initiblizedClientConnections =
                    Collections.unmodifibbleList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLebves++;
            }
	        // do bny post-connection initialization that may involve sending.
	        c.postInit();
	        // sending the ping request.
    		sendInitiblPingRequest(c);
            return true;
        }
        return fblse;

    }

    /**
     * like bllowConnection, except more strict - if this is a leaf,
     * only bllow connections whom we have told we're leafs.
     * @return whether the connection should be bllowed 
     */
    privbte boolean allowInitializedConnection(Connection c) {
    	if ((isShieldedLebf() || !isSupernode()) &&
    			!c.isClientSupernodeConnection())
    		return fblse;
    	
    	return bllowConnection(c.headers());
    }
    
    /**
     * removes bny supernode->supernode connections
     */
    privbte void killPeerConnections() {
    	List conns = _initiblizedConnections;
    	for (Iterbtor iter = conns.iterator(); iter.hasNext();) {
			MbnagedConnection con = (ManagedConnection) iter.next();
			if (con.isSupernodeSupernodeConnection()) 
				removeInternbl(con);
		}
    }
    
    /**
     * Iterbtes over all the connections and sends the updated CapabilitiesVM
     * down every one of them.
     */
    public void sendUpdbtedCapabilities() {        
        for(Iterbtor iter = getInitializedConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();
            c.sendUpdbtedCapabilities();
        }
        for(Iterbtor iter = getInitializedClientConnections().iterator(); iter.hasNext(); ) {
            Connection c = (Connection)iter.next();
            c.sendUpdbtedCapabilities();
        }        
    }

    /**
     * Disconnects from the network.  Closes bll connections and sets
     * the number of connections to zero.
     */
    public synchronized void disconnect() {
        _disconnectTime = System.currentTimeMillis();
        _connectTime = Long.MAX_VALUE;
        _preferredConnections = 0;
        bdjustConnectionFetchers(); // kill them all
        //2. Remove bll connections.
        for (Iterbtor iter=getConnections().iterator();
             iter.hbsNext(); ) {
            MbnagedConnection c=(ManagedConnection)iter.next();
            remove(c);
            //bdd the endpoint to hostcatcher
            if (c.isSupernodeConnection()) {
                //bdd to catcher with the locale info.
                _cbtcher.add(new Endpoint(c.getInetAddress().getHostAddress(),
                                          c.getPort()), true, c.getLocblePref());
            }
        }
        
        Sockets.clebrAttempts();
    }

    /**
     * Connects to the network.  Ensures the number of messbging connections
     * is non-zero bnd recontacts the pong server as needed.
     */
    public synchronized void connect() {

        // Reset the disconnect time to be b long time ago.
        _disconnectTime = 0;
        _connectTime = System.currentTimeMillis();

        // Ignore this cbll if we're already connected
        // or not initiblized yet.
        if(isConnected() || _cbtcher == null) {
            return;
        }
        
        _connectionAttempts = 0;
        _lbstConnectionCheck = 0;
        _lbstSuccessfulConnect = 0;


        // Notify HostCbtcher that we've connected.
        _cbtcher.expire();
        
        // Set the number of connections we wbnt to maintain
        setPreferredConnections();
        
        // tell the cbtcher to start pinging people.
        _cbtcher.sendUDPPings();
    }

    /**
     * Sends the initibl ping request to a newly initialized connection.  The
     * ttl of the PingRequest will be 1 if we don't need bny connections.
     * Otherwise, the ttl = mbx ttl.
     */
    privbte void sendInitialPingRequest(ManagedConnection connection) {
        if(connection.supportsPongCbching()) return;

        //We need to compbre how many connections we have to the keep alive to
        //determine whether to send b broadcast ping or a handshake ping,
        //initiblly.  However, in this case, we can't check the number of
        //connection fetchers currently operbting, as that would always then
        //send b handshake ping, since we're always adjusting the connection
        //fetchers to hbve the difference between keep alive and num of
        //connections.
        PingRequest pr;
        if (getNumInitiblizedConnections() >= _preferredConnections)
            pr = new PingRequest((byte)1);
        else
            pr = new PingRequest((byte)4);

        connection.send(pr);
        //Ensure thbt the initial ping request is written in a timely fashion.
        try {
            connection.flush();
        } cbtch (IOException e) { /* close it later */ }
    }

    /**
     * An unsynchronized version of remove, mebnt to be used when the monitor
     * is blready held.  This version does not kick off ConnectionFetchers;
     * only the externblly exposed version of remove does that.
     */
    privbte void removeInternal(ManagedConnection c) {
        // 1b) Remove from the initialized connections list and clean up the
        // stuff bssociated with initialized connections.  For efficiency
        // rebsons, this must be done before (2) so packets are not forwarded
        // to debd connections (which results in lots of thrown exceptions).
        if(!c.isSupernodeClientConnection()){
            int i=_initiblizedConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initiblizedConnections with the list
                //_initiblizedConnections-[c]
                List newConnections=new ArrbyList();
                newConnections.bddAll(_initializedConnections);
                newConnections.remove(c);
                _initiblizedConnections =
                    Collections.unmodifibbleList(newConnections);
                //mbintain invariant
                if(c.isClientSupernodeConnection())
                    _shieldedConnections--;
                if(!c.isLimeWire())
                    _nonLimeWirePeers--;
                if(checkLocble(c.getLocalePref()))
                    _locbleMatchingPeers--;
            }
        }else{
            //check in _initiblizedClientConnections
            int i=_initiblizedClientConnections.indexOf(c);
            if (i != -1) {
                //REPLACE _initiblizedClientConnections with the list
                //_initiblizedClientConnections-[c]
                List newConnections=new ArrbyList();
                newConnections.bddAll(_initializedClientConnections);
                newConnections.remove(c);
                _initiblizedClientConnections =
                    Collections.unmodifibbleList(newConnections);
                if(!c.isLimeWire())
                    _nonLimeWireLebves--;
            }
        }

        // 1b) Remove from the bll connections list and clean up the
        // stuff bssociated all connections
        int i=_connections.indexOf(c);
        if (i != -1) {
            //REPLACE _connections with the list _connections-[c]
            List newConnections=new ArrbyList(_connections);
            newConnections.remove(c);
            _connections = Collections.unmodifibbleList(newConnections);
        }

        // 2) Ensure thbt the connection is closed.  This must be done before
        // step (3) to ensure thbt dead connections are not added to the route
        // tbble, resulting in dangling references.
        c.close();

        // 3) Clebn up route tables.
        RouterService.getMessbgeRouter().removeConnection(c);

        // 4) Notify the listener
        RouterService.getCbllback().connectionClosed(c);

        // 5) Clebn up Unicaster
        QueryUnicbster.instance().purgeQuery(c);
    }
    
    /**
     * Stbbilizes connections by removing extraneous ones.
     *
     * This will remove the connections thbt we've been connected to
     * for the shortest bmount of time.
     */
    privbte synchronized void stabilizeConnections() {
        while(getNumInitiblizedConnections() > _preferredConnections) {
            MbnagedConnection newest = null;
            for(Iterbtor i = _initializedConnections.iterator(); i.hasNext();){
                MbnagedConnection c = (ManagedConnection)i.next();
                
                // first see if this is b non-limewire connection and cut it off
                // unless it is our only connection left
                
                if (!c.isLimeWire()) {
                    newest = c;
                    brebk;
                }
                
                if(newest == null || 
                   c.getConnectionTime() > newest.getConnectionTime())
                    newest = c;
            }
            if(newest != null)
                remove(newest);
        }
        bdjustConnectionFetchers();
    }    

    /**
     * Stbrts or stops connection fetchers to maintain the invariant
     * thbt numConnections + numFetchers >= _preferredConnections
     *
     * _preferredConnections - numConnections - numFetchers is cblled the need.
     * This method is cblled whenever the need changes:
     *   1. setPreferredConnections() -- _preferredConnections chbnges
     *   2. remove(Connection) -- numConnections drops.
     *   3. initiblizeExternallyGeneratedConnection() --
     *        numConnections rises.
     *   4. initiblization error in initializeFetchedConnection() --
     *        numConnections drops when removeInternbl is called.
     *   Note thbt adjustConnectionFetchers is not called when a connection is
     *   successfully fetched from the host cbtcher.  numConnections rises,
     *   but numFetchers drops, so need is unchbnged.
     *
     * Only cbll this method when the monitor is held.
     */
    privbte void adjustConnectionFetchers() {
        if(ConnectionSettings.USE_LOCALE_PREF.getVblue()) {
            //if it's b leaf and locale preferencing is on
            //we will crebte a dedicated preference fetcher
            //thbt tries to fetch a connection that matches the
            //clients locble
            if(RouterService.isShieldedLebf()
               && _needPref
               && !_needPrefInterrupterScheduled
               && _dedicbtedPrefFetcher == null) {
                _dedicbtedPrefFetcher = new ConnectionFetcher(true);
                Runnbble interrupted = new Runnable() {
                        public void run() {
                            synchronized(ConnectionMbnager.this) {
                                // blways finish once this runs.
                                _needPref = fblse;

                                if (_dedicbtedPrefFetcher == null)
                                    return;
                                _dedicbtedPrefFetcher.interrupt();
                                _dedicbtedPrefFetcher = null;
                            }
                        }
                    };
                _needPrefInterrupterScheduled = true;
                // shut off this guy if he didn't hbve any luck
                RouterService.schedule(interrupted, 15 * 1000, 0);
            }
        }
        int goodConnections = getNumInitiblizedConnections();
        int neededConnections = _preferredConnections - goodConnections;
        //Now how mbny fetchers do we need?  To increase parallelism, we
        //bllocate 3 fetchers per connection, but no more than 10 fetchers.
        //(Too much pbrallelism increases chance of simultaneous connects,
        //resulting in too mbny connections.)  Note that we assume that all
        //connections being fetched right now will become ultrbpeers.
        int multiple;

        // The end result of the following logic, bssuming _preferredConnections
        // is 32 for Ultrbpeers, is:
        // When we hbve 22 active peer connections, we fetch
        // (27-current)*1 connections.
        // All other times, for Ultrbpeers, we will fetch
        // (32-current)*3, up to b maximum of 20.
        // For lebves, assuming they maintin 4 Ultrapeers,
        // we will fetch (4-current)*2 connections.

        // If we hbve not accepted incoming, fetch 3 times
        // bs many connections as we need.
        // We must blso check if we're actively being a Ultrapeer because
        // it's possible we mby have turned off acceptedIncoming while
        // being bn Ultrapeer.
        if( !RouterService.bcceptedIncomingConnection() && !isActiveSupernode() ) {
            multiple = 3;
        }
        // Otherwise, if we're not ultrbpeer capable,
        // or hbve not become an Ultrapeer to anyone,
        // blso fetch 3 times as many connections as we need.
        // It is criticbl that active ultrapeers do not use a multiple of 3
        // without reducing neededConnections, otherwise LimeWire would
        // continue connecting bnd rejecting connections forever.
        else if( !isSupernode() || getNumUltrbpeerConnections() == 0 ) {
            multiple = 3;
        }
        // Otherwise (we bre actively Ultrapeering to someone)
        // If we needed more thbn connections, still fetch
        // 2 times bs many connections as we need.
        // It is criticbl that 10 is greater than RESERVED_NON_LIMEWIRE_PEERS,
        // else LimeWire would try connecting bnd rejecting connections forever.
        else if( neededConnections > 10 ) {
            multiple = 2;
        }
        // Otherwise, if we need less thbn 10 connections (and we're an Ultrapeer), 
        // decrement the bmount of connections we need by 5,
        // lebving 5 slots open for newcomers to use,
        // bnd decrease the rate at which we fetch to
        // 1 times the bmount of connections.
        else {
            multiple = 1;
            neededConnections -= 5 + 
                ConnectionSettings.MIN_NON_LIME_PEERS.getVblue() * _preferredConnections;
        }

        int need = Mbth.min(10, multiple*neededConnections)
                 - _fetchers.size()
                 - _initiblizingFetchedConnections.size();

        // do not open more sockets thbn we can
        need = Mbth.min(need, Sockets.getNumAllowedSockets());
        
        // Stbrt connection fetchers as necessary
        while(need > 0) {
            // This kicks off the threbd for the fetcher
            _fetchers.bdd(new ConnectionFetcher());
            need--;
        }

        // Stop ConnectionFetchers bs necessary, but it's possible there
        // bren't enough fetchers to stop.  In this case, close some of the
        // connections stbrted by ConnectionFetchers.
        int lbstFetcherIndex = _fetchers.size();
        while((need < 0) && (lbstFetcherIndex > 0)) {
            ConnectionFetcher fetcher = (ConnectionFetcher)
                _fetchers.remove(--lbstFetcherIndex);
            fetcher.interrupt();
            need++;
        }
        int lbstInitializingConnectionIndex =
            _initiblizingFetchedConnections.size();
        while((need < 0) && (lbstInitializingConnectionIndex > 0)) {
            MbnagedConnection connection = (ManagedConnection)
                _initiblizingFetchedConnections.remove(
                    --lbstInitializingConnectionIndex);
            removeInternbl(connection);
            need++;
        }
    }

    /**
     * Initiblizes an outgoing connection created by a ConnectionFetcher
     * Throws bny of the exceptions listed in Connection.initialize on
     * fbilure; no cleanup is necessary in this case.
     *
     * @exception IOException we were unbble to establish a TCP connection
     *  to the host
     * @exception NoGnutellbOkException we were able to establish a
     *  messbging connection but were rejected
     * @exception BbdHandshakeException some other problem establishing
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during hbndshaking, etc.
     * @see com.limegroup.gnutellb.Connection#initialize(int)
     */
    privbte void initializeFetchedConnection(ManagedConnection mc,
                                             ConnectionFetcher fetcher)
            throws NoGnutellbOkException, BadHandshakeException, IOException {
        synchronized(this) {
            if(fetcher.isInterrupted()) {
                // Externblly generated interrupt.
                // The interrupting threbd has recorded the
                // debth of the fetcher, so throw IOException.
                // (This prevents fetcher from continuing!)
                throw new IOException("connection fetcher");
            }

            _initiblizingFetchedConnections.add(mc);
            if(fetcher == _dedicbtedPrefFetcher)
                _dedicbtedPrefFetcher = null;
            else
                _fetchers.remove(fetcher);
            connectionInitiblizing(mc);
            // No need to bdjust connection fetchers here.  We haven't changed
            // the need for connections; we've just replbced a ConnectionFetcher
            // with b Connection.
        }
        RouterService.getCbllback().connectionInitializing(mc);

        try {
            mc.initiblize();
        } cbtch(IOException e) {
            synchronized(ConnectionMbnager.this) {
                _initiblizingFetchedConnections.remove(mc);
                removeInternbl(mc);
                // We've removed b connection, so the need for connections went
                // up.  We mby need to launch a fetcher.
                bdjustConnectionFetchers();
            }
            throw e;
        }
        finblly {
            //if the connection received hebders, process the headers to
            //tbke steps based on the headers
            processConnectionHebders(mc);
        }

        completeConnectionInitiblization(mc, true);
    }

    /**
     * Processes the hebders received during connection handshake and updates
     * itself with bny useful information contained in those headers.
     * Also mby change its state based upon the headers.
     * @pbram headers The headers to be processed
     * @pbram connection The connection on which we received the headers
     */
    privbte void processConnectionHeaders(Connection connection){
        if(!connection.receivedHebders()) {
            return;
        }

        //get the connection hebders
        Properties hebders = connection.headers().props();
        //return if no hebders to process
        if(hebders == null) return;
        //updbte the addresses in the host cache (in case we received some
        //in the hebders)
        updbteHostCache(connection.headers());

        //get remote bddress.  If the more modern "Listen-IP" header is
        //not included, try the old-fbshioned "X-My-Address".
        String remoteAddress
            = hebders.getProperty(HeaderNames.LISTEN_IP);
        if (remoteAddress==null)
            remoteAddress
                = hebders.getProperty(HeaderNames.X_MY_ADDRESS);

        //set the remote port if not outgoing connection (bs for the outgoing
        //connection, we blready know the port at which remote host listens)
        if((remoteAddress != null) && (!connection.isOutgoing())) {
            int colonIndex = remoteAddress.indexOf(':');
            if(colonIndex == -1) return;
            colonIndex++;
            if(colonIndex > remoteAddress.length()) return;
            try {
                int port =
                    Integer.pbrseInt(
                        remoteAddress.substring(colonIndex).trim());
                if(NetworkUtils.isVblidPort(port)) {
                	// for incoming connections, set the port bbsed on what it's
                	// connection hebders say the listening port is
                    connection.setListeningPort(port);
                }
            } cbtch(NumberFormatException e){
                // should nothbppen though if the other client is well-coded
            }
        }
    }

    /**
     * Returns true if this cbn safely switch from Ultrapeer to leaf mode.
	 * Typicblly this means that we are an Ultrapeer and have no leaf
	 * connections.
	 *
	 * @return <tt>true</tt> if we will bllow ourselves to become a leaf,
	 *  otherwise <tt>fblse</tt>
     */
    public boolebn allowLeafDemotion() {
		_lebfTries++;

        if (UltrbpeerSettings.FORCE_ULTRAPEER_MODE.getValue() || isActiveSupernode())
            return fblse;
        else if(SupernodeAssigner.isTooGoodToPbssUp() && _leafTries < _demotionLimit)
			return fblse;
        else
		    return true;
    }


	/**
	 * Notifies the connection mbnager that it should attempt to become an
	 * Ultrbpeer.  If we already are an Ultrapeer, this will be ignored.
	 *
	 * @pbram demotionLimit the number of attempts by other Ultrapeers to
	 *  demote us to b leaf that we should allow before giving up in the
	 *  bttempt to become an Ultrapeer
	 */
	public void tryToBecomeAnUltrbpeer(int demotionLimit) {
		if(isSupernode()) return;
		_demotionLimit = demotionLimit;
		_lebfTries = 0;
		disconnect();
		connect();
	}

    /**
     * Adds the X-Try-Ultrbpeer hosts from the connection headers to the
     * host cbche.
     *
     * @pbram headers the connection headers received
     */
    privbte void updateHostCache(HandshakeResponse headers) {

        if(!hebders.hasXTryUltrapeers()) return;

        //get the ultrbpeers, and add those to the host cache
        String hostAddresses = hebders.getXTryUltrapeers();

        //tokenize to retrieve individubl addresses
        StringTokenizer st = new StringTokenizer(hostAddresses,
            Constbnts.ENTRY_SEPARATOR);

        List hosts = new ArrbyList(st.countTokens());
        while(st.hbsMoreTokens()){
            String bddress = st.nextToken().trim();
            try {
                Endpoint e = new Endpoint(bddress);
                hosts.bdd(e);
            } cbtch(IllegalArgumentException iae){
                continue;
            }
        }
        _cbtcher.add(hosts);        
    }



    /**
     * Initiblizes an outgoing connection created by createConnection or any
     * incomingConnection.  If this is bn incoming connection and there are no
     * slots bvailable, rejects it and throws IOException.
     *
     * @throws IOException on fbilure.  No cleanup is necessary if this happens.
     */
    privbte void initializeExternallyGeneratedConnection(ManagedConnection c)
		throws IOException {
        //For outgoing connections bdd it to the GUI and the fetcher lists now.
        //For incoming, we'll do this below bfter checking incoming connection
        //slots.  This keeps reject connections from bppearing in the GUI, as
        //well bs improving performance slightly.
        if (c.isOutgoing()) {
            synchronized(this) {
                connectionInitiblizing(c);
                // We've bdded a connection, so the need for connections went
                // down.
                bdjustConnectionFetchers();
            }
            RouterService.getCbllback().connectionInitializing(c);
        }

        try {
            c.initiblize();

        } cbtch(IOException e) {
            remove(c);
            throw e;
        }
        finblly {
            //if the connection received hebders, process the headers to
            //tbke steps based on the headers
            processConnectionHebders(c);
        }

        //If there's not spbce for the connection, destroy it.
        //It reblly should have been destroyed earlier, but this is just in case.
        if (!c.isOutgoing() && !bllowConnection(c)) {
            //No need to remove, since it hbsn't been added to any lists.
            throw new IOException("No spbce for connection");
        }

        //For incoming connections, bdd it to the GUI.  For outgoing connections
        //this wbs done at the top of the method.  See note there.
        if (! c.isOutgoing()) {
            synchronized(this) {
                connectionInitiblizingIncoming(c);
                // We've bdded a connection, so the need for connections went
                // down.
                bdjustConnectionFetchers();
            }
            RouterService.getCbllback().connectionInitializing(c);
        }

        completeConnectionInitiblization(c, false);
    }

    /**
     * Performs the steps necessbry to complete connection initialization.
     *
     * @pbram mc the <tt>ManagedConnection</tt> to finish initializing
     * @pbram fetched Specifies whether or not this connection is was fetched
     *  by b connection fetcher.  If so, this removes that connection from
     *  the list of fetched connections being initiblized, keeping the
     *  connection fetcher dbta in sync
     */
    privbte void completeConnectionInitialization(ManagedConnection mc,
                                                  boolebn fetched) {
        synchronized(this) {
            if(fetched) {
                _initiblizingFetchedConnections.remove(mc);
            }
            // If the connection wbs killed while initializing, we shouldn't
            // bnnounce its initialization
            boolebn connectionOpen = connectionInitialized(mc);
            if(connectionOpen) {
                RouterService.getCbllback().connectionInitialized(mc);
                setPreferredConnections();
            }
        }
    }

    /**
     * Gets the number of preferred connections to mbintain.
     */
    public int getPreferredConnectionCount() {
        return _preferredConnections;
    }

    /**
     * Determines if we're bttempting to maintain the idle connection count.
     */
    public boolebn isConnectionIdle() {
        return
         _preferredConnections == ConnectionSettings.IDLE_CONNECTIONS.getVblue();
    }

    /**
     * Sets the mbximum number of connections we'll maintain.
    */
    privbte void setPreferredConnections() {
        // if we're disconnected, do nothing.
        if(!ConnectionSettings.ALLOW_WHILE_DISCONNECTED.getVblue() &&
           _disconnectTime != 0)
            return;

        int oldPreferred = _preferredConnections;

        if(isSupernode())
            _preferredConnections = ConnectionSettings.NUM_CONNECTIONS.getVblue();
        else if(isIdle())
            _preferredConnections = ConnectionSettings.IDLE_CONNECTIONS.getVblue();
        else
            _preferredConnections = PREFERRED_CONNECTIONS_FOR_LEAF;

        if(oldPreferred != _preferredConnections)
            stbbilizeConnections();
    }

    /**
     * Determines if we're idle long enough to chbnge the number of connections.
     */
    privbte boolean isIdle() {
        return SystemUtils.getIdleTime() >= MINIMUM_IDLE_TIME;
    }


    //
    // End connection list mbnagement functions
    //


    //
    // Begin connection lbunching thread inner classes
    //

    /**
     * This threbd does the initialization and the message loop for
     * MbnagedConnections created through createConnectionAsynchronously and
     * crebteConnectionBlocking
     */
    privbte class OutgoingConnector implements Runnable {
        privbte final ManagedConnection _connection;
        privbte final boolean _doInitialization;

        /**
		 * Crebtes a new <tt>OutgoingConnector</tt> instance that will
		 * bttempt to create a connection to the specified host.
		 *
		 * @pbram connection the host to connect to
         */
        public OutgoingConnector(MbnagedConnection connection,
								 boolebn initialize) {
            _connection = connection;
            _doInitiblization = initialize;
        }

        public void run() {
            try {
				if(_doInitiblization) {
					initiblizeExternallyGeneratedConnection(_connection);
				}
				stbrtConnection(_connection);
            } cbtch(IOException ignored) {}
        }
    }

	/**
	 * Runs stbndard calls that should be made whenever a connection is fully
	 * estbblished and should wait for messages.
	 *
	 * @pbram conn the <tt>ManagedConnection</tt> instance to start
	 * @throws <tt>IOException</tt> if there is bn excpetion while looping
	 *  for messbges
	 */
	privbte void startConnection(ManagedConnection conn) throws IOException {
	    Threbd.currentThread().setName("MessageLoopingThread");
		if(conn.isGUESSUltrbpeer()) {
			QueryUnicbster.instance().addUnicastEndpoint(conn.getInetAddress(),
				conn.getPort());
		}

		// this cbn throw IOException
		conn.loopForMessbges();
	}

    /**
     * Asynchronously fetches b connection from hostcatcher, then does
     * then initiblization and message loop.
     *
     * The ConnectionFetcher is responsible for recording its instbntiation
     * by bdding itself to the fetchers list.  It is responsible  for recording
     * its debth by removing itself from the fetchers list only if it
     * "interrupts itself", thbt is, only if it establishes a connection. If
     * the threbd is interrupted externally, the interrupting thread is
     * responsible for recording the debth.
     */
    privbte class ConnectionFetcher extends ManagedThread {
        //set if this connectionfetcher is b preferencing fetcher
        privbte boolean _pref = false;
        /**
         * Tries to bdd a connection.  Should only be called from a thread
         * thbt has the enclosing ConnectionManager's monitor.  This method
         * is only cblled from adjustConnectionFetcher's, which has the same
         * locking requirement.
         */
        public ConnectionFetcher() {
            this(fblse);
        }

        public ConnectionFetcher(boolebn pref) {
            setNbme("ConnectionFetcher");
            _pref = pref;
            // Kick off the threbd.
            setDbemon(true);
            stbrt();
        }

        // Try b single connection
        public void mbnagedRun() {
            try {
                // Wbit for an endpoint.
                Endpoint endpoint = null;
                do {
                    endpoint = _cbtcher.getAnEndpoint();
                } while ( !IPFilter.instbnce().allow(endpoint.getAddress()) ||
                          isConnectedTo(endpoint.getAddress()) );
                Assert.thbt(endpoint != null);
                _connectionAttempts++;
                MbnagedConnection connection = new ManagedConnection(
                    endpoint.getAddress(), endpoint.getPort());
                //set preferencing
                connection.setLocblePreferencing(_pref);

                // If we've been trying to connect for bwhile, check to make
                // sure the user's internet connection is live.  We only do
                // this if we're not blready connected, have not made any
                // successful connections recently, bnd have not checked the
                // user's connection in the lbst little while or have very
                // few hosts left to try.
                long curTime = System.currentTimeMillis();
                if(!isConnected() &&
                   _connectionAttempts > 40 &&
                   ((curTime-_lbstSuccessfulConnect)>4000) &&
                   ((curTime-_lbstConnectionCheck)>60*60*1000)) {
                    _connectionAttempts = 0;
                    _lbstConnectionCheck = curTime;
                    LOG.debug("checking for live connection");
                    ConnectionChecker.checkForLiveConnection();
                }

                //Try to connect, recording success or fbilure so HostCatcher
                //cbn update connection history.  Note that we declare
                //success if we were bble to establish the TCP connection
                //but couldn't hbndshake (NoGnutellaOkException).
                try {
                    initiblizeFetchedConnection(connection, this);
                    _lbstSuccessfulConnect = System.currentTimeMillis();
                    _cbtcher.doneWithConnect(endpoint, true);
                    if(_pref) // if pref connection succeeded
                        _needPref = fblse;
                } cbtch (NoGnutellaOkException e) {
                    _lbstSuccessfulConnect = System.currentTimeMillis();
                    if(e.getCode() == HbndshakeResponse.LOCALE_NO_MATCH) {
                        //if it fbiled because of a locale matching issue
                        //rebdd to hostcatcher??
                        _cbtcher.add(endpoint, true,
                                     connection.getLocblePref());
                    }
                    else {
                        _cbtcher.doneWithConnect(endpoint, true);
                        _cbtcher.putHostOnProbation(endpoint);
                    }
                    throw e;
                } cbtch (IOException e) {
                    _cbtcher.doneWithConnect(endpoint, false);
                    _cbtcher.expireHost(endpoint);
                    throw e;
                }

				stbrtConnection(connection);
            } cbtch(IOException e) {
            } cbtch (InterruptedException e) {
                // Externblly generated interrupt.
                // The interrupting threbd has recorded the
                // debth of the fetcher, so just return.
                return;
            } cbtch(Throwable e) {
                //Internbl error!
                ErrorService.error(e);
            }
        }

        public String toString() {
            return "ConnectionFetcher";
        }
	}

    /**
     * This method notifies the connection mbnager that the user does not have
     * b live connection to the Internet to the best of our determination.
     * In this cbse, we notify the user with a message and maintain any
     * Gnutellb hosts we have already tried instead of discarding them.
     */
    public void noInternetConnection() {

        if(_butomaticallyConnecting) {
            // We've blready notified the user about their connection and we're
            // blread retrying automatically, so just return.
            return;
        }

        
        // Notify the user thbt they have no internet connection and that
        // we will butomatically retry
        MessbgeService.showError("NO_INTERNET_RETRYING",
                QuestionsHbndler.NO_INTERNET_RETRYING);
        
        // Kill bll of the ConnectionFetchers.
        disconnect();
        
        // Try to reconnect in 10 seconds, bnd then every minute after
        // thbt.
        RouterService.schedule(new Runnbble() {
            public void run() {
                // If the lbst time the user disconnected is more recent
                // thbn when we started automatically connecting, just
                // return without trying to connect.  Note thbt the
                // disconnect time is reset if the user selects to connect.
                if(_butomaticConnectTime < _disconnectTime) {
                    return;
                }
                
                if(!RouterService.isConnected()) {
                    // Try to re-connect.  Note this cbll resets the time
                    // for our lbst check for a live connection, so we may
                    // hit web servers bgain to check for a live connection.
                    connect();
                }
            }
        }, 10*1000, 2*60*1000);
        _butomaticConnectTime = System.currentTimeMillis();
        _butomaticallyConnecting = true;
        

        recoverHosts();
    }

    /**
     * Utility method thbt tells the host catcher to recover hosts from disk
     * if it doesn't hbve enough hosts.
     */
    privbte void recoverHosts() {
        // Notify the HostCbtcher that it should keep any hosts it has already
        // used instebd of discarding them.
        // The HostCbtcher can be null in testing.
        if(_cbtcher != null && _catcher.getNumHosts() < 100) {
            _cbtcher.recoverHosts();
        }
    }

    /**
     * Utility method to see if the pbssed in locale matches
     * thbt of the local client. As of now, we assume that
     * those clients not bdvertising locale as english locale
     */
    privbte boolean checkLocale(String loc) {
        if(loc == null)
            loc = /** bssume english if locale is not given... */
                ApplicbtionSettings.DEFAULT_LOCALE.getValue();
        return ApplicbtionSettings.LANGUAGE.getValue().equals(loc);
    }

}
