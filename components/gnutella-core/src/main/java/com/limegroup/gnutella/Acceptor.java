package com.limegroup.gnutella;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.net.AsyncConnectionDispatcher;
import org.limewire.net.BlockingConnectionDispatcher;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.MessageService;
import org.limewire.setting.SettingsGroupManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * Listens on ports, accepts incoming connections, and dispatches threads to
 * handle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chat connections over TCP; more may be supported in the future.<p> 
 * This class has a special relationship with UDPService and should really be
 * the only class that intializes it.  See setListeningPort() for more
 * info.
 */
@Singleton
public class Acceptor implements ConnectionAcceptor, SocketProcessor {

    private static final Log LOG = LogFactory.getLog(Acceptor.class);

    // various time delays for checking of firewalled status.
    static long INCOMING_EXPIRE_TIME = 30 * 60 * 1000;   // 30 minutes
    static long WAIT_TIME_AFTER_REQUESTS = 30 * 1000;    // 30 seconds
    static long TIME_BETWEEN_VALIDATES = 10 * 60 * 1000; // 10 minutes
    
    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;

    /**
     * The port of the server socket.
     */
    private volatile int _port = 6346;
    
    /**
     * The real address of this host--assuming there's only one--used for pongs
     * and query replies.  This value is ignored if FORCE_IP_ADDRESS is
     * true. This is initialized in three stages:
     *   1. Statically initialized to all zeroes.
     *   2. Initialized in the Acceptor thread to getLocalHost().
     *   3. Initialized each time a connection is initialized to the local
     *      address of that connection's socket. 
     *
     * Why are all three needed?  Step (3) is needed because (2) can often fail
     * due to a JDK bug #4073539, or if your address changes via DHCP.  Step (2)
     * is needed because (3) ignores local addresses of 127.x.x.x.  Step (1) is
     * needed because (2) can't occur in the main thread, as it may block
     * because the update checker is trying to resolve addresses.  (See JDK bug
     * #4147517.)  Note this may delay the time to create a listening socket by
     * a few seconds; big deal!
     *
     * LOCKING: obtain Acceptor.class' lock 
     */
    private byte[] _address = new byte[4];
    
    /**
     * The external address.  This is the address as visible from other peers.
     *
     * LOCKING: obtain Acceptor.class' lock
     */
    private byte[] _externalAddress = new byte[4];
    
	/**
	 * Variable for whether or not we have accepted an incoming connection --
	 * used to determine firewall status.
	 */
    @InspectablePrimitive("accepted incoming")
	private volatile boolean _acceptedIncoming = false;
	
    /**
     * Keep track of the last time _acceptedIncoming was set - we want to
     * revalidate it every so often.
     */
    private volatile long _lastIncomingTime = 0;

    /**
     * The last time you did a connect back check.  It is set to the time
     * we start up since we try once when we start up.
     */
    private volatile long _lastConnectBackTime = System.currentTimeMillis();
    
    /**
     * Whether or not this Acceptor was started.  All connections accepted prior
     * to starting are dropped.
     */
    private volatile boolean _started;
    
    private final NetworkManager networkManager;
    private final Provider<UDPService> udpService;
    private final Provider<MulticastService> multicastService;
    private final Provider<ConnectionDispatcher> connectionDispatcher;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<ActivityCallback> activityCallback;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final ConnectionServices connectionServices;
    private final Provider<UPnPManager> upnpManager;
    
    private final boolean upnpEnabled; 
    
    @Inject
    public Acceptor(NetworkManager networkManager,
            Provider<UDPService> udpService,
            Provider<MulticastService> multicastService,
            @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<ActivityCallback> activityCallback,
            Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter, ConnectionServices connectionServices,
            Provider<UPnPManager> upnpManager) {
        this.networkManager = networkManager;
        this.udpService = udpService;
        this.multicastService = multicastService;
        this.connectionDispatcher = connectionDispatcher;
        this.backgroundExecutor = backgroundExecutor;
        this.activityCallback = activityCallback;
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.connectionServices = connectionServices;
        this.upnpManager = upnpManager;
        
        // capture UPnP setting on construction, so start/stop can
        // work even if setting changes between the two.
        upnpEnabled = !ConnectionSettings.DISABLE_UPNP.getValue();
    }
    
    /** Returns true if UPnP was enabled when Acceptor was constructed. */
    private boolean isUPnPEnabled() {
        return upnpEnabled;
    }

