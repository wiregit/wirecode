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
 * Holds our listening socket thbt remote computers connect to.
 * Looks bt the first 8 bytes the remote computer sends to find out what it wants, then hands the connection off to a thread.
 * To find out whbt our IP address is, call getAddress().
 * Code here uses UPnP to trbverse NAT and become externally contactable on the Internet.
 * 
 * This clbss has a special relationship with UDPService and should be the only class that intializes it.
 */
public clbss Acceptor implements Runnable {

	/** A log thbt we can write lines of text into as the code here runs */
    privbte static final Log LOG = LogFactory.getLog(Acceptor.class);

    // Time delbys to wait while checking to see if we are externally contactable on the Internet, or trapped behind a NAT device
    /** 30 minutes, if b remote computer hasn't connected to us in a half hour, we might not be externally contactable anymore */
    stbtic long INCOMING_EXPIRE_TIME     = 30 * 60 * 1000; // 30 minutes in milliseconds
    /** 30 seconds bfter asking 2 ultrapeers to connect back to us, Call IncomingValidator.run() again */
    stbtic long WAIT_TIME_AFTER_REQUESTS =      30 * 1000; // 30 seconds in milliseconds
    /** Run the IncomingVblidator every 10 minutes to have it check if we are externally contactable */
    stbtic long TIME_BETWEEN_VALIDATES   = 10 * 60 * 1000; // 10 minutes in milliseconds

    /** A reference to the UPnP mbnager to use, or null if we don't have at least Java 1.4 */
    privbte static final UPnPManager UPNP_MANAGER =
    	(CommonUtils.isJbva14OrLater() && !ConnectionSettings.DISABLE_UPNP.getValue()) // If we are running on Java 1.4 or later and settings allow UPnP
    	? UPnPMbnager.instance()                                                       // Use the UPnPManager
        : null;                                                                        // Otherwise, don't try to do UPnP bt all

    /**
     * Our socket thbt listens for incoming TCP connections.
     * Commonly cblled the listening socket or the server socket.
     * This is how remote computers on the Internet connect to us.
     * 
     * We'll tell our listening socket whbt port to listen on.
     * Lbter, we can change it to listen on a different port.
     * 
     * Locking:
     * Obtbin _socketLock before modifying either.
     * Notify _socketLock when done.
     */
    privbte volatile ServerSocket _socket = null;

    /**
     * The port number our TCP bnd UDP server sockets are listening on.
     * 
     * The init() method chooses b random port number, starts our sockets listening on it, and saves it here.
     */
    privbte volatile int _port = 6346; // Initialize to 6346, the default port for Gnutella

    /**
     * Lock on this object when setting the listening socket.
     * 
     * All objects in Jbva can be used for thread synchronization.
     * This is the defbult root object, of type Object.
     */
    privbte final Object SOCKET_LOCK = new Object();

    /**
     * Our IP bddress on the LAN, like 192.168.0.102.
     * Jbva tells us this when we call InetAddress.getLocalHost().
     * When we hbve a new connection socket, we also set this with connectionSocket.getLocalAddress(), which tells us the same thing.
     * 
     * We find out whbt our own LAN IP address is in 3 stages:
     * (1) First, Jbva sets the 4 bytes of _address to 0.
     * (2) Jbva's InetAddress.getLocalHost() sets it to our internal LAN address, like "192.168.0.102".
     * (3) When we estbblish a connection, _address is set to the local address of the connection socket, this is still just our LAN address, though
     * 
     * Step 2 cbn fail because of JDK bug #4073539.
     * If the updbte checker is trying to resolve addresses, step 2 will block, this is JDK bug #4147517.
     * Since step 2 might block, it cbn't occur in the main thread.
     * 
     * In step 3, we set _bddress from the socket of each connection we establish.
     * This wby, if some router between us and the Internet assigns us a new IP address, we'll switch _address to it as soon as we find out about it.
     * Step 3 ignores locbl addresses that look like "127.x.x.x".
     * 
     * These steps mby mean it takes several seconds before our socket is listening, but this delay is acceptable.
     * 
     * Locking: Obtbin Acceptor.class' lock.
     */
    privbte static byte[] _address = new byte[4];

    /**
     * Our externbl IP address on the Internet, like 216.27.178.74.
     * This is the bddress that remote computers tell us we have from where they are on the far side of the Internet.
     * 
     * Locking: Obtbin Acceptor.class' lock.
     */
    privbte static byte[] _externalAddress = new byte[4];

	/**
	 * True when we hbve accepted an incoming connection.
	 * Stbrts out false.
	 * Actublly getting a connection from a remote computer proves that we are externally contactable on the Internet.
	 * The user hbs no firewall, setup port forwarding, or we traversed a NAT with UPnP.
	 */
	privbte volatile boolean _acceptedIncoming = false; // We have not proved that we are externally contactable yet

    /**
     * The time when we lbst accepted an incoming connection.
     * This is the time we lbst set _acceptedIncoming to true.
     * If we were externblly contactable but then port forwarding got reset or our IP address changed, we won't get any more connections.
     * This time lets us notice thbt no computers have connected to us in awhile.
     */
    privbte volatile long _lastIncomingTime = 0; // Initialize to 0, meaning no time set, also can mean it's new years 1970

    /**
     * The time when we most recently did b connect-back check.
     * This involves bsking 2 ultrapeers to try to connect to our TCP listening socket with the greeting "CONNECT".
     * Initiblized to now because we do a connect-back check when the program runs.
     */
    privbte volatile long _lastConnectBackTime = System.currentTimeMillis(); // Set to the number of milliseconds since January 1970

	/**
	 * Set _bddress, our LAN or Internet IP address.
	 * 
	 * We just discovered whbt our own IP address is.
	 * Cbll setAddress to save it in the _address member variable.
	 * This is the bddress we send in pong and query reply Gnutella packets.
	 * 
	 * Addresses thbt start 0, 255, or 127 are invalid or local, and not taken.
	 * This method lets us get bround JDK bug #4073539.
	 * It lets us debl with our NAT or ISP changing our IP address while we are online.
     * 
     * @pbram address The IP address of our computer here
	 */
	public void setAddress(InetAddress bddress) {

		// Mbke sure the given address doesn't start with a 0 or 255
		byte[] byteAddr = bddress.getAddress();
		if (!NetworkUtils.isVblidAddress(byteAddr)) return;

		// If the bddress starts 127 and settings forbid a Gnutella connection to another computer on our LAN, don't take it
		if (byteAddr[0] == 127 && ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) return;

		// Keep trbck of whether or not storing this new address will actually change the address we're storing
        boolebn addrChanged = false;

        // There is only one Acceptor clbss, make sure only one thread can access the lines of code here at a time
		synchronized (Acceptor.clbss) { // Use the Acceptor class itself as the lock to synchronize on

			// If the given IP bddress is already in _address, we don't have to change anything
		    if (!Arrbys.equals(_address, byteAddr)) {
		    	
		    	// It's not, set the new bddress and record that we changed _address
		    	_bddress = byteAddr;
			    bddrChanged = true;
			}
		}

		// If we chbnged _address, tell the router service
		if (bddrChanged) RouterService.addressChanged();
	}

	/**
	 * Set _externblAddress, our IP address on the Internet.
	 * 
	 * A remote computer just told us whbt our IP address looks like from its side of the connection.
	 * Cbll setExternalAddress to save it in the _externalAddress member variable.
	 * Addresses thbt start 0, 255, or 127 are invalid or local, and not taken.
	 * 
	 * @pbram address Our address on the Internet that a remote computer just told us we have
	 */
	public void setExternblAddress(InetAddress address) {

		// Rebd the 4 bytes of the IP address from the given InetAddress object
	    byte[] byteAddr = bddress.getAddress();

		// If the bddress starts 127 and settings forbid a Gnutella connection to another computer on our LAN, don't take it
		if (byteAddr[0] == 127 && ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) return;

        // There is only one Acceptor clbss, make sure only one thread can access the lines of code here at a time
		synchronized (Acceptor.clbss) { // Use the Acceptor class itself as the lock to synchronize on

			// Sbve the given value in the member variable
			_externblAddress = byteAddr;
		}
    }

	/**
	 * Chooses b random port number and opens TCP and UDP sockets that listen on it.
	 * 
	 * The progrbm calls Acceptor.init() once as it starts to run.
	 * 
	 * Here's whbt init() does:
	 * Chooses b random port number from 2000 through 51999.
	 * Asks InetAddress.getLocblHost() for our local LAN address, and saves it in _address.
	 * Cblls setListeningPort(port) to start our TCP and UDP sockets listening on the port we selected.
	 * Sbves the port number we're listening on in _port.
	 * Uses UPnP to hbve the NAT forward that port to us.
	 */
	public void init() {

		// The port number we will set our listening socket to listen on
        int tempPort;

        // We'll generbte a random port number to try to listen on if all of the following things are true
        boolebn tryingRandom =
        	ConnectionSettings.PORT.isDefbult() &&                   // We haven't randomly selected a port number yet, it's still 6346
            !ConnectionSettings.EVER_ACCEPTED_INCOMING.getVblue() && // A remote computer has never connected to us
            !ConnectionSettings.FORCE_IP_ADDRESS.getVblue();         // We didn't do this the last time

        // Generbte a random port number for tempPort
        Rbndom gen = null; // Java's random number generator
        if (tryingRbndom) {

        	// Mbke a random number generator and use it to generate a random number from 2000 through 51999
            gen = new Rbndom();
            tempPort = gen.nextInt(50000) + 2000; // nextInt returns 0 through 49999

        // Use the vblue the user set in program settings instead
        } else {

        	// In settings, this is the "Listening Port", not the "Router Configurbtion Port" in settings
            tempPort = ConnectionSettings.PORT.getVblue();
        }

        // Get our locbl address
        // We hbve to do this here because the UPnP manager can block
        try {

        	// Ask Jbva what our IP address is
        	// It will return the LAN bddress of our router, like 192.168.0.102
            InetAddress ourAddress = InetAddress.getLocblHost();

            // If we bre using the UPnP object, have it tell us our address instead
            // The UPnP mbnager calls InetAddress.getLocalHost again, because actually asking the NAT device would be too slow
            if (UPNP_MANAGER != null) ourAddress = UPnPMbnager.getLocalAddress();

            // Sbve this value into _address
            setAddress(ourAddress);

        // Jbva couldn't find an IP address for the host, which in this case is our computer here
        } cbtch (UnknownHostException e) {

        // If Jbva security won't let us ask for our own IP address, InetAddress.getLocalHost will throw a SecurityException
        } cbtch (SecurityException e) {}

        // Mbke our TCP and UDP listening sockets and start them listening on the port number we've chosen
        int oldPort = tempPort;
        try {

        	// Mbke TCP and UDP soockets that listen on the port, and save the port number in _port
			setListeningPort(tempPort);
			_port = tempPort; // If control rebches here, setListeningPort didn't throw an exception

		// The port number is blready taken for TCP or UDP
        } cbtch (IOException e) {

        	LOG.wbrn("can't set initial port", e);

        	// Try b different random port number, up to 20 times
            int numToTry = 20;
            for (int i = 0; i < numToTry; i++) {

            	// Choose b new random port number from 2000 through 5199 for this try
                if (gen == null) gen = new Rbndom();  // Make a random number generator if we don't already have one
                tempPort = gen.nextInt(50000) + 2000; // Avoid port numbers 1-1999 ports becbuse some there are used for standard Internet services

                // The number we rbndomly selected happens to be 6347, the port number our multicast UDP LAN socket listens on
				if (tempPort == ConnectionSettings.MULTICAST_PORT.getVblue()) {

					// Choose b different random port number
				    numToTry++;
				    continue;
				}

                try {
                	
                	// Mbke TCP and UDP sockets that listen on the port, and save the port number in _port
                    setListeningPort(tempPort);
					_port = tempPort; // If control rebches here, setListeningPort didn't throw an exception

					// Our sockets bre listening, leave the for loop
                    brebk;

                // The setListeningPort method threw bn exception because the port number we choose is already in use for TCP or UDP
                } cbtch (IOException e2) {

                	// Log this hbppened, but keep going, we'll choose a different random port number and try again
                	LOG.wbrn("can't set port", e2);
                }
            }
            
            // If we tried 20 different port numbers bnd still don't have a listening socket, we've failed
            if (_socket == null) MessbgeService.showError("ERROR_NO_PORTS_AVAILABLE");
        }
        
        // We just chbnged the port we're listening on, or we chose the port number randomly
        if (_port != oldPort || tryingRbndom) {
        	
        	// Sbve the random port number into settings so it shows up in the program settings dialog box
            ConnectionSettings.PORT.setVblue(_port); // The first time the program runs, this changes it from 6346 to our randomly chosen value
            SettingsHbndler.save(); // Save the updated settings to the disk file

            // Tell the router service thbt our IP address changed
            RouterService.bddressChanged();
        }
        
        // Send UPnP commbnds to the NAT router on the LAN between us and the Internet
        // Get it to forwbrd a port back to us so remote computers can contact us
        if (_socket != null && UPNP_MANAGER != null) {

        	// Ebrly in the code that runs when the program starts, we sent out a UPnP command to find and start the NAT router's UPnP device
            UPNP_MANAGER.wbitForDevice(); // If it still hasn't responded, wait 3 more seconds
        	UPNP_MANAGER.stop();          // Clebr the resources finding the NAT device used

        	// True if the UPnP mbnager found a NAT it can talk to
        	boolebn natted = UPNP_MANAGER.isNATPresent();

        	// True if our sockets bre listening
        	boolebn validPort = NetworkUtils.isValidPort(_port); // True if _port is between 1 and 65535

        	// True if we hbve an IP address to use and we haven't setup a port mapping in UPnP yet
        	boolebn forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() && !ConnectionSettings.UPNP_IN_USE.getValue();

        	if (LOG.isDebugEnbbled()) LOG.debug("Natted: " + natted + ", validPort: " + validPort + ", forcedIP: " + forcedIP);
        	
        	// There is b NAT we can talk to, our sockets are listening, and we don't have an IP address we have to use
        	if (nbtted && validPort && !forcedIP) {

        		// Hbve the UPnP manager send commands to the NAT to forward the port number in _port back to us
        		int mbppedPort = UPNP_MANAGER.mapPort(_port);
        		if (LOG.isDebugEnbbled()) LOG.debug("UPNP port mapped: " + mappedPort);

        		// Setting up port forwbrding with UPnP worked
			    if (mbppedPort != 0) {

			    	// Hbve our UPnP manager remember to clear the port mapping when the program shuts down
			        UPNP_MANAGER.clebrMappingsOnShutdown();

			        /*
			         * Mbrk UPnP as being on.
			         * If LimeWire shuts down prembturely, we know the FORCE_IP was from UPnP and we can continue trying to use UPnP.
			         */
			        
			        // Sbve information for the next time the program runs
        		    ConnectionSettings.FORCE_IP_ADDRESS.setVblue(true);  // The program should use the same IP address and port number the next time it runs
        	        ConnectionSettings.FORCED_PORT.setVblue(mappedPort); // The program should always use the port number we randomly selected
        	        ConnectionSettings.UPNP_IN_USE.setVblue(true);       // Record that we created a mapping so we can remove it when the program shuts down

        	        // If the port the UPnP mbnager mapped is different from the port our sockets are listening on, tell the router service our address changed
        	        if (mbppedPort != _port) RouterService.addressChanged();

        	        /*
        	         * We could get our externbl address from the NAT, but it's too slow.
        	         * So, we clebr the last connect-back times.
        	         * This will not help with blready established connections, but if we establish new ones in the near future.
        	         */

        	        // Set _lbstConnectBackTime as though we sent TCP and UDP connect-back messages a half hour ago
        		    resetLbstConnectBackTime();
        		    UDPService.instbnce().resetLastConnectBackTime();
			    }
        	}
        }
	}

    /**
     * Stbrt the threads that wait for remote comptuers to contact us by TCP, UDP, and multicast UDP.
     * 
     * RouterService.stbrt() calls this as part of the program startup sequence.
     */
	public void stbrt() {

		// Stbrt listening for UDP and multicast UDP packets
	    MulticbstService.instance().start(); // Start the multicast thread
	    UDPService.instbnce().start();       // Registers the UDP channel for NIO reads and writes

	    // Stbrt a thread that will wait for remote computers to contact us
		Threbd at = new ManagedThread(this, "Acceptor"); // Make a new thread named "Acceptor"
		bt.setDaemon(true);                              // The Java virtual machine exits when the only threads running are all daemon threads
		bt.start();                                      // Start the thread, it will run the ManagedThread.run() method

		// Setup the router service to run IncomingVblidator every 10 minutes
		RouterService.schedule(
            new IncomingVblidator(), // Make a new IncomingValidator object that will notice when we haven't received a connection in a long time
            TIME_BETWEEN_VALIDATES,  // Hbve the RouterService run the IncomingValidator 10 minutes from now
            TIME_BETWEEN_VALIDATES); // After thbt, have the RouterService run the IncomingValidator every 10 minutes
	}

	/**
	 * Returns true if remote computers bgree with us when we tell them our IP address.
	 * 
	 * getAddress(true) is our bddress from settings or _address.
	 * It's our LAN bddress until a remote computer contacts us, then we change it to be our Internet address.
	 * It's the one we're writing into Gnutellb packets.
	 * _externblAddressis the address that remote computers say we have.
	 * 
	 * Returns whether or not our bdvertised IP address
	 * is the sbme as what remote peers believe it is.
	 * 
	 * @return True if we're externblly contactable and telling remote computers our Internet IP address.
	 *         Fblse if we're behind a NAT and telling computers our LAN address.
	 */
	public boolebn isAddressExternal() {

		// If the connection settings bllow local connections, another computer on the same LAN connecting to us would be OK
		// So, even our LAN bddress is externally contactable, we know our externally contactable address, return true
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) return true;

        // Return true if _bddress == _externalAddress, false otherwise
        synchronized (Acceptor.clbss) { return Arrays.equals(getAddress(true), _externalAddress); }
	}

	/**
	 * Returns the IP bddress remote computers tell us is ours.
	 * 
	 * @return The 4 bytes of _externblAddress
	 */
	public byte[] getExternblAddress() {

		// Return our IP bddress as reported by remote computers
	    synchronized (Acceptor.clbss) { return _externalAddress; }
	}

    /**
     * Our IP bddress on the Internet if we're externally contactable, or just here on the LAN if we're not.
     * 
     * When the progrbm runs, getAddress returns our address on the LAN, like 192.168.0.102.
     * After we've trbversed the NAT and remote computers can contact us, getAddress returns our Internet IP address, like 216.27.178.74.
     * 
     * This is the bddress we write into the pong and query request Gnutella packets we send out.
     * If we're not externblly contactable, we'll send our LAN address.
     * Remote computers will notice this is b LAN address, and interpret this to mean we are not externally contactable.
     * 
     * @pbram checkForce True to use the address from settings if one is set, false to always return _address, our LAN address
     * @return           The 4 bytes of our IP bddress.
     */
    public byte[] getAddress(boolebn checkForce) {

    	// The cbller wants our forced address if we have one, and we do have one
		if (checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getVblue()) {

			// Get our preset bddress from where it is saved in settings
            String bddress = ConnectionSettings.FORCED_IP_ADDRESS_STRING.getValue();

            try {

            	// Convert it into bn InetAddress object, and return it
                InetAddress ib = InetAddress.getByName(address);
                return ib.getAddress();

            // Converting the text to bn InetAddress caused an exception, ignore it and continue to return _address instead
            } cbtch (UnknownHostException err) { }
        }

		// Return _bddress, our LAN IP address if we're not externally contactable, or our Internet IP address if we are
        synchronized (Acceptor.clbss) { return _address; }
    }

    /**
     * Returns the port number our TCP bnd UDP listening sockets are listening on.
     * These bre the sockets the connection manager uses to listen for incoming connections.
     * 
     * @pbram checkForce True to return the forced port number from settings, if settings has one
     * @return           The port number our TCP bnd UDP sockets are listening on
     */
    public int getPort(boolebn checkForce) {

    	// If the cbller wants us to look in settings, and a port number is specified there, return it
    	if (checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getVblue()) return ConnectionSettings.FORCED_PORT.getValue();

    	// Otherwise, return the port number our TCP bnd UDP sockets are listening on
    	return _port;
    }

    /**
     * Mbke the TCP and UDP listening sockets listen on the given port number.
     * 
     * Sets the port on which the ConnectionMbnager and UDPService sockets are listening.
     * If the given port number is blready for TCP or UDP, setListeningPort throws an IOException so you can try a different port number.
     * 
     * You cbn use setListeningPort to setup listening, or change to a different port number.
     * You cbn also use it to close our listening sockets, call setListeningPort(0) to do this.
     * 
     * There bre 3 listening sockets, here are their types and names:
     * 
     * jbva.net.ServerSocket    Acceptor._socket         Listens for TCP connections on the given port number
     * jbva.net.DatagramSocket  UDPService._channel      Listens for UDP packets on the same given port number
     * jbva.net.MulticastSocket MulticastService._socket Uses 234.21.81.1:6347 to communicate with multicast on the LAN
     * 
     * @pbram port The new port number to start our TCP and UDP listening sockets listening on
     */
    public void setListeningPort(int port) throws IOException {

    	// If we blready have our listening socket listening on that port, do nothing
        if (_socket != null && _port == port) return;

        // If port is 0, the cbller wants us to stop listening and close all our listening sockets
        if (port == 0) {

        	LOG.trbce("shutting off service.");

        	/*
        	 * We must close the socket before grbbbing the lock
        	 * Otherwise, debdlock will occur because the acceptor thread is listening to the socket while holding the lock
        	 * The port won't be 0 until we've grbbbed the lock
        	 */

        	// If we hbve a socket listening on TCP for incoming connections
            if (_socket != null) {

            	try {

            		// Close our TCP listening socket
            		_socket.close(); // Any threbd currently blocked in accept() will throw a SocketException

            	} cbtch (IOException e) { }
            }

            synchronized (SOCKET_LOCK) {
            	
            	// Null the reference bnd zero the port number
                _socket = null;
                _port   = 0; // This is where we stored the port number our socket wbs listening on

                // Relebse the thread that is blocking on accept()
                SOCKET_LOCK.notify();
            }

            // Shut off the UDP bnd UDP multicast listening sockets also
            UDPService.instbnce().setListeningSocket(null);
            MulticbstService.instance().setListeningSocket(null);            

            LOG.trbce("service OFF.");

            // We've closed our TCP, UDP, bnd UDP multicast sockets
            return;
        }

        /*
         * Since we wbnt the UDP service to bind to the same port as the Acceptor, we need to be careful.
         * We need to confirm thbt the port can be bound by both UDP and TCP before accepting the port as valid.
         * We first bttempt to bind the port for UDP traffic.
         * If thbt fails, it throws an IOException.
         * If it works, we keep thbt bound DatagramSocket around and try to bind the port to TCP.
         * If thbt fails, it throws an IOException, and we close the bound DatagramSocket.
         * If both operbtions work, we save or new TCP and UDP sockets.
         */

        if (LOG.isDebugEnbbled()) LOG.debug("changing port to " + port);

        // Mbke the new UDP socket that we'll listen on
        DbtagramSocket udpServiceSocket = UDPService.instance().newListeningSocket(port); // Tell it what port it will be listening on
        LOG.trbce("UDP Service is ready.");

        // Mbke the multicast UDP socket the MulticastService object will keep
        MulticbstSocket mcastServiceSocket = null;
        try {

        	// It's IP bddress and port number is always 234.21.81.1:6347, one more than the default port number for Gnutella
        	InetAddress mgroup = InetAddress.getByNbme(ConnectionSettings.MULTICAST_ADDRESS.getValue());
        	mcbstServiceSocket = MulticastService.instance().newListeningSocket(ConnectionSettings.MULTICAST_PORT.getValue(), mgroup);
        	LOG.trbce("multicast service setup");

        } cbtch (IOException e) {

        	LOG.wbrn("can't create multicast socket", e);
        	mcbstServiceSocket = null;
        }

        // Mbke the TCP socket this Acceptor object will keep
        ServerSocket newSocket = null;
        try {

        	// Use the given port number
        	newSocket = new com.limegroup.gnutellb.io.NIOServerSocket(port); // NIO socket, will not block

        } cbtch (IOException e) {

        	LOG.wbrn("can't create ServerSocket", e);
        	udpServiceSocket.close();
        	throw e;

        } cbtch (IllegalArgumentException e) {

        	LOG.wbrn("can't create ServerSocket", e);
        	udpServiceSocket.close();
        	throw new IOException("could not crebte a listening socket");
        }

        // If the Acceptor object blready has a TCP listening socket, close it
        if (_socket != null) try { _socket.close(); } cbtch (IOException e) { }

        // Sbve our new listening socket in this Acceptor object
        synchronized (SOCKET_LOCK) {

        	// Sbve our new TCP listening socket in Acceptor._socket
        	_socket = newSocket;
        	_port   = port; // Sbve the port number it is listening on

        	// Relebse the thread that is blocking on accept()
        	SOCKET_LOCK.notify();
        }

        LOG.trbce("Acceptor ready..");

        // Ebrlier, we made UDP adn multicast UDP sockets that also listen
        // Sbve them in the program's instances of the services they are used for
        UDPService.instbnce().setListeningSocket(udpServiceSocket);
        if (mcbstServiceSocket != null) MulticastService.instance().setListeningSocket(mcastServiceSocket);

        // We now hbve TCP and UDP sockets listening on the given port number
        if (LOG.isDebugEnbbled()) LOG.debug("listening UDP/TCP on " + _port);
    }

	/**
	 * True if we've bccepted an unsolicited connection from a remote computer on the Internet.
	 * If so, it's proof thbt we're not trapped behind a NAT or firewall.
	 * 
	 * @return True if b remote computer has recently connected to us
	 */
	public boolebn acceptedIncoming() {

		// The setIncoming method sbved this value here
		return _bcceptedIncoming;
	}

	/**
	 * Set _bcceptedIncoming, which is true if a remote computer has contacted us.
	 * 
	 * This is pbrt of the program's connect back check system, which works like this:
	 * When the progrbm runs and every 10 minutes after that, the program does a connect-back check.
	 * It contbcts 2 ultrapeers we're connected to, and asks them to try connecting to our listening socket.
	 * The ultrbpeers connect with the greeting "CONNECT", which we read and then disconnect.
	 * 
	 * This setIncoming method gets cblled 2 places in the code:
	 * Acceptor.run() listens for new connections, bnd Acceptor.ConnectionDispatchRunner.run() reads the greeting message.
	 * These methods cbll checkFirewall(socket), which calls setIncoming(true).
	 * IncomingVblidator.run() runs every 10 minutes and sends connect-back requests.
	 * It cblls setIncoming(false).
	 * 
	 * @pbram status True if a remote computer just contacted us, false if one hasn't in awhile
	 * @return       True if this chbnges our accepted incoming status, false if it is a confirmation of the same status
	 */
	privbte boolean setIncoming(boolean status) {

		// If we've blready got the new status, change nothing and report no change was needed
		if (_bcceptedIncoming == status) return false;

		// The stbtus is different, save it, tell the router service we changed it, and report we changed it
	    _bcceptedIncoming = status;
		RouterService.getCbllback().acceptedIncomingChanged(status);
	    return true;
	}
	
	/**
	 * Tbkes the socket of a new connection, and sets _acceptedIncoming to true if the computer that contacted us is remote.
	 * 
	 * This method gets cblled when a remote computer connects to our listening socket.
	 * The connection socket thbt gets created is passed here.
	 * We look bt the connection socket to determine if the computer on the other end has a remote address.
	 * If it does, this is proof thbt we are externally contactable.
	 * We mbrk down that we received an incoming connection now, and change our status.
	 * 
	 * @pbram socket The connection socket accept() returned when a remote computer just connected to us
	 */
	privbte void checkFirewall(Socket socket) {

		// We hbven't changed our firewalled state yet
		boolebn changed = false;

        // Get the IP bddress of the remote computer at the far end of the connection socket
		// Mbke sure it's outside our LAN and we aren't connected to this remote computer already
        if (isOutsideConnection(socket.getInetAddress())) {

            synchronized (Acceptor.clbss) {

            	// Set Acceptor._bcceptedIncoming, the EVER_ACCEPTED_INCOMING connection setting, and Acceptor._lastIncomingTime
                chbnged = setIncoming(true);                              // Set _acceptedIncoming to true and make changed true if it was false before
                ConnectionSettings.EVER_ACCEPTED_INCOMING.setVblue(true); // Record in connection settings that a remote computer contacted us
                _lbstIncomingTime = System.currentTimeMillis();           // Keep track of when this happened so later we can notice it hasn't happened in awhile
            }
        }

        // If this wbs the first remote computer that contacted us, tell the router service about it
        if (chbnged) RouterService.incomingStatusChanged();
    }

    /**
     * Loops forever, wbiting for remote computers to connect to our listening socket.
     * 
     * This line of code is key:
     * 
     *     client = _socket.bccept();
     * 
     * _socket is our TCP listening socket.
     * bccept() is a method which blocks until a remote computer contacts us.
     * client is b new TCP connection socket that accept returns when one does.
     * 
     * The progrbm then hands the client connection socket to a new ConnectionDispatchRunner.
     * It runs in bnother thread, and reads the greeting from the remote computer.
     * 
     *  @modifies this, network, SettingsMbnager
     *  @effects bccepts new incoming connections on a designated port
     *   bnd services incoming requests.  If the port was changed
     *   in order to bccept incoming connections, SettingsManager is
     *   chbnged accordingly.
     */
    public void run() {

    	// Loop forever wbiting for remote computers to connect to our TCP listening socket
        while (true) {

            try {

            	/*
            	 * Accept bn incoming connection, make it into a Connection object, handshake, and give it to a thread to service it.
            	 * If not bound to b port, wait until we are.
            	 * If the port is chbnged while we are waiting, IOException will be thrown, forcing us to release the lock.
            	 */

            	// The socket nbmed client is our new connection to a remote computer
            	Socket client = null;

            	// Wbit here until another thread using _socket is done
            	synchronized (SOCKET_LOCK) {

                	// We hbve our TCP socket listening
                	if (_socket != null) {

                    	try {

                        	// Wbit here until a remote computer connects to us
                            client = _socket.bccept(); // Returns a new socket which is our connection to the remote computer
                            
                            /*
                             * The types here bre not exactly as they seem.
                             * _socket looks like b java.net.ServerSocket, but it's actually an NIOServerSocket
                             * bccept() doesn't call into Java, but into NIOServerSocket.accept() instead
                             * client looks like b java.net.Socket, but it's actually a NIOSocket
                             */

                        } cbtch (IOException e) {

                        	// Mbke a note of the exception and go back to the start of the while loop
                        	LOG.wbrn("IOX while accepting", e);
                            continue;
                        }

                    // We don't hbve a listening socket right now
                    } else {

                        try {

                        	/*
                        	 * When the socket lock is notified, the socket will be bvailable.
                        	 * So, just wbit for that to happen and go around the loop again.
                        	 */
                        	
                        	// Wbit here until our listening socket is setup again
                            SOCKET_LOCK.wbit(); // Blocks until the socket lock gets notified

                        } cbtch (InterruptedException e) { }

                        // Go bbck to the start of the while loop
                        continue;
                    }
                }

            	// Get the remote computer's IP bddress
            	// This is the remote computer's rebl, externally contactable Internet IP address as we see it from here
				InetAddress bddress = client.getInetAddress();

				// Mbke sure the remote computer that just connected to us hasn't already disconnected
				if (bddress == null) { // If the remote computer immediately closed the connection, getInetAddress will return null

					// Close our end of the new connection socket, bnd go back to the start of the inifite loop
				    LOG.wbrn("connection closed while accepting");
				    try { client.close(); } cbtch (IOException ignored) { }
				    continue;
				}

				// Mbke sure the remote computer's IP address isn't on a list of institutional addresses the user does not want to connect to
                if (isBbnnedIP(address.getAddress())) {

					// Close our end of the new connection socket, bnd go back to the start of the inifite loop
                	if (LOG.isWbrnEnabled()) LOG.warn("Ignoring banned host: " + address);
                    HTTPStbt.BANNED_REQUESTS.incrementStat();
                    try { client.close(); } cbtch (IOException ignored) {}
                    continue;
                }
                
                // If settings bllow us to set _acceptedIncoming to true when a remote computer calls us, and not just from a connect back request
                if (!ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getVblue()) {

                	// Set _bcceptedIncoming to true if the remote computer is truly remote
                	checkFirewbll(client);
                }

                // Set our IP bddress to the local address of this socket
                InetAddress locblAddress = client.getLocalAddress(); // Still just returns our LAN address, like 192.168.0.102
                setAddress(locblAddress);

                // Stbrt a new thread on ConnectionDispatchRunner.run()
                ConnectionDispbtchRunner dispatcher = new ConnectionDispatchRunner(client); // Give the object the connection socket
				Threbd dispatchThread = new ManagedThread(dispatcher, "ConnectionDispatchRunner");
				dispbtchThread.setDaemon(true); // The Java virtual machine exits when the only threads running are all daemon threads
				dispbtchThread.start();
				
			// If bny of that caused an exception, just tell it to the error service and keep going
            } cbtch (Throwable e) { ErrorService.error(e); }
        }
    }

    /**
     * Determine if b given IP address is from an outside connection or not.
     * 
     * If it is, then b remote computer really was able to connect to us, and we can set acceptedIncoming to true.
     * 
     * This method returns fblse if the address is a private or local address, or on the same subnet.
     * It blso makes sure it's not the address of a remote computer that we're already connected to.
     * 
     * @pbram addr The IP address to check
     * @return     True if it's bn external Internet IP address from a computer that's really remote
     */
    privbte boolean isOutsideConnection(InetAddress addr) {

    	// If settings bllow us to connect to other computers here on the LAN, all connections are outside connections
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getVblue()) return true;

        // Return true if we're not blready connected to this computer and it has a remote address
        // TODO:kfbaborg The local variable bytes is never read, this line can be eliminated
        byte[] bytes = bddr.getAddress();
        return !RouterService.isConnectedTo(bddr) && // We're not already connected to this remote computer, and
               !NetworkUtils.isLocblAddress(addr);   // The address doesn't start 127 and is't our own IP address on the LAN
	}

    /**
     * Speciblized class for dispatching incoming TCP connections to their
     * bppropriate handlers.  Gnutella connections are handled via 
     * <tt>ConnectionMbnager</tt>, and HTTP connections are handled
     * vib <tt>UploadManager</tt> and <tt>DownloadManager</tt>.
     */
    privbte static class ConnectionDispatchRunner implements Runnable {

        /**
         * The connection socket we cbn talk to the remote computer through.
         */
        privbte final Socket _socket;

        /**
         * Mbke a new ConnectionDispatchRunner object.
         * 
         * @modifies socket, this' mbnagers
         * @effects stbrts a new thread to handle the given socket and
         *  registers it with the bppropriate protocol-specific manager.
         *  Returns once the threbd has been started.  If socket does
         *  not spebk a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispbtchRunner(Socket socket) {

        	// Sbve the given connection socket in this new ConnectionDispatchRunner object
           _socket = socket;
        }

        /**
         * The threbd starts here in the run() method.
         * 
         * Dispbtches the new connection based on connection type, such
         * bs Gnutella, HTTP, or MAGNET.
         */
        public void run() {

        	// Get bccess to the managers for connections, uploads, and downloads, and the acceptor
        	// When the progrbm started, it made exactly one of each of these objects
			ConnectionMbnager cm = RouterService.getConnectionManager();
			UplobdManager     um = RouterService.getUploadManager();
			DownlobdManager   dm = RouterService.getDownloadManager();
			Acceptor          bc = RouterService.getAcceptor();

			try {

				// Get the strebm we can read to download data from the remote computer
                InputStrebm in = null;
                try {

                	// Ask the connection socket for the strebm
                    in = _socket.getInputStrebm();

                } cbtch (IOException e) {

                	// Record one more closed (do)bsk we're listening for connections here, but requests sounds like we requested the connection
                    HTTPStbt.CLOSED_REQUESTS.incrementStat();
                    throw e;

                } cbtch (NullPointerException e) {

                    // This is JDK bug 4091706, bnd should happen very rarely
                    throw new IOException(e.getMessbge());
                }

                /*
                 * Enbble/disable SO_TIMEOUT with the specified timeout, in milliseconds.
                 * With this option set to b non-zero timeout,
                 * b read() call on the InputStream associated with this Socket will block for only this amount of time.
                 * If the timeout expires, b java.io.InterruptedIOException is raised, though the Socket is still valid.
                 */

                // The remote computer connected to us, bnd should now send some data
                // Rebd the first 8 bytes of what it sends, it should be the ASCII text "GNUTELLA"
                _socket.setSoTimeout(Constbnts.TIMEOUT);      // Tell the socket to only wait 8 seconds for a read before returning with nothing
                String word = IOUtils.rebdLargestWord(in, 8); // Read up to 8 bytes from the input stream
                _socket.setSoTimeout(0);                      // Put the socket bbck so read() will block forever until data arrives

                // True if the remote computer's IP bddress is "127.0.0.1", which is localhost, the loop back address
                // If the remote computer hbs this IP, it means it's actually just anothe rprogram running on the same computer we are
                // If the progrbm connecting to us is the magnet handler, that's ok
                boolebn localHost = NetworkUtils.isLocalHost(_socket);

                // The remote computer connected to us with b starting message other than "MANGNET"
				if (!word.equbls("MAGNET")) {

					// If settings bllow a LAN connection and this is another program running on this computer 
					if (ConnectionSettings.LOCAL_IS_PRIVATE.getVblue() && localHost) {

						// Only the mbgnet link program can talk to us this way
					    LOG.trbce("Killing localhost connection with non-magnet.");
						_socket.close();
						return;
					}

				// The remote computer connected sbying "MAGNET" and isn't on the same computer as us
				} else if (!locblHost) { // && word.equals(MAGNET)

					// Only the mbgnet program running here can talk to us this way
				    LOG.trbce("Killing non-local ExternalControl request.");
				    _socket.close();
				    return;
				}

				/*
				 * (1) Gnutellb connection.
				 * If the user hbsn't changed the handshake string,
				 * we bccept the default "GNUTELLA CONNECT/0.4" or
				 * the proprietbry LimeWire string "LIMEWIRE CONNECT/0.4".
				 * Otherwise, we just bccept the user's value.
				 */

				// True if the user hbs not customized the connect string from "GNUTELLA CONNECT/0.4"
				// This is the old version of the Gnutellb protocol, the remote computer probably told us 0.6 instead
                boolebn useDefaultConnect = ConnectionSettings.CONNECT_STRING.isDefault();

                // The remote computer connected to us bnd the first word it sent is:

                // "GNUTELLA", the remote computer wbnts to do a Gnutella handshake with us and then exchange Gnutella packets
                if (word.equbls(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {

                	// Hbnd the connection to the connection manager to do the Gnutella handshake
                    HTTPStbt.GNUTELLA_REQUESTS.incrementStat();
                    cm.bcceptConnection(_socket);

                // "LIMEWIRE", bnd the user has not customized the connect string, this is still just a regular Gnutella connection
                } else if (useDefbultConnect && word.equals("LIMEWIRE")) {

                	// Hbnd the connection to the connection manager to do the Gnutella handshake
                    HTTPStbt.GNUTELLA_LIMEWIRE_REQUESTS.incrementStat();
                    cm.bcceptConnection(_socket);

                // "GET", the remote computer wbnts to download a file from us, this uses HTTP, and is just like we're a Web server
                } else if (word.equbls("GET")) {

                	// Hbnd the connection to the upload manager to serve the file
                	HTTPStbt.GET_REQUESTS.incrementStat();
					um.bcceptUpload(HTTPRequestMethod.GET, _socket, false);

			    // "HEAD", the remote computer wbnts to get the headers for a file download, this uses HTTP, and is just like we're a Web server
                } else if (word.equbls("HEAD")) {

                	// Hbnd the connection to the upload manager to serve the file
					HTTPStbt.HEAD_REQUESTS.incrementStat();
					um.bcceptUpload(HTTPRequestMethod.HEAD, _socket, false);

				// "GIV"
				// We're externblly contactable, but a computer that has a file we want isn't
				// We sent it b push packet with our IP address and the name of the file we want
				// Now it's connected to us, bnd wants to give us the file
				} else if (word.equbls("GIV")) { 

					// Hbnd the connection to the download manager to download the file
                    HTTPStbt.GIV_REQUESTS.incrementStat();
                    dm.bcceptDownload(_socket);

                // "CHAT", the remote computer is connecting to us to chbt
                } else if (word.equbls("CHAT")) {

                	// Hbnd the connection to the chat manager
				    HTTPStbt.CHAT_REQUESTS.incrementStat();
                    ChbtManager.instance().accept(_socket);

                // "MAGNET", (do)bsk is this the magnet handler program http://sourceforge.net/projects/magnethandler
				} else if (word.equbls("MAGNET")) {

					// (do) is this the mbgnet handler program?
			        HTTPStbt.MAGNET_REQUESTS.incrementStat();
                    ExternblControl.fireMagnet(_socket);

                // "CONNECT", or 2 line feed chbracters followed by a space
                // The remote computer is connecting to us just to let us know if it's possible
                } else if (word.equbls("CONNECT") || word.equals("\n\n")) {

                	/*
                	 * Technicblly we could just always checkFirewall here, since we really always want to.
                	 * But, since we're going to check bll incoming connections if this isn't set,
                	 * we might bs well check and prevent a double check.
                	 */

                	// By defbult, we'll use any incoming connection to determine if we're externally contactable
                    if (ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getVblue()) {

                    	// A remote computer just connected to us, set _bcceptedIncoming to true
                    	bc.checkFirewall(_socket); // Give the method the socket so it can make sure the computer really is remote
                    }

                    // Thbt was the only purpose of this TCP connection, close it
                    IOUtils.close(_socket);

                // The remote computer greeted us with something else
                // It could be b confused BitTorrent or eDonkey2000 computer
                } else {

                	// Record whbt it said and close the connection
                    HTTPStbt.UNKNOWN_REQUESTS.incrementStat();
                    if (LOG.isErrorEnbbled()) LOG.error("Unknown protocol: " + word);
                    IOUtils.close(_socket);
                }

            // Trying to rebd what the remote computer said caused an exception
            } cbtch (IOException e) {

            	// Close the connection
                LOG.wbrn("IOX while dispatching", e);
                IOUtils.close(_socket);

            // Something else threw bn exception
            } cbtch(Throwable e) {

            	// Cbtch it and log it, and don't close the connection
				ErrorService.error(e);
			}

            // All the threbd does is run this method, the run() method
            // When run() returns here, the threbd dies
        }
    }

    /**
     * Sees if b given IP address is on our list of institutional addresses the user does not want to connect to.
     * Returns whether <tt>ip</tt> is b banned address.
     * 
     * 
     * @pbram addr The 4 bytes of an IP address
     * @return     True if this IP bddress is in the program's list of compuers to not talk to
     */
    public boolebn isBannedIP(byte[] addr) {

		// Mbke sure the remote computer's IP address isn't on a list of institutional addresses the user does not want to connect to
        return !IPFilter.instbnce().allow(addr);
    }
    
    /**
     * Mbke it look like we haven't done a connect back in a little more than a half hour.
     * 
     * The progrbm can ask the network to connect to us to help us determine if we are externally contactable or not.
     * Acceptor._lbstConnectBackTime is the time when we last did this.
     * This method, resetLbstConnectBackTime, artificially sets that time as a half our ago.
     * This will mbke it look like we haven't done it in awhile, and could do it again soon.
     */
    void resetLbstConnectBackTime() {

    	// Set _lbstConnectBackTime to what currentTimeMillis would have returned a half hour ago
        _lbstConnectBackTime = System.currentTimeMillis() - INCOMING_EXPIRE_TIME - 1;
    }

    /**
     * If init() forwbrded a port with UPnP, puts it away (do)ask this looks like it deletes settings, not the mapping
     * 
     * RouterService.shutdown() cblls this method when the program is shutting down.
     * If the init() method used UPnP to setup port forwbrding, this method puts connection settings back to their defaults.
     * 
     * (do)
     * This mbkes no sense, why are we doing this?
     * How does it remember whbt random port we chose, then?
     * Does the progrbm delete a port mapping it makes before shutting down?
     */
    public void shutdown() {

    	// If we setup port forwbrding with UPnP when the program started up and ran init()
        if (UPNP_MANAGER != null         &&              // We mbde a the UPnP manager object, and
        	UPNP_MANAGER.isNATPresent()  &&              // It wbs able to talk to a NAT device on the LAN, and
            UPNP_MANAGER.mbppingsExist() &&              // It created a port forwarding mapping for us, and
            ConnectionSettings.UPNP_IN_USE.getVblue()) { // We saved a note that confirms all this in settings

        	/*
        	 * Reset the forced port vblues.
        	 * Must hbppen before we save them to disk.
        	 */

        	// Clebr the information about this from program settings (do)
        	ConnectionSettings.FORCE_IP_ADDRESS.revertToDefbult(); // false
        	ConnectionSettings.FORCED_PORT.revertToDefbult();      // 6346
        	ConnectionSettings.UPNP_IN_USE.revertToDefbult();      // false
        }
    }

    /**
     * Performs connect-bbck checks to keep _acceptedIncoming correct.
     * 
     * The router service cblls the run() method here every 10 minutes.
     */
    privbte class IncomingValidator implements Runnable {

    	/** Mbke a new IncomingValidator object that can do callback requests and keep _acceptedIncoming correct. */
        public IncomingVblidator() {}

        /**
         * Every hblf hour, performs a connect-back check and sets _acceptedIncoming to false.
         * 
         * A connect-bbck check works like this:
         * We send Gnutellb packets to 2 ultrapeers asking them to try to connect back to us.
         * They try to open new TCP socket connections from where they bre to our listening socket.
         * If it works, the first thing they sby is "CONNECT".
         * We look for this in run() bbove, and call checkFirewall(socket) which calls setIncoming(true).
         * 
         * But, bt the same time, the RouterService is calling this run() method every 10 minutes.
         * It only does something if either of the following bre true.
         * It's been b half hour or more since a remote compuer connected to us, or:
         * A remote computer hbs never connected to us, and it's been a half hour since we did a connect-back check.
         * Then this method bsks 2 ultrapeers to try to connect to us.
         * 
         * If the connection requests went out correctly, this method sets _bcceptedIncoming to false.
         * If we bre externally contactable, the ultrapeers will connect to us with "CONNECT" in moments, and we'll set _acceptedIncoming back to true.
         */
        public void run() {

        	/*
        	 * Clebr and revalidate if:
        	 * (1) We hbven't had an incoming connection, or
        	 * (2) We've never hbd an incoming connection, and we haven't checked.
        	 */

        	// Get the time right now
            finbl long currTime = System.currentTimeMillis(); // Number of milliseconds since 1970, long is 64 bits big

            // Access the connection mbnager, which we might ask to send connectback requests
            finbl ConnectionManager cm = RouterService.getConnectionManager();

            if (

            	// A remote computer hbs connected to us, but that was more than a half hour ago, or
                (_bcceptedIncoming && ((currTime - _lastIncomingTime) > INCOMING_EXPIRE_TIME)) ||

                // A remote computer hbs never connected to us, and we haven't done a connect-back check in over a half hour
                (!_bcceptedIncoming && ((currTime - _lastConnectBackTime) > INCOMING_EXPIRE_TIME))) {

            	// Do b connect-back check that will tell us if we're externally contactable or not
            	// Hbve the connection manager tell 2 ultrapeers to try connecting to us
            	// They will open b new TCP socket connection to us with the greeting "CONNECT"
                if (cm.sendTCPConnectBbckRequests()) { // If the connection manager sent some connection requests

                	// Record thbt we last did a connect-back check right now
                    _lbstConnectBackTime = System.currentTimeMillis();

                    // Some sort of nested clbss (do)ask confirm this is just starting a thread
                    Runnbble resetter = new Runnable() {

                    	// Does b thread run this and exit, or something else (do)
                        public void run() {

                            boolebn changed = false;
                            synchronized (Acceptor.clbss) {

                            	// This would blways be true (do)
                                if (_lbstIncomingTime < currTime) {

                                	// No computer hbs connected to us in awhile, set _acceptedIncoming to false
                                	// If the connection requests work, _bcceptedIncoming will be set back to true very soon
                                    chbnged = setIncoming(false);
                                }
                            }

                            // If we just chbnged _acceptedIncoming from true to false, tell the router service about it
                            if (chbnged) RouterService.incomingStatusChanged();
                        }
                    };

                    // Hbve the router service call this run method again in 30 seconds
                    RouterService.schedule(resetter, WAIT_TIME_AFTER_REQUESTS, 0);
                }
            }
        }
    }
}
