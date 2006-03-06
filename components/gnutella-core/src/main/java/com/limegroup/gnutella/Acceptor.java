
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.chat.ChatManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SettingsHandler;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Holds our listening socket that remote computers connect to.
 * Looks at the first 8 bytes the remote computer sends to find out what it wants, then hands the connection off to a thread.
 * To find out what our IP address is, call getAddress().
 * Code here uses UPnP to traverse NAT and become externally contactable on the Internet.
 * 
 * This class has a special relationship with UDPService and should be the only class that intializes it.
 */
public class Acceptor implements Runnable {

	/** A log that we can write lines of text into as the code here runs */
    private static final Log LOG = LogFactory.getLog(Acceptor.class);

    // Time delays to wait while checking to see if we are externally contactable on the Internet, or trapped behind a NAT device
    /** 30 minutes, if a remote computer hasn't connected to us in a half hour, we might not be externally contactable anymore */
    static long INCOMING_EXPIRE_TIME     = 30 * 60 * 1000; // 30 minutes in milliseconds
    /** 30 seconds after asking 2 ultrapeers to connect back to us, Call IncomingValidator.run() again */
    static long WAIT_TIME_AFTER_REQUESTS =      30 * 1000; // 30 seconds in milliseconds
    /** Run the IncomingValidator every 10 minutes to have it check if we are externally contactable */
    static long TIME_BETWEEN_VALIDATES   = 10 * 60 * 1000; // 10 minutes in milliseconds

    /** A reference to the UPnP manager to use, or null if we don't have at least Java 1.4 */
    private static final UPnPManager UPNP_MANAGER =
    	(CommonUtils.isJava14OrLater() && !ConnectionSettings.DISABLE_UPNP.getValue()) // If we are running on Java 1.4 or later and settings allow UPnP
    	? UPnPManager.instance()                                                       // Use the UPnPManager
        : null;                                                                        // Otherwise, don't try to do UPnP at all

    /**
     * Our socket that listens for incoming TCP connections.
     * Commonly called the listening socket or the server socket.
     * This is how remote computers on the Internet connect to us.
     * 
     * We'll tell our listening socket what port to listen on.
     * Later, we can change it to listen on a different port.
     * 
     * This looks like a java.net.ServerSocket object, but it's actually a LimeWire NIOServerSocket.
     * 
     * Locking:
     * Obtain _socketLock before modifying either.
     * Notify _socketLock when done.
     */
    private volatile ServerSocket _socket = null;

    /**
     * The port number our TCP and UDP server sockets are listening on.
     * 
     * The init() method chooses a random port number, starts our sockets listening on it, and saves it here.
     */
    private volatile int _port = 6346; // Initialize to 6346, the default port for Gnutella

    /**
     * Lock on this object when setting the listening socket.
     * 
     * All objects in Java can be used for thread synchronization.
     * This is the default root object, of type Object.
     */
    private final Object SOCKET_LOCK = new Object();

    /**
     * Our IP address on the LAN, like 192.168.0.102.
     * Java tells us this when we call InetAddress.getLocalHost().
     * When we have a new connection socket, we also set this with connectionSocket.getLocalAddress(), which tells us the same thing.
     * 
     * We find out what our own LAN IP address is in 3 stages:
     * (1) First, Java sets the 4 bytes of _address to 0.
     * (2) Java's InetAddress.getLocalHost() sets it to our internal LAN address, like "192.168.0.102".
     * (3) When we establish a connection, _address is set to the local address of the connection socket, this is still just our LAN address, though
     * 
     * Step 2 can fail because of JDK bug #4073539.
     * If the update checker is trying to resolve addresses, step 2 will block, this is JDK bug #4147517.
     * Since step 2 might block, it can't occur in the main thread.
     * 
     * In step 3, we set _address from the socket of each connection we establish.
     * This way, if some router between us and the Internet assigns us a new IP address, we'll switch _address to it as soon as we find out about it.
     * Step 3 ignores local addresses that look like "127.x.x.x".
     * 
     * These steps may mean it takes several seconds before our socket is listening, but this delay is acceptable.
     * 
     * Locking: Obtain Acceptor.class' lock.
     */
    private static byte[] _address = new byte[4];

    /**
     * Our external IP address on the Internet, like 216.27.178.74.
     * This is the address that remote computers tell us we have from where they are on the far side of the Internet.
     * 
     * Locking: Obtain Acceptor.class' lock.
     */
    private static byte[] _externalAddress = new byte[4];

	/**
	 * True when we have accepted an incoming connection.
	 * Starts out false.
	 * Actually getting a connection from a remote computer proves that we are externally contactable on the Internet.
	 * The user has no firewall, setup port forwarding, or we traversed a NAT with UPnP.
	 */
	private volatile boolean _acceptedIncoming = false; // We have not proved that we are externally contactable yet

    /**
     * The time when we last accepted an incoming connection.
     * This is the time we last set _acceptedIncoming to true.
     * If we were externally contactable but then port forwarding got reset or our IP address changed, we won't get any more connections.
     * This time lets us notice that no computers have connected to us in awhile.
     */
    private volatile long _lastIncomingTime = 0; // Initialize to 0, meaning no time set, also can mean it's new years 1970

    /**
     * The time when we most recently did a connect-back check.
     * This involves asking 2 ultrapeers to try to connect to our TCP listening socket with the greeting "CONNECT".
     * Initialized to now because we do a connect-back check when the program runs.
     */
    private volatile long _lastConnectBackTime = System.currentTimeMillis(); // Set to the number of milliseconds since January 1970