    /**
     * @modifes this
     * @effects sets the IP address to use in pongs and query replies.
     *  If addr is invalid or a local address, this is not modified.
     *  This method must be to get around JDK bug #4073539, as well
     *  as to try to handle the case of a computer whose IP address
     *  keeps changing.
	 */
	public void setAddress(InetAddress address) {
		byte[] byteAddr = address.getAddress();
		if( !NetworkUtils.isValidAddress(byteAddr) )
		    return;
		    
		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return;
        }

        boolean addrChanged = false;
		synchronized(Acceptor.class) {
		    if( !Arrays.equals(_address, byteAddr) ) {
			    _address = byteAddr;
			    addrChanged = true;
			}
		}
		
		if( addrChanged )
		    networkManager.addressChanged();
	}
	
	/**
	 * Sets the external address.
	 */
	public void setExternalAddress(InetAddress address) {
	    byte[] byteAddr = address.getAddress();

		if( byteAddr[0] == 127 &&
           ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return;
        }

		synchronized(Acceptor.class) {
		    _externalAddress = byteAddr;
		}
    }

	/**
	 * tries to bind the serversocket and create UPnPMappings.
	 * call before running.
	 */
	public void init() {
        int tempPort;
        // try a random port if we have not received an incoming connection  
        // and have been running on the default port (6346) 
        // and the user has not changed the settings
        boolean tryingRandom = ConnectionSettings.PORT.isDefault() && 
                !ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() &&
                !ConnectionSettings.FORCE_IP_ADDRESS.getValue();
        
        Random gen = null;
        if (tryingRandom) {
            gen = new Random();
            tempPort = gen.nextInt(50000)+2000;
        }
        else
            tempPort = ConnectionSettings.PORT.getValue();

        //0. Get local address.  This must be done here because it can
        //   block under certain conditions.
        //   See the notes for _address.
        try {
            if(isUPnPEnabled())
                setAddress(NetworkUtils.getLocalAddress());
            else
                setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
		int oldPort = tempPort;
        try {
			setListeningPort(tempPort);
			_port = tempPort;
        } catch (IOException e) {
            LOG.warn("can't set initial port", e);
        
            // 2. Try 20 different ports. 
            int numToTry = 20;
            for (int i=0; i<numToTry; i++) {
                if(gen == null)
                    gen = new Random();
                tempPort = gen.nextInt(50000);
                tempPort += 2000;//avoid the first 2000 ports
                
				// do not try to bind to the multicast port.
				if (tempPort == ConnectionSettings.MULTICAST_PORT.getValue()) {
				    numToTry++;
				    continue;
				}
                try {
                    setListeningPort(tempPort);
					_port = tempPort;
                    break;
                } catch (IOException e2) { 
                    LOG.warn("can't set port", e2);
                }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null) {
                MessageService.showError(I18n.marktr("LimeWire was unable to set up a port to listen for incoming connections. Some features of LimeWire may not work as expected."));
            }
        }
        
        if (_port != oldPort || tryingRandom) {
            ConnectionSettings.PORT.setValue(_port);
            SettingsGroupManager.instance().save();
            networkManager.addressChanged();
        }

        setupUPnP();
	}
	
	private void setupUPnP() {
        // if we created a socket and have a NAT, and the user is not 
        // explicitly forcing a port, create the mappings 
        if (_socket != null && isUPnPEnabled()) {
            // wait a bit for the device.
            upnpManager.get().waitForDevice();
            
        	// if we haven't discovered the router by now, its not there
            upnpManager.get().stop();
        	
        	boolean natted = upnpManager.get().isNATPresent();
        	boolean validPort = NetworkUtils.isValidPort(_port);
        	boolean forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() &&
				!ConnectionSettings.UPNP_IN_USE.getValue();
        	
        	if(LOG.isDebugEnabled())
        	    LOG.debug("Natted: " + natted + ", validPort: " + validPort + ", forcedIP: " + forcedIP);
        	
        	if(natted && validPort && !forcedIP) {
        		int mappedPort = upnpManager.get().mapPort(_port);
        		if(LOG.isDebugEnabled())
        		    LOG.debug("UPNP port mapped: " + mappedPort);
        		
			    //if we created a mapping successfully, update the forced port
			    if (mappedPort != 0 ) {
			        upnpManager.get().clearMappingsOnShutdown();
			        
			        //  mark UPNP as being on so that if LimeWire shuts
			        //  down prematurely, we know the FORCE_IP was from UPnP
			        //  and that we can continue trying to use UPnP
        		    ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        	        ConnectionSettings.FORCED_PORT.setValue(mappedPort);
        	        ConnectionSettings.UPNP_IN_USE.setValue(true);
        	        if (mappedPort != _port)
        	            networkManager.addressChanged();
        		
        		    // we could get our external address from the NAT but its too slow
        		    // so we clear the last connect back times.
        	        // This will not help with already established connections, but if 
        	        // we establish new ones in the near future
        		    resetLastConnectBackTime();
        		    udpService.get().resetLastConnectBackTime();
			    }			        
        	}
        }
	}
	
    /**
     * Launches the port monitoring thread, MulticastService, and UDPService.
     */
	public void start() {
        multicastService.get().start();
        udpService.get().start();
        backgroundExecutor.scheduleWithFixedDelay(new IncomingValidator(),
                TIME_BETWEEN_VALIDATES, TIME_BETWEEN_VALIDATES,
                TimeUnit.MILLISECONDS);
        connectionDispatcher.get().addConnectionAcceptor(this, false, "CONNECT", "\n\n");
        _started = true;
    }
	
	/**
     * Returns whether or not our advertised IP address is the same as what remote peers believe it is.
     */
	public boolean isAddressExternal() {
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
            return true;
	    synchronized(Acceptor.class) {
	        return Arrays.equals(getAddress(true), _externalAddress);
	    }
	}
	
	public boolean isBlocking() {
	    return false;
	}
	
	/**
	 * Returns this' external address.
	 */
	public byte[] getExternalAddress() {
	    synchronized(Acceptor.class) {
	        return _externalAddress;
        }
	}

    /**
     * Returns this' address to use for ping replies, query replies,
     * and pushes.
     * 
     * @param checkForce whether or not to check if the IP address is forced.
     *   If false, the forced IP address will never be used.
     *   If true, the forced IP address will only be used if one is set.
     */
    public byte[] getAddress(boolean checkForce) {        
		if(checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {
            String address = 
                ConnectionSettings.FORCED_IP_ADDRESS_STRING.getValue();
            try {
                InetAddress ia = InetAddress.getByName(address);
                byte[] addr = ia.getAddress();
                if(addr != null)
                    return addr;
            } catch (UnknownHostException err) {
                // ignore and return _address
            }
        }
        
        synchronized (Acceptor.class) {
            return _address;
        }
    }

    public ConnectionDispatcher getConnectionDispatcher() {
        return connectionDispatcher.get();
    }
    
    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     *
     * @param checkForce whether or not to check if the port is forced.     
     * @return the listening port
     */
    public int getPort(boolean checkForce) {
        if(checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getValue())
			return ConnectionSettings.FORCED_PORT.getValue();
        return _port;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager AND the UDPService
     *  is listening.  If either service CANNOT bind TCP/UDP to the port,
     *  <i>neither<i> service is modified and a IOException is throw.
     *  If port==0, tells this to stop listening for incoming GNUTELLA TCP AND
     *  UDP connections/messages.  This is properly synchronized and can be 
     *  called even while run() is being called.  
     */
    public void setListeningPort(int port) throws IOException {
        //1. Special case: if unchanged, do nothing.
        if (_socket!=null && _port==port)
            return;
        //2. Special case if port==0.  This ALWAYS works.
        //Note that we must close the socket BEFORE grabbing
        //the lock.  Otherwise deadlock will occur since
        //the acceptor thread is listening to the socket
        //while holding the lock.  Also note that port
        //will not have changed before we grab the lock.
        else if (port==0) {
            LOG.trace("shutting off service.");
            IOUtils.close(_socket);            
            _socket=null;
            _port=0;

            //Shut off UDPService also!
            udpService.get().setListeningSocket(null);
            //Shut off MulticastServier too!
            multicastService.get().setListeningSocket(null);            

            LOG.trace("service OFF.");
            return;
        }
        //3. Normal case.  See note about locking above.
        /* Since we want the UDPService to bind to the same port as the 
         * Acceptor, we need to be careful about this case.  Essentially, we 
         * need to confirm that the port can be bound by BOTH UDP and TCP 
         * before actually acceping the port as valid.  To effect this change,
         * we first attempt to bind the port for UDP traffic.  If that fails, a
         * IOException will be thrown.  If we successfully UDP bind the port 
         * we keep that bound DatagramSocket around and try to bind the port to 
         * TCP.  If that fails, a IOException is thrown and the valid 
         * DatagramSocket is closed.  If that succeeds, we then 'commit' the 
         * operation, setting our new TCP socket and UDP sockets.
         */
        else {
            
            if(LOG.isDebugEnabled())
                LOG.debug("changing port to " + port);

            DatagramSocket udpServiceSocket = udpService.get().newListeningSocket(port);

            LOG.trace("UDP Service is ready.");
            
            MulticastSocket mcastServiceSocket = null;
            try {
                InetAddress mgroup = InetAddress.getByName(
                    ConnectionSettings.MULTICAST_ADDRESS.getValue()
                );
                mcastServiceSocket =                            
                    multicastService.get().newListeningSocket(
                        ConnectionSettings.MULTICAST_PORT.getValue(), mgroup
                    );
                LOG.trace("multicast service setup");
            } catch(IOException e) {
                LOG.warn("can't create multicast socket", e);
            }
            
        
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket = SocketFactory.newServerSocket(port, new SocketListener());
            } catch (IOException e) {
                LOG.warn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw e;
            } catch (IllegalArgumentException e) {
                LOG.warn("can't create ServerSocket", e);
                udpServiceSocket.close();
                throw new IOException("could not create a listening socket");
            }
            //b) Close old socket
            IOUtils.close(_socket);
            
            //c) Replace with new sock.
            _socket=newSocket;
            _port=port;

            LOG.trace("Acceptor ready..");

            // Commit UDPService's new socket
            udpService.get().setListeningSocket(udpServiceSocket);
            // Commit the MulticastService's new socket
            // if we were able to get it
            if (mcastServiceSocket != null) {
                multicastService.get().setListeningSocket(mcastServiceSocket);
            }

            if(LOG.isDebugEnabled())
                LOG.debug("listening UDP/TCP on " + _port);
        }
    }


	/**
	 * Determines whether or not LimeWire has detected it is firewalled or not.
	 */
	public boolean acceptedIncoming() {
        return _acceptedIncoming;
	}

	/**
	 * For testing.
	 */
	protected void setAcceptedIncoming(boolean incoming) {
        _acceptedIncoming = incoming;
    }
	
	/**
	 * Sets the new incoming status.
	 * Returns whether or not the status changed.
	 */
	private boolean setIncoming(boolean status) {
		if (_acceptedIncoming == status)
			return false;
	    _acceptedIncoming = status;
		activityCallback.get().acceptedIncomingChanged(status);
	    return true;
	}
	
	public void acceptConnection(String word, Socket s) {
        if (ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getValue())
            checkFirewall(s.getInetAddress());
        IOUtils.close(s);
	}
	
	/**
	 * Updates the firewalled status with info from the given incoming address.
	 */
	public void checkFirewall(InetAddress address) {
		// we have accepted an incoming socket -- only record
        // that we've accepted incoming if it's definitely
        // not from our local subnet and we aren't connected to
        // the host already.
        boolean changed = false;
        if(isOutsideConnection(address)) {
            synchronized (Acceptor.class) {
                changed = setIncoming(true);
                ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
                _lastIncomingTime = System.currentTimeMillis();
            }
        }
        if(changed)
            networkManager.incomingStatusChanged();
    }


    /**
     * Listens for new incoming sockets & starts a thread to
     * process them if necessary.
     */
	private class SocketListener implements AcceptObserver {
        
        public void handleIOException(IOException iox) {
            LOG.warn("IOX while accepting", iox);
        }
        
        public void shutdown() {
            LOG.debug("shutdown one SocketListener");
        }
        
        public void handleAccept(Socket client) {
            processSocket(client);
        }
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.SocketProcessor#processSocket(java.net.Socket)
	 */
    public void processSocket(Socket client) {
        processSocket(client, null);
    }
    
    /* (non-Javadoc)
	 * @see com.limegroup.gnutella.SocketProcessor#processSocket(java.net.Socket, java.lang.String)
	 */
    public void processSocket(Socket client, String allowedProtocol) {
        if (!_started) {
            IOUtils.close(client);
            return;
        }

        // If the client was closed before we were able to get the address,
        // then getInetAddress will return null.
        InetAddress address = client.getInetAddress();
        if (address == null || !NetworkUtils.isValidAddress(address) ||
        		!NetworkUtils.isValidPort(client.getPort())) {
            IOUtils.close(client);
            LOG.warn("connection closed while accepting");
        } else if (isBannedIP(address.getAddress())) {
            if (LOG.isWarnEnabled())
                LOG.warn("Ignoring banned host: " + address);
            HTTPStat.BANNED_REQUESTS.incrementStat();
            IOUtils.close(client);
        } else {
            if (LOG.isDebugEnabled())
                LOG.debug("Dispatching new client connecton: " + address);

            // if we want to unset firewalled from any connection,
            // do it here.
            if (!ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getValue())
                checkFirewall(client.getInetAddress());

            // Set our IP address of the local address of this socket.
            InetAddress localAddress = client.getLocalAddress();
            setAddress(localAddress);

            try {
                client.setSoTimeout(Constants.TIMEOUT);
            } catch (SocketException se) {
                IOUtils.close(client);
                return;
            }

            // Dispatch asynchronously if possible.
            if (client instanceof NIOMultiplexor) {// supports non-blocking reads
                ((NIOMultiplexor) client).setReadObserver(new AsyncConnectionDispatcher(connectionDispatcher.get(), client, allowedProtocol) {
                    @Override
                    public void shutdown() {
                        super.shutdown();
                        HTTPStat.CLOSED_REQUESTS.incrementStat();
                    }
                });
            } else {
                HTTPStat.CLOSED_REQUESTS.incrementStat();
                ThreadExecutor.startThread(new BlockingConnectionDispatcher(connectionDispatcher.get(), client, allowedProtocol) {
                    @Override
                    public void shutdown() {
                        super.shutdown();
                        HTTPStat.CLOSED_REQUESTS.incrementStat();
                    }                    
                }, "ConnectionDispatchRunner");
            }
        }
    }
    
    /**
     * Determines whether or not this INetAddress is found an outside source, so as to correctly set "acceptedIncoming"
     * to true.
     * 
     * This ignores connections from private or local addresses, ignores those who may be on the same subnet, and
     * ignores those who we are already connected to.
     */
    private boolean isOutsideConnection(InetAddress addr) {
        // short-circuit for tests.
        if(!ConnectionSettings.LOCAL_IS_PRIVATE.getValue())
            return true;
        
        return !connectionServices.isConnectedTo(addr) &&
               !NetworkUtils.isLocalAddress(addr);
	}

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * 
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(byte[] addr) {        
        return !ipFilter.get().allow(addr);
    }
    
    /**
     * Resets the last connectback time.
     */
    void resetLastConnectBackTime() {
        _lastConnectBackTime = 
             System.currentTimeMillis() - INCOMING_EXPIRE_TIME - 1;
    }

    /**
     * If we used UPnP Mappings this session, clean them up and revert
     * any relevant settings.
     */
    public void shutdown() {
        shutdownUPnP();
    }
    
    private void shutdownUPnP() {
        if(isUPnPEnabled() &&
           upnpManager.get().isNATPresent() &&
           upnpManager.get().mappingsExist() &&
           ConnectionSettings.UPNP_IN_USE.getValue()) {
        	// reset the forced port values - must happen before we save them to disk
        	ConnectionSettings.FORCE_IP_ADDRESS.revertToDefault();
        	ConnectionSettings.FORCED_PORT.revertToDefault();
        	ConnectionSettings.UPNP_IN_USE.revertToDefault();
        }
    }    
    
    /**
     * (Re)validates acceptedIncoming.
     */
    private class IncomingValidator implements Runnable {
        public IncomingValidator() {}
        public void run() {
            // clear and revalidate if 1) we haven't had in incoming 
            // or 2) we've never had incoming and we haven't checked
            final long currTime = System.currentTimeMillis();
            if (
                (_acceptedIncoming && //1)
                 ((currTime - _lastIncomingTime) > INCOMING_EXPIRE_TIME)) 
                || 
                (!_acceptedIncoming && //2)
                 ((currTime - _lastConnectBackTime) > INCOMING_EXPIRE_TIME))
                ) {
                // send a connectback request to a few peers and clear
                // _acceptedIncoming IF some requests were sent.
                if(connectionManager.get().sendTCPConnectBackRequests())  {
                    _lastConnectBackTime = System.currentTimeMillis();
                    Runnable resetter = new Runnable() {
                        public void run() {
                            boolean changed = false;
                            synchronized (Acceptor.class) {
                                if (_lastIncomingTime < currTime) {
                                    changed = setIncoming(false);
                                }
                            }
                            if(changed)
                                networkManager.incomingStatusChanged();
                        }
                    };
                    backgroundExecutor.scheduleWithFixedDelay(resetter, 
                                           WAIT_TIME_AFTER_REQUESTS, 0, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
    
}
