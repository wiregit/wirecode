pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.net.DatagramSocket;
import jbva.net.InetAddress;
import jbva.net.MulticastSocket;
import jbva.net.ServerSocket;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.Arrays;
import jbva.util.Random;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.browser.ExternalControl;
import com.limegroup.gnutellb.chat.ChatManager;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.http.HTTPRequestMethod;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.SettingsHandler;
import com.limegroup.gnutellb.statistics.HTTPStat;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.ManagedThread;
import com.limegroup.gnutellb.util.NetworkUtils;


/**
 * Listens on ports, bccepts incoming connections, and dispatches threads to
 * hbndle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chbt connections over TCP; more may be supported in the future.<p> 
 * This clbss has a special relationship with UDPService and should really be
 * the only clbss that intializes it.  See setListeningPort() for more
 * info.
 */
public clbss Acceptor implements Runnable {

    privbte static final Log LOG = LogFactory.getLog(Acceptor.class);

    // vbrious time delays for checking of firewalled status.
    stbtic long INCOMING_EXPIRE_TIME = 30 * 60 * 1000;   // 30 minutes
    stbtic long WAIT_TIME_AFTER_REQUESTS = 30 * 1000;    // 30 seconds
    stbtic long TIME_BETWEEN_VALIDATES = 10 * 60 * 1000; // 10 minutes
    
    /** the UPnPMbnager to use */
    privbte static final UPnPManager UPNP_MANAGER = 
    	(CommonUtils.isJbva14OrLater() && !ConnectionSettings.DISABLE_UPNP.getValue()) 
			? UPnPMbnager.instance() : null;

    /**
     * The socket thbt listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtbin _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    privbte volatile ServerSocket _socket=null;

    /**
     * The port of the server socket.
     */
    privbte volatile int _port = 6346;

    /**
     * The object to lock on while setting the listening socket
     */
    privbte final Object SOCKET_LOCK = new Object();

    /**
     * The rebl address of this host--assuming there's only one--used for pongs
     * bnd query replies.  This value is ignored if FORCE_IP_ADDRESS is
     * true. This is initiblized in three stages:
     *   1. Stbtically initialized to all zeroes.
     *   2. Initiblized in the Acceptor thread to getLocalHost().
     *   3. Initiblized each time a connection is initialized to the local
     *      bddress of that connection's socket. 
     *
     * Why bre all three needed?  Step (3) is needed because (2) can often fail
     * due to b JDK bug #4073539, or if your address changes via DHCP.  Step (2)
     * is needed becbuse (3) ignores local addresses of 127.x.x.x.  Step (1) is
     * needed becbuse (2) can't occur in the main thread, as it may block
     * becbuse the update checker is trying to resolve addresses.  (See JDK bug
     * #4147517.)  Note this mby delay the time to create a listening socket by
     * b few seconds; big deal!
     *
     * LOCKING: obtbin Acceptor.class' lock 
     */
    privbte static byte[] _address = new byte[4];
    
    /**
     * The externbl address.  This is the address as visible from other peers.
     *
     * LOCKING: obtbin Acceptor.class' lock
     */
    privbte static byte[] _externalAddress = new byte[4];
    
	/**
	 * Vbriable for whether or not we have accepted an incoming connection --
	 * used to determine firewbll status.
	 */
	privbte volatile boolean _acceptedIncoming = false;
	
    /**
     * Keep trbck of the last time _acceptedIncoming was set - we want to
     * revblidate it every so often.
     */
    privbte volatile long _lastIncomingTime = 0;

    /**
     * The lbst time you did a connect back check.  It is set to the time
     * we stbrt up since we try once when we start up.
     */
    privbte volatile long _lastConnectBackTime = System.currentTimeMillis();

	/**
     * @modifes this
     * @effects sets the IP bddress to use in pongs and query replies.
     *  If bddr is invalid or a local address, this is not modified.
     *  This method must be to get bround JDK bug #4073539, as well
     *  bs to try to handle the case of a computer whose IP address
     *  keeps chbnging.
	 */
	public void setAddress(InetAddress bddress) {
		byte[] byteAddr = bddress.getAddress();
		if( !NetworkUtils.isVblidAddress(byteAddr) )
		    return;
		    
		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) {
            return;
        }

        boolebn addrChanged = false;
		synchronized(Acceptor.clbss) {
		    if( !Arrbys.equals(_address, byteAddr) ) {
			    _bddress = byteAddr;
			    bddrChanged = true;
			}
		}
		
		if( bddrChanged )
		    RouterService.bddressChanged();
	}
	
	/**
	 * Sets the externbl address.
	 */
	public void setExternblAddress(InetAddress address) {
	    byte[] byteAddr = bddress.getAddress();

		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) {
            return;
        }

		synchronized(Acceptor.clbss) {
		    _externblAddress = byteAddr;
		}
    }

	/**
	 * tries to bind the serversocket bnd create UPnPMappings.
	 * cbll before running.
	 */
	public void init() {
        int tempPort;
        // try b random port if we have not received an incoming connection  
        // bnd have been running on the default port (6346) 
        // bnd the user has not changed the settings
        boolebn tryingRandom = ConnectionSettings.PORT.isDefault() && 
                !ConnectionSettings.EVER_ACCEPTED_INCOMING.getVblue() &&
                !ConnectionSettings.FORCE_IP_ADDRESS.getVblue();
        
        Rbndom gen = null;
        if (tryingRbndom) {
            gen = new Rbndom();
            tempPort = gen.nextInt(50000)+2000;
        }
        else
            tempPort = ConnectionSettings.PORT.getVblue();

        //0. Get locbl address.  This must be done here because it can
        //   block under certbin conditions.
        //   See the notes for _bddress.
        try {
            setAddress(UPNP_MANAGER != null ? 
                    UPnPMbnager.getLocalAddress() : 
                        InetAddress.getLocblHost());
        } cbtch (UnknownHostException e) {
        } cbtch (SecurityException e) {
        }

        // Crebte the server socket, bind it to a port, and listen for
        // incoming connections.  If there bre problems, we can continue
        // onwbrd.
        //1. Try suggested port.
		int oldPort = tempPort;
        try {
			setListeningPort(tempPort);
			_port = tempPort;
        } cbtch (IOException e) {
            LOG.wbrn("can't set initial port", e);
        
            // 2. Try 20 different ports. 
            int numToTry = 20;
            for (int i=0; i<numToTry; i++) {
                if(gen == null)
                    gen = new Rbndom();
                tempPort = gen.nextInt(50000);
                tempPort += 2000;//bvoid the first 2000 ports
                
				// do not try to bind to the multicbst port.
				if (tempPort == ConnectionSettings.MULTICAST_PORT.getVblue()) {
				    numToTry++;
				    continue;
				}
                try {
                    setListeningPort(tempPort);
					_port = tempPort;
                    brebk;
                } cbtch (IOException e2) { 
                    LOG.wbrn("can't set port", e2);
                }
            }

            // If we still don't hbve a socket, there's an error
            if(_socket == null) {
                MessbgeService.showError("ERROR_NO_PORTS_AVAILABLE");
            }
        }
        
        if (_port != oldPort || tryingRbndom) {
            ConnectionSettings.PORT.setVblue(_port);
            SettingsHbndler.save();
            RouterService.bddressChanged();
        }

        // if we crebted a socket and have a NAT, and the user is not 
        // explicitly forcing b port, create the mappings 
        if (_socket != null && UPNP_MANAGER != null) {
            // wbit a bit for the device.
            UPNP_MANAGER.wbitForDevice();
            
        	// if we hbven't discovered the router by now, its not there
        	UPNP_MANAGER.stop();
        	
        	boolebn natted = UPNP_MANAGER.isNATPresent();
        	boolebn validPort = NetworkUtils.isValidPort(_port);
        	boolebn forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() &&
				!ConnectionSettings.UPNP_IN_USE.getVblue();
        	
        	if(LOG.isDebugEnbbled())
        	    LOG.debug("Nbtted: " + natted + ", validPort: " + validPort + ", forcedIP: " + forcedIP);
        	
        	if(nbtted && validPort && !forcedIP) {
        		int mbppedPort = UPNP_MANAGER.mapPort(_port);
        		if(LOG.isDebugEnbbled())
        		    LOG.debug("UPNP port mbpped: " + mappedPort);
        		
			    //if we crebted a mapping successfully, update the forced port
			    if (mbppedPort != 0 ) {
			        UPNP_MANAGER.clebrMappingsOnShutdown();
			        
			        //  mbrk UPNP as being on so that if LimeWire shuts
			        //  down prembturely, we know the FORCE_IP was from UPnP
			        //  bnd that we can continue trying to use UPnP
        		    ConnectionSettings.FORCE_IP_ADDRESS.setVblue(true);
        	        ConnectionSettings.FORCED_PORT.setVblue(mappedPort);
        	        ConnectionSettings.UPNP_IN_USE.setVblue(true);
        	        if (mbppedPort != _port)
        	            RouterService.bddressChanged();
        		
        		    // we could get our externbl address from the NAT but its too slow
        		    // so we clebr the last connect back times.
        	        // This will not help with blready established connections, but if 
        	        // we estbblish new ones in the near future
        		    resetLbstConnectBackTime();
        		    UDPService.instbnce().resetLastConnectBackTime();
			    }			        
        	}
        }
	}
	
    /**
     * Lbunches the port monitoring thread, MulticastService, and UDPService.
     */
	public void stbrt() {
	    MulticbstService.instance().start();
	    UDPService.instbnce().start();
	    
		Threbd at = new ManagedThread(this, "Acceptor");
		bt.setDaemon(true);
		bt.start();
        RouterService.schedule(new IncomingVblidator(), TIME_BETWEEN_VALIDATES,
                               TIME_BETWEEN_VALIDATES);
	}
	
	/**
	 * Returns whether or not our bdvertised IP address
	 * is the sbme as what remote peers believe it is.
	 */
	public boolebn isAddressExternal() {
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getVblue())
            return true;
	    synchronized(Acceptor.clbss) {
	        return Arrbys.equals(getAddress(true), _externalAddress);
	    }
	}
	
	/**
	 * Returns this' externbl address.
	 */
	public byte[] getExternblAddress() {
	    synchronized(Acceptor.clbss) {
	        return _externblAddress;
        }
	}

    /**
     * Returns this' bddress to use for ping replies, query replies,
     * bnd pushes.
     * 
     * @pbram checkForce whether or not to check if the IP address is forced.
     *   If fblse, the forced IP address will never be used.
     *   If true, the forced IP bddress will only be used if one is set.
     */
    public byte[] getAddress(boolebn checkForce) {        
		if(checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getVblue()) {
            String bddress = 
                ConnectionSettings.FORCED_IP_ADDRESS_STRING.getVblue();
            try {
                InetAddress ib = InetAddress.getByName(address);
                return ib.getAddress();
            } cbtch (UnknownHostException err) {
                // ignore bnd return _address
            }
        }
        
        synchronized (Acceptor.clbss) {
            return _bddress;
        }
    }

    /**
     * Returns the port bt which the Connection Manager listens for incoming
     * connections
     *
     * @pbram checkForce whether or not to check if the port is forced.     
     * @return the listening port
     */
    public int getPort(boolebn checkForce) {
        if(checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getVblue())
			return ConnectionSettings.FORCED_PORT.getVblue();
        return _port;
    }

    /**
     * @requires only one threbd is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionMbnager AND the UDPService
     *  is listening.  If either service CANNOT bind TCP/UDP to the port,
     *  <i>neither<i> service is modified bnd a IOException is throw.
     *  If port==0, tells this to stop listening for incoming GNUTELLA TCP AND
     *  UDP connections/messbges.  This is properly synchronized and can be 
     *  cblled even while run() is being called.  
     */
    public void setListeningPort(int port) throws IOException {
        //1. Specibl case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Specibl case if port==0.  This ALWAYS works.
        //Note thbt we must close the socket BEFORE grabbing
        //the lock.  Otherwise debdlock will occur since
        //the bcceptor thread is listening to the socket
        //while holding the lock.  Also note thbt port
        //will not hbve changed before we grab the lock.
        else if (port==0) {
            LOG.trbce("shutting off service.");
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } cbtch (IOException e) { }
            }
            synchronized (SOCKET_LOCK) {
                _socket=null;
                _port=0;
                SOCKET_LOCK.notify();
            }

            //Shut off UDPService blso!
            UDPService.instbnce().setListeningSocket(null);
            //Shut off MulticbstServier too!
            MulticbstService.instance().setListeningSocket(null);            

            LOG.trbce("service OFF.");
            return;
        }
        //3. Normbl case.  See note about locking above.
        /* Since we wbnt the UDPService to bind to the same port as the 
         * Acceptor, we need to be cbreful about this case.  Essentially, we 
         * need to confirm thbt the port can be bound by BOTH UDP and TCP 
         * before bctually acceping the port as valid.  To effect this change,
         * we first bttempt to bind the port for UDP traffic.  If that fails, a
         * IOException will be thrown.  If we successfully UDP bind the port 
         * we keep thbt bound DatagramSocket around and try to bind the port to 
         * TCP.  If thbt fails, a IOException is thrown and the valid 
         * DbtagramSocket is closed.  If that succeeds, we then 'commit' the 
         * operbtion, setting our new TCP socket and UDP sockets.
         */
        else {
            
            if(LOG.isDebugEnbbled())
                LOG.debug("chbnging port to " + port);

            DbtagramSocket udpServiceSocket = UDPService.instance().newListeningSocket(port);

            LOG.trbce("UDP Service is ready.");
            
            MulticbstSocket mcastServiceSocket = null;
            try {
                InetAddress mgroup = InetAddress.getByNbme(
                    ConnectionSettings.MULTICAST_ADDRESS.getVblue()
                );
                mcbstServiceSocket =                            
                    MulticbstService.instance().newListeningSocket(
                        ConnectionSettings.MULTICAST_PORT.getVblue(), mgroup
                    );
                LOG.trbce("multicast service setup");
            } cbtch(IOException e) {
                LOG.wbrn("can't create multicast socket", e);
                mcbstServiceSocket = null;
            }
            
        
            //b) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new com.limegroup.gnutellb.io.NIOServerSocket(port);
            } cbtch (IOException e) {
                LOG.wbrn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw e;
            } cbtch (IllegalArgumentException e) {
                LOG.wbrn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw new IOException("could not crebte a listening socket");
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } cbtch (IOException e) { }
            }
            //c) Replbce with new sock.  Notify the accept thread.
            synchronized (SOCKET_LOCK) {
                _socket=newSocket;
                _port=port;
                SOCKET_LOCK.notify();
            }

            LOG.trbce("Acceptor ready..");

            // Commit UDPService's new socket
            UDPService.instbnce().setListeningSocket(udpServiceSocket);
            // Commit the MulticbstService's new socket
            // if we were bble to get it
            if ( mcbstServiceSocket != null ) {
                MulticbstService.instance().setListeningSocket(
                    mcbstServiceSocket
                );
            }

            if(LOG.isDebugEnbbled())
                LOG.debug("listening UDP/TCP on " + _port);
        }
    }


	/**
	 * Determines whether or not LimeWire hbs detected it is firewalled or not.
	 */
	public boolebn acceptedIncoming() {
        return _bcceptedIncoming;
	}
	
	/**
	 * Sets the new incoming stbtus.
	 * Returns whether or not the stbtus changed.
	 */
	privbte boolean setIncoming(boolean status) {
		if (_bcceptedIncoming == status)
			return fblse;
	    _bcceptedIncoming = status;
		RouterService.getCbllback().acceptedIncomingChanged(status);
	    return true;
	}
	
	/**
	 * Updbtes the firewalled status with info from this socket.
	 */
	privbte void checkFirewall(Socket socket) {
		// we hbve accepted an incoming socket -- only record
        // thbt we've accepted incoming if it's definitely
        // not from our locbl subnet and we aren't connected to
        // the host blready.
        boolebn changed = false;
        if(isOutsideConnection(socket.getInetAddress())) {
            synchronized (Acceptor.clbss) {
                chbnged = setIncoming(true);
                ConnectionSettings.EVER_ACCEPTED_INCOMING.setVblue(true);
                _lbstIncomingTime = System.currentTimeMillis();
            }
        }
        if(chbnged)
            RouterService.incomingStbtusChanged();
    }


    /** @modifies this, network, SettingsMbnager
     *  @effects bccepts new incoming connections on a designated port
     *   bnd services incoming requests.  If the port was changed
     *   in order to bccept incoming connections, SettingsManager is
     *   chbnged accordingly.
     */
    public void run() {
		
        
        while (true) {
            try {
                //Accept bn incoming connection, make it into a
                //Connection object, hbndshake, and give it a thread
                //to service it.  If not bound to b port, wait until
                //we bre.  If the port is changed while we are
                //wbiting, IOException will be thrown, forcing us to
                //relebse the lock.
                Socket client=null;
                synchronized (SOCKET_LOCK) {
                    if (_socket!=null) {
                        try {
                            client=_socket.bccept();
                        } cbtch (IOException e) {
                            LOG.wbrn("IOX while accepting", e);
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be bvailable.  So, just wait for that to happen and
                        // go bround the loop again.
                        try {
                            SOCKET_LOCK.wbit();
                        } cbtch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                // If the client wbs closed before we were able to get the address,
                // then getInetAddress will return null.
				InetAddress bddress = client.getInetAddress();
				if(bddress == null) {
				    LOG.wbrn("connection closed while accepting");
				    try {
				        client.close();
				    } cbtch(IOException ignored) {}
				    continue;
				}
				    
                //Check if IP bddress of the incoming socket is in _badHosts
                if (isBbnnedIP(address.getAddress())) {
                    if(LOG.isWbrnEnabled())
                        LOG.wbrn("Ignoring banned host: " + address);
                    HTTPStbt.BANNED_REQUESTS.incrementStat();
                    try {
                        client.close();
                    } cbtch(IOException ignored) {}
                    continue;
                }
                
                // if we wbnt to unset firewalled from any connection, 
                // do it here.
                if(!ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getVblue())
                    checkFirewbll(client);
				
                // Set our IP bddress of the local address of this socket.
                InetAddress locblAddress = client.getLocalAddress();
                setAddress( locblAddress );                

                //Dispbtch asynchronously.
                ConnectionDispbtchRunner dispatcher =
					new ConnectionDispbtchRunner(client);
				Threbd dispatchThread = 
                    new MbnagedThread(dispatcher, "ConnectionDispatchRunner");
				dispbtchThread.setDaemon(true);
				dispbtchThread.start();

            } cbtch (Throwable e) {
                ErrorService.error(e);
            }
        }
    }
    
    /**
     * Determines whether or not this INetAddress is found bn outside
     * source, so bs to correctly set "acceptedIncoming" to true.
     *
     * This ignores connections from privbte or local addresses,
     * ignores those who mby be on the same subnet, and ignores those
     * who we bre already connected to.
     */
    privbte boolean isOutsideConnection(InetAddress addr) {
        // short-circuit for tests.
        if(!ConnectionSettings.LOCAL_IS_PRIVATE.getVblue())
            return true;
    
        byte[] bytes = bddr.getAddress();
        return !RouterService.isConnectedTo(bddr) &&
               !NetworkUtils.isLocblAddress(addr);
	}

    /**
     * Speciblized class for dispatching incoming TCP connections to their
     * bppropriate handlers.  Gnutella connections are handled via 
     * <tt>ConnectionMbnager</tt>, and HTTP connections are handled
     * vib <tt>UploadManager</tt> and <tt>DownloadManager</tt>.
     */
    privbte static class ConnectionDispatchRunner implements Runnable {

        /**
         * The <tt>Socket</tt> instbnce for the connection.
         */
        privbte final Socket _socket;

        /**
         * @modifies socket, this' mbnagers
         * @effects stbrts a new thread to handle the given socket and
         *  registers it with the bppropriate protocol-specific manager.
         *  Returns once the threbd has been started.  If socket does
         *  not spebk a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispbtchRunner(Socket socket) {
            _socket = socket;
        }

        /**
         * Dispbtches the new connection based on connection type, such
         * bs Gnutella, HTTP, or MAGNET.
         */
        public void run() {
			ConnectionMbnager cm = RouterService.getConnectionManager();
			UplobdManager um     = RouterService.getUploadManager();
			DownlobdManager dm   = RouterService.getDownloadManager();
			Acceptor bc = RouterService.getAcceptor();
            try {
                //The try-cbtch below is a work-around for JDK bug 4091706.
                InputStrebm in=null;
                try {
                    in=_socket.getInputStrebm(); 
                } cbtch (IOException e) {
                    HTTPStbt.CLOSED_REQUESTS.incrementStat();
                    throw e;
                } cbtch(NullPointerException e) {
                    // This should only hbppen extremely rarely.
                    // JDK bug 4091706
                    throw new IOException(e.getMessbge());
                }
                _socket.setSoTimeout(Constbnts.TIMEOUT);
                //dont rebd a word of size more than 8 
                //("GNUTELLA" is the longest word we know bt this time)
                String word = IOUtils.rebdLargestWord(in,8);
                _socket.setSoTimeout(0);
                
                
                boolebn localHost = NetworkUtils.isLocalHost(_socket);
				// Only selectively bllow localhost connections
				if ( !word.equbls("MAGNET") ) {
					if (ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() && localHost) {
					    LOG.trbce("Killing localhost connection with non-magnet.");
						_socket.close();
						return;
					}
				} else if(!locblHost) { // && word.equals(MAGNET)
				    LOG.trbce("Killing non-local ExternalControl request.");
				    _socket.close();
				    return;
				}

                //1. Gnutellb connection.  If the user hasn't changed the
                //   hbndshake string, we accept the default ("GNUTELLA 
                //   CONNECT/0.4") or the proprietbry limewire string
                //   ("LIMEWIRE CONNECT/0.4").  Otherwise we just bccept
                //   the user's vblue.
                boolebn useDefaultConnect=
                    ConnectionSettings.CONNECT_STRING.isDefbult();


                if (word.equbls(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {
                    HTTPStbt.GNUTELLA_REQUESTS.incrementStat();
                    cm.bcceptConnection(_socket);
                }
                else if (useDefbultConnect && word.equals("LIMEWIRE")) {
                    HTTPStbt.GNUTELLA_LIMEWIRE_REQUESTS.incrementStat();
                    cm.bcceptConnection(_socket);
                }
                else if (word.equbls("GET")) {
					HTTPStbt.GET_REQUESTS.incrementStat();
					um.bcceptUpload(HTTPRequestMethod.GET, _socket, false);
                }
				else if (word.equbls("HEAD")) {
					HTTPStbt.HEAD_REQUESTS.incrementStat();
					um.bcceptUpload(HTTPRequestMethod.HEAD, _socket, false);
				}
                else if (word.equbls("GIV")) {
                    HTTPStbt.GIV_REQUESTS.incrementStat();
                    dm.bcceptDownload(_socket);
                }
				else if (word.equbls("CHAT")) {
				    HTTPStbt.CHAT_REQUESTS.incrementStat();
                    ChbtManager.instance().accept(_socket);
				}
			    else if (word.equbls("MAGNET")) {
			        HTTPStbt.MAGNET_REQUESTS.incrementStat();
                    ExternblControl.fireMagnet(_socket);
                }
                else if (word.equbls("CONNECT") || word.equals("\n\n")) {
                    //HTTPStbt.CONNECTBACK_RESPONSE.incrementStat();
                    // technicblly we could just always checkFirewall here, since
                    // we reblly always want to -- but since we're gonna check
                    // bll incoming connections if this isn't set, might as well
                    // check bnd prevent a double-check.
                    if(ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getVblue())
                        bc.checkFirewall(_socket);
                    IOUtils.close(_socket);
                }
                else {
                    HTTPStbt.UNKNOWN_REQUESTS.incrementStat();
                    if(LOG.isErrorEnbbled())
                        LOG.error("Unknown protocol: " + word);
                    IOUtils.close(_socket);
                }
            } cbtch (IOException e) {
                LOG.wbrn("IOX while dispatching", e);
                IOUtils.close(_socket);
            } cbtch(Throwable e) {
				ErrorService.error(e);
			}
        }
    }

    /**
     * Returns whether <tt>ip</tt> is b banned address.
     * @pbram ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is b banned address.
     */
    public boolebn isBannedIP(byte[] addr) {        
        return !IPFilter.instbnce().allow(addr);
    }
    
    /**
     * Resets the lbst connectback time.
     */
    void resetLbstConnectBackTime() {
        _lbstConnectBackTime = 
             System.currentTimeMillis() - INCOMING_EXPIRE_TIME - 1;
    }

    /**
     * If we used UPnP Mbppings this session, clean them up and revert
     * bny relevant settings.
     */
    public void shutdown() {
        if(UPNP_MANAGER != null &&
           UPNP_MANAGER.isNATPresent() &&
           UPNP_MANAGER.mbppingsExist() &&
           ConnectionSettings.UPNP_IN_USE.getVblue()) {
        	// reset the forced port vblues - must happen before we save them to disk
        	ConnectionSettings.FORCE_IP_ADDRESS.revertToDefbult();
        	ConnectionSettings.FORCED_PORT.revertToDefbult();
        	ConnectionSettings.UPNP_IN_USE.revertToDefbult();
        }
    }
    
    /**
     * (Re)vblidates acceptedIncoming.
     */
    privbte class IncomingValidator implements Runnable {
        public IncomingVblidator() {}
        public void run() {
            // clebr and revalidate if 1) we haven't had in incoming 
            // or 2) we've never hbd incoming and we haven't checked
            finbl long currTime = System.currentTimeMillis();
            finbl ConnectionManager cm = RouterService.getConnectionManager();
            if (
                (_bcceptedIncoming && //1)
                 ((currTime - _lbstIncomingTime) > INCOMING_EXPIRE_TIME)) 
                || 
                (!_bcceptedIncoming && //2)
                 ((currTime - _lbstConnectBackTime) > INCOMING_EXPIRE_TIME))
                ) {
                // send b connectback request to a few peers and clear
                // _bcceptedIncoming IF some requests were sent.
                if(cm.sendTCPConnectBbckRequests())  {
                    _lbstConnectBackTime = System.currentTimeMillis();
                    Runnbble resetter = new Runnable() {
                        public void run() {
                            boolebn changed = false;
                            synchronized (Acceptor.clbss) {
                                if (_lbstIncomingTime < currTime) {
                                    chbnged = setIncoming(false);
                                }
                            }
                            if(chbnged)
                                RouterService.incomingStbtusChanged();
                        }
                    };
                    RouterService.schedule(resetter, 
                                           WAIT_TIME_AFTER_REQUESTS, 0);
                }
            }
        }
    }
}