	/**
	 * Set _address, our LAN or Internet IP address.
	 * 
	 * We just discovered what our own IP address is.
	 * Call setAddress to save it in the _address member variable.
	 * This is the address we send in pong and query reply Gnutella packets.
	 * 
	 * Addresses that start 0, 255, or 127 are invalid or local, and not taken.
	 * This method lets us get around JDK bug #4073539.
	 * It lets us deal with our NAT or ISP changing our IP address while we are online.
     * 
     * @param address The IP address of our computer here
	 */
	public void setAddress(InetAddress address) {

		// Make sure the given address doesn't start with a 0 or 255
		byte[] byteAddr = address.getAddress();
		if (!NetworkUtils.isValidAddress(byteAddr)) return;

		// If the address starts 127 and settings forbid a Gnutella connection to another computer on our LAN, don't take it
		if (byteAddr[0] == 127 && ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) return;

		// Keep track of whether or not storing this new address will actually change the address we're storing
        boolean addrChanged = false;

        // There is only one Acceptor class, make sure only one thread can access the lines of code here at a time
		synchronized (Acceptor.class) { // Use the Acceptor class itself as the lock to synchronize on

			// If the given IP address is already in _address, we don't have to change anything
		    if (!Arrays.equals(_address, byteAddr)) {
		    	
		    	// It's not, set the new address and record that we changed _address
		    	_address = byteAddr;
			    addrChanged = true;
			}
		}

		// If we changed _address, tell the router service
		if (addrChanged) RouterService.addressChanged();
	}

	/**
	 * Set _externalAddress, our IP address on the Internet.
	 * 
	 * A remote computer just told us what our IP address looks like from its side of the connection.
	 * Call setExternalAddress to save it in the _externalAddress member variable.
	 * Addresses that start 0, 255, or 127 are invalid or local, and not taken.
	 * 
	 * @param address Our address on the Internet that a remote computer just told us we have
	 */
	public void setExternalAddress(InetAddress address) {

		// Read the 4 bytes of the IP address from the given InetAddress object
	    byte[] byteAddr = address.getAddress();

		// If the address starts 127 and settings forbid a Gnutella connection to another computer on our LAN, don't take it
		if (byteAddr[0] == 127 && ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) return;

        // There is only one Acceptor class, make sure only one thread can access the lines of code here at a time
		synchronized (Acceptor.class) { // Use the Acceptor class itself as the lock to synchronize on

			// Save the given value in the member variable
			_externalAddress = byteAddr;
		}
    }

	/**
	 * Chooses a random port number and opens TCP and UDP sockets that listen on it.
	 * 
	 * The program calls Acceptor.init() once as it starts to run.
	 * 
	 * Here's what init() does:
	 * Chooses a random port number from 2000 through 51999.
	 * Asks InetAddress.getLocalHost() for our local LAN address, and saves it in _address.
	 * Calls setListeningPort(port) to start our TCP and UDP sockets listening on the port we selected.
	 * Saves the port number we're listening on in _port.
	 * Uses UPnP to have the NAT forward that port to us.
	 */
	public void init() {

		// The port number we will set our listening socket to listen on
        int tempPort;

        // We'll generate a random port number to try to listen on if all of the following things are true
        boolean tryingRandom =
        	ConnectionSettings.PORT.isDefault() &&                   // We haven't randomly selected a port number yet, it's still 6346
            !ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue() && // A remote computer has never connected to us
            !ConnectionSettings.FORCE_IP_ADDRESS.getValue();         // We don't have our real Internet IP address saved in settings

        // Generate a random port number for tempPort
        Random gen = null; // Java's random number generator
        if (tryingRandom) {

        	// Make a random number generator and use it to generate a random number from 2000 through 51999
            gen = new Random();
            tempPort = gen.nextInt(50000) + 2000; // nextInt returns 0 through 49999

        // Use the value the user set in program settings instead
        } else {

        	// In settings, this is the "Listening Port", not the "Router Configuration Port" in settings
            tempPort = ConnectionSettings.PORT.getValue();
        }

        // Get our local address
        // We have to do this here because the UPnP manager can block
        try {

        	// Ask Java what our IP address is
        	// It will return the LAN address of our router, like 192.168.0.102
            InetAddress ourAddress = InetAddress.getLocalHost();

            // If we are using the UPnP object, have it tell us our address instead
            // The UPnP manager calls InetAddress.getLocalHost again, because actually asking the NAT device would be too slow
            if (UPNP_MANAGER != null) ourAddress = UPnPManager.getLocalAddress();

            // Save this value into _address
            setAddress(ourAddress);

        // Java couldn't find an IP address for the host, which in this case is our computer here
        } catch (UnknownHostException e) {

        // If Java security won't let us ask for our own IP address, InetAddress.getLocalHost will throw a SecurityException
        } catch (SecurityException e) {}

        // Make our TCP and UDP listening sockets and start them listening on the port number we've chosen
        int oldPort = tempPort;
        try {

        	// Make TCP and UDP soockets that listen on the port, and save the port number in _port
			setListeningPort(tempPort);
			_port = tempPort; // If control reaches here, setListeningPort didn't throw an exception

		// The port number is already taken for TCP or UDP
        } catch (IOException e) {

        	LOG.warn("can't set initial port", e);

        	// Try a different random port number, up to 20 times
            int numToTry = 20;
            for (int i = 0; i < numToTry; i++) {

            	// Choose a new random port number from 2000 through 5199 for this try
                if (gen == null) gen = new Random();  // Make a random number generator if we don't already have one
                tempPort = gen.nextInt(50000) + 2000; // Avoid port numbers 1-1999 ports because some there are used for standard Internet services

                // The number we randomly selected happens to be 6347, the port number our multicast UDP LAN socket listens on
				if (tempPort == ConnectionSettings.MULTICAST_PORT.getValue()) {

					// Choose a different random port number
				    numToTry++;
				    continue;
				}

                try {
                	
                	// Make TCP and UDP sockets that listen on the port, and save the port number in _port
                    setListeningPort(tempPort);
					_port = tempPort; // If control reaches here, setListeningPort didn't throw an exception

					// Our sockets are listening, leave the for loop
                    break;

                // The setListeningPort method threw an exception because the port number we choose is already in use for TCP or UDP
                } catch (IOException e2) {

                	// Log this happened, but keep going, we'll choose a different random port number and try again
                	LOG.warn("can't set port", e2);
                }
            }
            
            // If we tried 20 different port numbers and still don't have a listening socket, we've failed
            if (_socket == null) MessageService.showError("ERROR_NO_PORTS_AVAILABLE");
        }
        
        // We just changed the port we're listening on, or we chose the port number randomly
        if (_port != oldPort || tryingRandom) {
        	
        	// Save the random port number into settings so it shows up in the program settings dialog box
            ConnectionSettings.PORT.setValue(_port); // The first time the program runs, this changes it from 6346 to our randomly chosen value
            SettingsHandler.save(); // Save the updated settings to the disk file

            // Tell the router service that our IP address changed
            RouterService.addressChanged();
        }
        
        // Send UPnP commands to the NAT router on the LAN between us and the Internet
        // Get it to forward a port back to us so remote computers can contact us
        if (_socket != null && UPNP_MANAGER != null) {

        	// Early in the code that runs when the program starts, we sent out a UPnP command to find and start the NAT router's UPnP device
            UPNP_MANAGER.waitForDevice(); // If it still hasn't responded, wait 3 more seconds
        	UPNP_MANAGER.stop();          // Clear the resources finding the NAT device used

        	// True if the UPnP manager found a NAT it can talk to
        	boolean natted = UPNP_MANAGER.isNATPresent();

        	// True if our sockets are listening
        	boolean validPort = NetworkUtils.isValidPort(_port); // True if _port is between 1 and 65535

        	// True if our real Internet IP address is in settings and we don't have a UPnP mapping on the NAT right now
        	boolean forcedIP = ConnectionSettings.FORCE_IP_ADDRESS.getValue() && !ConnectionSettings.UPNP_IN_USE.getValue();

        	if (LOG.isDebugEnabled()) LOG.debug("Natted: " + natted + ", validPort: " + validPort + ", forcedIP: " + forcedIP);
        	
        	// There is a NAT we can talk to, our sockets are listening, and we don't have an IP address we have to use
        	if (natted && validPort && !forcedIP) {

        		/*
        		 * The UPnP mapping is only there while the program is running.
        		 * If the program creates a port mapping when it starts up, it deletes it when it shuts down.
        		 * 
        		 * UPNP_MANAGER.mapPort(_port) creates the port mapping now.
        		 * UPNP_MANAGER.clearMappingsOnShutdown() has the router service use the UPnP manager to remove the port mappings when the program shuts down.
        		 */

        		// Have the UPnP manager send commands to the NAT to forward the port number in _port back to us
        		int mappedPort = UPNP_MANAGER.mapPort(_port);
        		if (LOG.isDebugEnabled()) LOG.debug("UPNP port mapped: " + mappedPort);

        		// Setting up port forwarding with UPnP worked
			    if (mappedPort != 0) {

			    	// Create a thread that waits until the program shuts down, when the router service tells it to delete the port mapping
			        UPNP_MANAGER.clearMappingsOnShutdown();

			        /*
			         * If LimeWire stops running without being able to shut down properly, these settings will let it clean up the UPnP mapping when it runs the next time.
			         */

			        // Save information the program will read the next time it starts if it isn't shut down properly
        		    ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);  // Record that we're saving our IP address and port number in settings
        	        ConnectionSettings.FORCED_PORT.setValue(mappedPort); // Keep the port number we forwarded with UPnP in settings
        	        ConnectionSettings.UPNP_IN_USE.setValue(true);       // Make a note that there is a UPnP mapping on the NAT right now

        	        // If the port the UPnP manager mapped is different from the port our sockets are listening on, tell the router service our address changed
        	        if (mappedPort != _port) RouterService.addressChanged();

        	        /*
        	         * We could get our external address from the NAT, but it's too slow.
        	         * So, we clear the last connect-back times.
        	         * This will not help with already established connections, but if we establish new ones in the near future.
        	         */

        	        // Set _lastConnectBackTime as though we sent TCP and UDP connect-back messages a half hour ago
        		    resetLastConnectBackTime();
        		    UDPService.instance().resetLastConnectBackTime();
			    }
        	}
        }
	}

    /**
     * Start the threads that wait for remote comptuers to contact us by TCP, UDP, and multicast UDP.
     * 
     * RouterService.start() calls this as part of the program startup sequence.
     */
	public void start() {

		// Start listening for UDP and multicast UDP packets
	    MulticastService.instance().start(); // Start the multicast thread
	    UDPService.instance().start();       // Registers the UDP channel for NIO reads and writes

	    // Start a thread that will wait for remote computers to contact us
		Thread at = new ManagedThread(this, "Acceptor"); // Make a new thread named "Acceptor"
		at.setDaemon(true);                              // The Java virtual machine exits when the only threads running are all daemon threads
		at.start();                                      // Start the thread, it will run the ManagedThread.run() method

		// Setup the router service to run IncomingValidator every 10 minutes
		RouterService.schedule(
            new IncomingValidator(), // Make a new IncomingValidator object that will notice when we haven't received a connection in a long time
            TIME_BETWEEN_VALIDATES,  // Have the RouterService run the IncomingValidator 10 minutes from now
            TIME_BETWEEN_VALIDATES); // After that, have the RouterService run the IncomingValidator every 10 minutes
	}

	/**
	 * Returns true if remote computers agree with us when we tell them our IP address.
	 * 
	 * getAddress(true) is our address from settings or _address.
	 * It's our LAN address until a remote computer contacts us, then we change it to be our Internet address.
	 * It's the one we're writing into Gnutella packets.
	 * _externalAddressis the address that remote computers say we have.
	 * 
	 * Returns whether or not our advertised IP address
	 * is the same as what remote peers believe it is.
	 * 
	 * @return True if we're externally contactable and telling remote computers our Internet IP address.
	 *         False if we're behind a NAT and telling computers our LAN address.
	 */
	public boolean isAddressExternal() {

		// If the connection settings allow local connections, another computer on the same LAN connecting to us would be OK
		// So, even our LAN address is externally contactable, we know our externally contactable address, return true
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) return true;

        // Return true if _address == _externalAddress, false otherwise
        synchronized (Acceptor.class) { return Arrays.equals(getAddress(true), _externalAddress); }
	}

	/**
	 * Returns the IP address remote computers tell us is ours.
	 * 
	 * @return The 4 bytes of _externalAddress
	 */
	public byte[] getExternalAddress() {

		// Return our IP address as reported by remote computers
	    synchronized (Acceptor.class) { return _externalAddress; }
	}

    /**
     * Our IP address on the Internet if we're externally contactable, or just here on the LAN if we're not.
     * 
     * When the program runs, getAddress returns our address on the LAN, like 192.168.0.102.
     * After we've traversed the NAT and remote computers can contact us, getAddress returns our Internet IP address, like 216.27.178.74.
     * 
     * This is the address we write into the pong and query request Gnutella packets we send out.
     * If we're not externally contactable, we'll send our LAN address.
     * Remote computers will notice this is a LAN address, and interpret this to mean we are not externally contactable.
     * 
     * @param checkForce True to use the address from settings if one is set, false to always return _address, our LAN address
     * @return           The 4 bytes of our IP address.
     */
    public byte[] getAddress(boolean checkForce) {

    	// The caller wants us to look in settings for our real Internet IP address, and we've stored it there
	    if (checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {

	        // Get it
            String address = ConnectionSettings.FORCED_IP_ADDRESS_STRING.getValue();

            try {

                // Convert it into an InetAddress object, and return it
                InetAddress ia = InetAddress.getByName(address);
                return ia.getAddress();

            // Converting the text to an InetAddress caused an exception, ignore it and continue to return _address instead
            } catch (UnknownHostException err) { }
        }

		// Return _address, our LAN IP address if we're not externally contactable, or our Internet IP address if we are
        synchronized (Acceptor.class) { return _address; }
    }

    /**
     * Returns the port number our TCP and UDP listening sockets are listening on.
     * These are the sockets the connection manager uses to listen for incoming connections.
     * 
     * @param checkForce True to return the forced port number from settings, if settings has one
     * @return           The port number our TCP and UDP sockets are listening on
     */
    public int getPort(boolean checkForce) {

    	// If the caller wants us to look in settings, and we've saved our real Internet IP address there, return the port number stored next to it
    	if (checkForce && ConnectionSettings.FORCE_IP_ADDRESS.getValue()) return ConnectionSettings.FORCED_PORT.getValue();

    	// Otherwise, return the port number our TCP and UDP sockets are listening on
    	return _port;
    }

    /**
     * Make the TCP and UDP listening sockets listen on the given port number.
     * 
     * Sets the port on which the ConnectionManager and UDPService sockets are listening.
     * If the given port number is already for TCP or UDP, setListeningPort throws an IOException so you can try a different port number.
     * 
     * You can use setListeningPort to setup listening, or change to a different port number.
     * You can also use it to close our listening sockets, call setListeningPort(0) to do this.
     * 
     * There are 3 listening sockets, here are their types and names:
     * 
     * java.net.ServerSocket    Acceptor._socket         Listens for TCP connections on the given port number
     * java.net.DatagramSocket  UDPService._channel      Listens for UDP packets on the same given port number
     * java.net.MulticastSocket MulticastService._socket Uses 234.21.81.1:6347 to communicate with multicast on the LAN
     * 
     * @param port The new port number to start our TCP and UDP listening sockets listening on
     */
    public void setListeningPort(int port) throws IOException {

    	// If we already have our listening socket listening on that port, do nothing
        if (_socket != null && _port == port) return;

        // If port is 0, the caller wants us to stop listening and close all our listening sockets
        if (port == 0) {

        	LOG.trace("shutting off service.");

        	/*
        	 * We must close the socket before grabbing the lock
        	 * Otherwise, deadlock will occur because the acceptor thread is listening to the socket while holding the lock
        	 * The port won't be 0 until we've grabbed the lock
        	 */

        	// If we have a socket listening on TCP for incoming connections
            if (_socket != null) {

            	try {

            		// Close our TCP listening socket
            		_socket.close(); // Any thread currently blocked in accept() will throw a SocketException

            	} catch (IOException e) { }
            }

            synchronized (SOCKET_LOCK) {
            	
            	// Null the reference and zero the port number
                _socket = null;
                _port   = 0; // This is where we stored the port number our socket was listening on

                // Release the thread that is blocking on accept()
                SOCKET_LOCK.notify();
            }

            // Shut off the UDP and UDP multicast listening sockets also
            UDPService.instance().setListeningSocket(null);
            MulticastService.instance().setListeningSocket(null);            

            LOG.trace("service OFF.");

            // We've closed our TCP, UDP, and UDP multicast sockets
            return;
        }

        /*
         * Since we want the UDP service to bind to the same port as the Acceptor, we need to be careful.
         * We need to confirm that the port can be bound by both UDP and TCP before accepting the port as valid.
         * We first attempt to bind the port for UDP traffic.
         * If that fails, it throws an IOException.
         * If it works, we keep that bound DatagramSocket around and try to bind the port to TCP.
         * If that fails, it throws an IOException, and we close the bound DatagramSocket.
         * If both operations work, we save or new TCP and UDP sockets.
         */

        if (LOG.isDebugEnabled()) LOG.debug("changing port to " + port);

        // Make the new UDP socket that we'll listen on
        DatagramSocket udpServiceSocket = UDPService.instance().newListeningSocket(port); // Tell it what port it will be listening on
        LOG.trace("UDP Service is ready.");

        // Make the multicast UDP socket the MulticastService object will keep
        MulticastSocket mcastServiceSocket = null;
        try {

        	// It's IP address and port number is always 234.21.81.1:6347, one more than the default port number for Gnutella
        	InetAddress mgroup = InetAddress.getByName(ConnectionSettings.MULTICAST_ADDRESS.getValue());
        	mcastServiceSocket = MulticastService.instance().newListeningSocket(ConnectionSettings.MULTICAST_PORT.getValue(), mgroup);
        	LOG.trace("multicast service setup");

        } catch (IOException e) {

        	LOG.warn("can't create multicast socket", e);
        	mcastServiceSocket = null;
        }

        // Make the TCP socket this Acceptor object will keep
        ServerSocket newSocket = null;
        try {

        	// Make a new NIOServerSocket and start it listening on the given port number
        	newSocket = new com.limegroup.gnutella.io.NIOServerSocket(port);

        } catch (IOException e) {

        	LOG.warn("can't create ServerSocket", e);
        	udpServiceSocket.close();
        	throw e;

        } catch (IllegalArgumentException e) {

        	LOG.warn("can't create ServerSocket", e);
        	udpServiceSocket.close();
        	throw new IOException("could not create a listening socket");
        }

        // If the Acceptor object already has a TCP listening socket, close it
        if (_socket != null) try { _socket.close(); } catch (IOException e) { }

        // Save our new listening socket in this Acceptor object
        synchronized (SOCKET_LOCK) {

        	// Save our new TCP listening socket in Acceptor._socket
        	_socket = newSocket;
        	_port   = port; // Save the port number it is listening on

        	// Release the thread that is blocking on accept()
        	SOCKET_LOCK.notify();
        }

        LOG.trace("Acceptor ready..");

        // Earlier, we made UDP adn multicast UDP sockets that also listen
        // Save them in the program's instances of the services they are used for
        UDPService.instance().setListeningSocket(udpServiceSocket);
        if (mcastServiceSocket != null) MulticastService.instance().setListeningSocket(mcastServiceSocket);

        // We now have TCP and UDP sockets listening on the given port number
        if (LOG.isDebugEnabled()) LOG.debug("listening UDP/TCP on " + _port);
    }

	/**
	 * True if we've accepted an unsolicited connection from a remote computer on the Internet.
	 * If so, it's proof that we're not trapped behind a NAT or firewall.
	 * 
	 * @return True if a remote computer has recently connected to us
	 */
	public boolean acceptedIncoming() {

		// The setIncoming method saved this value here
		return _acceptedIncoming;
	}

	/**
	 * Set _acceptedIncoming, which is true if a remote computer has contacted us.
	 * 
	 * This is part of the program's connect back check system, which works like this:
	 * When the program runs and every 10 minutes after that, the program does a connect-back check.
	 * It contacts 2 ultrapeers we're connected to, and asks them to try connecting to our listening socket.
	 * The ultrapeers connect with the greeting "CONNECT", which we read and then disconnect.
	 * 
	 * This setIncoming method gets called 2 places in the code:
	 * Acceptor.run() listens for new connections, and Acceptor.ConnectionDispatchRunner.run() reads the greeting message.
	 * These methods call checkFirewall(socket), which calls setIncoming(true).
	 * IncomingValidator.run() runs every 10 minutes and sends connect-back requests.
	 * It calls setIncoming(false).
	 * 
	 * @param status True if a remote computer just contacted us, false if one hasn't in awhile
	 * @return       True if this changes our accepted incoming status, false if it is a confirmation of the same status
	 */
	private boolean setIncoming(boolean status) {

		// If we've already got the new status, change nothing and report no change was needed
		if (_acceptedIncoming == status) return false;

		// The status is different, save it, tell the router service we changed it, and report we changed it
	    _acceptedIncoming = status;
		RouterService.getCallback().acceptedIncomingChanged(status);
	    return true;
	}
	
	/**
	 * Takes the socket of a new connection, and sets _acceptedIncoming to true if the computer that contacted us is remote.
	 * 
	 * This method gets called when a remote computer connects to our listening socket.
	 * The connection socket that gets created is passed here.
	 * We look at the connection socket to determine if the computer on the other end has a remote address.
	 * If it does, this is proof that we are externally contactable.
	 * We mark down that we received an incoming connection now, and change our status.
	 * 
	 * @param socket The connection socket accept() returned when a remote computer just connected to us
	 */
	private void checkFirewall(Socket socket) {

		// We haven't changed our firewalled state yet
		boolean changed = false;

        // Get the IP address of the remote computer at the far end of the connection socket
		// Make sure it's outside our LAN and we aren't connected to this remote computer already
        if (isOutsideConnection(socket.getInetAddress())) {

            synchronized (Acceptor.class) {

            	// Set Acceptor._acceptedIncoming, the EVER_ACCEPTED_INCOMING connection setting, and Acceptor._lastIncomingTime
                changed = setIncoming(true);                              // Set _acceptedIncoming to true and make changed true if it was false before
                ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true); // Record in connection settings that a remote computer contacted us
                _lastIncomingTime = System.currentTimeMillis();           // Keep track of when this happened so later we can notice it hasn't happened in awhile
            }
        }

        // If this was the first remote computer that contacted us, tell the router service about it
        if (changed) RouterService.incomingStatusChanged();
    }

    /**
     * Loops forever, waiting for remote computers to connect to our listening socket.
     * 
     * This line of code is key:
     * 
     *   client = _socket.accept();
     * 
     * _socket is our TCP listening socket.
     * accept() is a method which blocks until a remote computer contacts us.
     * client is a new TCP connection socket that accept returns when one does.
     * 
     * The program then hands the client connection socket to a new ConnectionDispatchRunner.
     * It runs in another thread, and reads the greeting from the remote computer.
     * 
     *  @modifies this, network, SettingsManager
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.  If the port was changed
     *   in order to accept incoming connections, SettingsManager is
     *   changed accordingly.
     */
    public void run() {

    	// Loop forever waiting for remote computers to connect to our TCP listening socket
        while (true) {

            try {

            	/*
            	 * Accept an incoming connection, make it into a Connection object, handshake, and give it to a thread to service it.
            	 * If not bound to a port, wait until we are.
            	 * If the port is changed while we are waiting, IOException will be thrown, forcing us to release the lock.
            	 */

            	// The socket named client is our new connection to a remote computer
            	Socket client = null;

            	// Wait here until another thread using _socket is done
            	synchronized (SOCKET_LOCK) {

                	// We have our TCP socket listening
                	if (_socket != null) {

                    	try {

                        	// Wait here until a remote computer connects to us
                            client = _socket.accept(); // Returns a new socket which is our connection to the remote computer
                            
                            /*
                             * The types here are not exactly as they seem.
                             * _socket looks like a java.net.ServerSocket, but it's actually an NIOServerSocket.
                             * accept() doesn't call into Java, but into NIOServerSocket.accept() instead.
                             * client looks like a java.net.Socket, but it's actually a NIOSocket.
                             */

                        } catch (IOException e) {

                        	// Make a note of the exception and go back to the start of the while loop
                        	LOG.warn("IOX while accepting", e);
                            continue;
                        }

                    // We don't have a listening socket right now
                    } else {

                        try {

                        	/*
                        	 * When the socket lock is notified, the socket will be available.
                        	 * So, just wait for that to happen and go around the loop again.
                        	 */
                        	
                        	// Wait here until our listening socket is setup again
                            SOCKET_LOCK.wait(); // Blocks until the socket lock gets notified

                        } catch (InterruptedException e) { }

                        // Go back to the start of the while loop
                        continue;
                    }
                }

            	// Get the remote computer's IP address
            	// This is the remote computer's real, externally contactable Internet IP address as we see it from here
				InetAddress address = client.getInetAddress();

				// Make sure the remote computer that just connected to us hasn't already disconnected
				if (address == null) { // If the remote computer immediately closed the connection, getInetAddress will return null

					// Close our end of the new connection socket, and go back to the start of the inifite loop
				    LOG.warn("connection closed while accepting");
				    try { client.close(); } catch (IOException ignored) { }
				    continue;
				}

				// Make sure the remote computer's IP address isn't on a list of institutional addresses the user does not want to connect to
                if (isBannedIP(address.getAddress())) {

					// Close our end of the new connection socket, and go back to the start of the inifite loop
                	if (LOG.isWarnEnabled()) LOG.warn("Ignoring banned host: " + address);
                    HTTPStat.BANNED_REQUESTS.incrementStat();
                    try { client.close(); } catch (IOException ignored) {}
                    continue;
                }
                
                // If settings allow us to set _acceptedIncoming to true when a remote computer calls us, and not just from a connect back request
                if (!ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getValue()) {

                	// Set _acceptedIncoming to true if the remote computer is truly remote
                	checkFirewall(client);
                }

                // Set our IP address to the local address of this socket
                InetAddress localAddress = client.getLocalAddress(); // Still just returns our LAN address, like 192.168.0.102
                setAddress(localAddress);

                // Start a new thread on ConnectionDispatchRunner.run()
                ConnectionDispatchRunner dispatcher = new ConnectionDispatchRunner(client); // Give the object the connection socket
				Thread dispatchThread = new ManagedThread(dispatcher, "ConnectionDispatchRunner");
				dispatchThread.setDaemon(true); // The Java virtual machine exits when the only threads running are all daemon threads
				dispatchThread.start();
				
			// If any of that caused an exception, just tell it to the error service and keep going
            } catch (Throwable e) { ErrorService.error(e); }
        }
    }

    /**
     * Determine if a given IP address is from an outside connection or not.
     * 
     * If it is, then a remote computer really was able to connect to us, and we can set acceptedIncoming to true.
     * 
     * This method returns false if the address is a private or local address, or on the same subnet.
     * It also makes sure it's not the address of a remote computer that we're already connected to.
     * 
     * @param addr The IP address to check
     * @return     True if it's an external Internet IP address from a computer that's really remote
     */
    private boolean isOutsideConnection(InetAddress addr) {

    	// If settings allow us to connect to other computers here on the LAN, all connections are outside connections
        if (!ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) return true;

        // Return true if we're not already connected to this computer and it has a remote address
        // TODO:kfaaborg The local variable bytes is never read, this line can be eliminated
        byte[] bytes = addr.getAddress();
        return !RouterService.isConnectedTo(addr) && // We're not already connected to this remote computer, and
               !NetworkUtils.isLocalAddress(addr);   // The address doesn't start 127 and is't our own IP address on the LAN
	}

    /**
     * Specialized class for dispatching incoming TCP connections to their
     * appropriate handlers.  Gnutella connections are handled via 
     * <tt>ConnectionManager</tt>, and HTTP connections are handled
     * via <tt>UploadManager</tt> and <tt>DownloadManager</tt>.
     */
    private static class ConnectionDispatchRunner implements Runnable {

        /**
         * The connection socket we can talk to the remote computer through.
         */
        private final Socket _socket;

        /**
         * Make a new ConnectionDispatchRunner object.
         * 
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {

        	// Save the given connection socket in this new ConnectionDispatchRunner object
           _socket = socket;
        }

        /**
         * The thread starts here in the run() method.
         * 
         * Dispatches the new connection based on connection type, such
         * as Gnutella, HTTP, or MAGNET.
         */
        public void run() {

            /*
             * Get access to the managers for connections, uploads, and downloads, and the acceptor
             * When the program started, it made exactly one of each of these objects
             */

            // Get references to related objects
			ConnectionManager cm = RouterService.getConnectionManager(); // The ConnectionManger keeps a list of our Gnutella connections
			UploadManager     um = RouterService.getUploadManager();
			DownloadManager   dm = RouterService.getDownloadManager();
			Acceptor          ac = RouterService.getAcceptor();          // The Acceptor holds the listening socket that remote computers connect to

			try {

				// Get the stream we can read to download data from the remote computer
                InputStream in = null;
                try {

                	// Ask the connection socket for the stream
                    in = _socket.getInputStream();

                } catch (IOException e) {

                	// Record one more closed (do)ask we're listening for connections here, but requests sounds like we requested the connection
                    HTTPStat.CLOSED_REQUESTS.incrementStat();
                    throw e;

                } catch (NullPointerException e) {

                    // This is JDK bug 4091706, and should happen very rarely
                    throw new IOException(e.getMessage());
                }

                /*
                 * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
                 * With this option set to a non-zero timeout,
                 * a read() call on the InputStream associated with this Socket will block for only this amount of time.
                 * If the timeout expires, a java.io.InterruptedIOException is raised, though the Socket is still valid.
                 */

                // The remote computer connected to us, and should now send some data
                // Read the first 8 bytes of what it sends, it should be the ASCII text "GNUTELLA"
                _socket.setSoTimeout(Constants.TIMEOUT);      // Tell the socket to only wait 8 seconds for a read before returning with nothing
                String word = IOUtils.readLargestWord(in, 8); // Read up to 8 bytes from the input stream
                _socket.setSoTimeout(0);                      // Put the socket back so read() will block forever until data arrives

                // True if the remote computer's IP address is "127.0.0.1", which is localhost, the loop back address
                // If the remote computer has this IP, it means it's actually just anothe rprogram running on the same computer we are
                // If the program connecting to us is the magnet handler, that's ok
                boolean localHost = NetworkUtils.isLocalHost(_socket);

                // The remote computer connected to us with a starting message other than "MANGNET"
				if (!word.equals("MAGNET")) {

					// If settings allow a LAN connection and this is another program running on this computer 
					if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && localHost) {

						// Only the magnet link program can talk to us this way
					    LOG.trace("Killing localhost connection with non-magnet.");
						_socket.close();
						return;
					}

				// The remote computer connected saying "MAGNET" and isn't on the same computer as us
				} else if (!localHost) { // && word.equals(MAGNET)

					// Only the magnet program running here can talk to us this way
				    LOG.trace("Killing non-local ExternalControl request.");
				    _socket.close();
				    return;
				}

				/*
				 * (1) Gnutella connection.
				 * If the user hasn't changed the handshake string,
				 * we accept the default "GNUTELLA CONNECT/0.4" or
				 * the proprietary LimeWire string "LIMEWIRE CONNECT/0.4".
				 * Otherwise, we just accept the user's value.
				 */

				// True if the user has not customized the connect string from "GNUTELLA CONNECT/0.4"
				// This is the old version of the Gnutella protocol, the remote computer probably told us 0.6 instead
                boolean useDefaultConnect = ConnectionSettings.CONNECT_STRING.isDefault();

                // The remote computer connected to us and the first word it sent is:

                // "GNUTELLA", the remote computer wants to do a Gnutella handshake with us and then exchange Gnutella packets
                if (word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {

                	// Hand the connection to the connection manager to do the Gnutella handshake
                    HTTPStat.GNUTELLA_REQUESTS.incrementStat();
                    cm.acceptConnection(_socket);

                // "LIMEWIRE", and the user has not customized the connect string, this is still just a regular Gnutella connection
                } else if (useDefaultConnect && word.equals("LIMEWIRE")) {

                	// Hand the connection to the connection manager to do the Gnutella handshake
                    HTTPStat.GNUTELLA_LIMEWIRE_REQUESTS.incrementStat();
                    cm.acceptConnection(_socket);

                // "GET", the remote computer wants to download a file from us, this uses HTTP, and is just like we're a Web server
                } else if (word.equals("GET")) {

                	// Hand the connection to the upload manager to serve the file
                	HTTPStat.GET_REQUESTS.incrementStat();
					um.acceptUpload(HTTPRequestMethod.GET, _socket, false);

			    // "HEAD", the remote computer wants to get the headers for a file download, this uses HTTP, and is just like we're a Web server
                } else if (word.equals("HEAD")) {

                	// Hand the connection to the upload manager to serve the file
					HTTPStat.HEAD_REQUESTS.incrementStat();
					um.acceptUpload(HTTPRequestMethod.HEAD, _socket, false);

				// "GIV"
				// We're externally contactable, but a computer that has a file we want isn't
				// We sent it a push packet with our IP address and the name of the file we want
				// Now it's connected to us, and wants to give us the file
				} else if (word.equals("GIV")) { 

					// Hand the connection to the download manager to download the file
                    HTTPStat.GIV_REQUESTS.incrementStat();
                    dm.acceptDownload(_socket);

                // "CHAT", the remote computer is connecting to us to chat
                } else if (word.equals("CHAT")) {

                	// Hand the connection to the chat manager
				    HTTPStat.CHAT_REQUESTS.incrementStat();
                    ChatManager.instance().accept(_socket);

                // "MAGNET", (do)ask is this the magnet handler program http://sourceforge.net/projects/magnethandler
				} else if (word.equals("MAGNET")) {

					// (do) is this the magnet handler program?
			        HTTPStat.MAGNET_REQUESTS.incrementStat();
                    ExternalControl.fireMagnet(_socket);

                // "CONNECT", or 2 line feed characters followed by a space
                // The remote computer is connecting to us just to let us know if it's possible
                } else if (word.equals("CONNECT") || word.equals("\n\n")) {

                	/*
                	 * Technically we could just always checkFirewall here, since we really always want to.
                	 * But, since we're going to check all incoming connections if this isn't set,
                	 * we might as well check and prevent a double check.
                	 */

                    // If settings allow us to use any incoming connection to determine if we're externally contactable
                    if (ConnectionSettings.UNSET_FIREWALLED_FROM_CONNECTBACK.getValue()) { // False by default

                    	// A remote computer just connected to us, set _acceptedIncoming to true
                    	ac.checkFirewall(_socket); // Give the method the socket so it can make sure the computer really is remote
                    }

                    // That was the only purpose of this TCP connection, close it
                    IOUtils.close(_socket);

                // The remote computer greeted us with something else
                // It could be a confused BitTorrent or eDonkey2000 computer
                } else {

                	// Record what it said and close the connection
                    HTTPStat.UNKNOWN_REQUESTS.incrementStat();
                    if (LOG.isErrorEnabled()) LOG.error("Unknown protocol: " + word);
                    IOUtils.close(_socket);
                }

            // Trying to read what the remote computer said caused an exception
            } catch (IOException e) {

            	// Close the connection
                LOG.warn("IOX while dispatching", e);
                IOUtils.close(_socket);

            // Something else threw an exception
            } catch(Throwable e) {

            	// Catch it and log it, and don't close the connection
				ErrorService.error(e);
			}

            // All the thread does is run this method, the run() method
            // When run() returns here, the thread dies
        }
    }

    /**
     * Sees if a given IP address is on our list of institutional addresses the user does not want to connect to.
     * 
     * @param addr The 4 bytes of an IP address
     * @return     True if this IP address is in the program's list of compuers to not talk to
     */
    public boolean isBannedIP(byte[] addr) {

		// Make sure the remote computer's IP address isn't on a list of institutional addresses the user does not want to connect to
        return !IPFilter.instance().allow(addr);
    }
    
    /**
     * Make it look like we haven't done a connect back in a little more than a half hour.
     * 
     * The program can ask the network to connect to us to help us determine if we are externally contactable or not.
     * Acceptor._lastConnectBackTime is the time when we last did this.
     * This method, resetLastConnectBackTime, artificially sets that time as a half our ago.
     * This will make it look like we haven't done it in awhile, and could do it again soon.
     */
    void resetLastConnectBackTime() {

    	// Set _lastConnectBackTime to what currentTimeMillis would have returned a half hour ago
        _lastConnectBackTime = System.currentTimeMillis() - INCOMING_EXPIRE_TIME - 1;
    }

    /**
     * Writes settings to clean up a UPnP mapping next time if the program terminates this time without shutting down.
     */
    public void shutdown() {

    	// If we setup port forwarding with UPnP when the program started up and ran init()
        if (UPNP_MANAGER != null         &&              // We made a the UPnP manager object, and
        	UPNP_MANAGER.isNATPresent()  &&              // It was able to talk to a NAT device on the LAN, and
            UPNP_MANAGER.mappingsExist() &&              // It created a port forwarding mapping for us, and
            ConnectionSettings.UPNP_IN_USE.getValue()) { // We saved a note that confirms all this in settings

        	/*
        	 * When the program shuts down, the router service will use the UPnP manager to remove the port mapping we set up.
        	 * But, the program might terminate unexpectedly, leaving the mapping in place.
        	 * So, we set these settings this way now.
        	 * If the program starts and they are set this way, we'll know there's a mapping we still need to remove.
        	 */

        	// Save settings that indicate we made a port mapping we still need to clean up
        	ConnectionSettings.FORCE_IP_ADDRESS.revertToDefault(); // Set back to false, meaning don't look in settings for our Internet IP address and port number
        	ConnectionSettings.FORCED_PORT.revertToDefault();      // Set back to 6346, meaning choose a new random port number to listen on
        	ConnectionSettings.UPNP_IN_USE.revertToDefault();      // Set back to false, meaning there is nothing in the NAT UPnP needs to clean up
        	// TODO:kfaaborg How is the program supposed to clean up the mapping when we've erased information about it from settings?
        }
    }

    /**
     * Performs connect-back checks to keep _acceptedIncoming correct.
     * 
     * The router service calls the run() method here every 10 minutes.
     */
    private class IncomingValidator implements Runnable {

    	/** Make a new IncomingValidator object that can do callback requests and keep _acceptedIncoming correct. */
        public IncomingValidator() {}

        /**
         * Every half hour, performs a connect-back check and sets _acceptedIncoming to false.
         * 
         * A connect-back check works like this:
         * We send Gnutella packets to 2 ultrapeers asking them to try to connect back to us.
         * They try to open new TCP socket connections from where they are to our listening socket.
         * If it works, the first thing they say is "CONNECT".
         * We look for this in run() above, and call checkFirewall(socket) which calls setIncoming(true).
         * 
         * But, at the same time, the RouterService is calling this run() method every 10 minutes.
         * It only does something if either of the following are true.
         * It's been a half hour or more since a remote compuer connected to us, or:
         * A remote computer has never connected to us, and it's been a half hour since we did a connect-back check.
         * Then this method asks 2 ultrapeers to try to connect to us.
         * 
         * If the connection requests went out correctly, this method sets _acceptedIncoming to false.
         * If we are externally contactable, the ultrapeers will connect to us with "CONNECT" in moments, and we'll set _acceptedIncoming back to true.
         */
        public void run() {

        	/*
        	 * Clear and revalidate if:
        	 * (1) We haven't had an incoming connection, or
        	 * (2) We've never had an incoming connection, and we haven't checked.
        	 */

        	// Get the time right now
            final long currTime = System.currentTimeMillis(); // Number of milliseconds since 1970, long is 64 bits big

            // Access the connection manager, which we might ask to send connectback requests
            final ConnectionManager cm = RouterService.getConnectionManager();

            if (

            	// A remote computer has connected to us, but that was more than a half hour ago, or
                (_acceptedIncoming && ((currTime - _lastIncomingTime) > INCOMING_EXPIRE_TIME)) ||

                // A remote computer has never connected to us, and we haven't done a connect-back check in over a half hour
                (!_acceptedIncoming && ((currTime - _lastConnectBackTime) > INCOMING_EXPIRE_TIME))) {

            	// Do a connect-back check that will tell us if we're externally contactable or not
            	// Have the connection manager tell 2 ultrapeers to try connecting to us
            	// They will open a new TCP socket connection to us with the greeting "CONNECT"
                if (cm.sendTCPConnectBackRequests()) { // If the connection manager sent some connection requests

                	// Record that we last did a connect-back check right now
                    _lastConnectBackTime = System.currentTimeMillis();

                    // Some sort of nested class (do)ask confirm this is just starting a thread
                    Runnable resetter = new Runnable() {

                    	// Does a thread run this and exit, or something else (do)
                        public void run() {

                            boolean changed = false;
                            synchronized (Acceptor.class) {

                            	// This would always be true (do)
                                if (_lastIncomingTime < currTime) {

                                	// No computer has connected to us in awhile, set _acceptedIncoming to false
                                	// If the connection requests work, _acceptedIncoming will be set back to true very soon
                                    changed = setIncoming(false);
                                }
                            }

                            // If we just changed _acceptedIncoming from true to false, tell the router service about it
                            if (changed) RouterService.incomingStatusChanged();
                        }
                    };

                    // Have the router service call this run method again in 30 seconds
                    RouterService.schedule(resetter, WAIT_TIME_AFTER_REQUESTS, 0);
                }
            }
        }
    }
}
