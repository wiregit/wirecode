package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.browser.ExternalControl;


/**
 * Listens on ports, accepts incoming connections, and dispatches threads to
 * handle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chat connections over TCP; more may be supported in the future.<p> 
 * This class has a special relationship with UDPService and should really be
 * the only class that intializes it.  See setListeningPort() for more
 * info.
 */
public class Acceptor implements Runnable {
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
     * The object to lock on while setting the listening socket
     */
    private final Object SOCKET_LOCK = new Object();

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
    private static byte[] _address = new byte[4];
    
	/**
	 * Variable for whether or not we have accepted an incoming connection --
	 * used to determine firewall status.
	 */
	private volatile boolean _acceptedIncoming = false;


	/**
     * @modifes this
     * @effects sets the IP address to use in pongs and query replies.  If addr
     *  is localhost (127.x.x.x), this is not modified.  This method must be
	 *  to get around JDK bug #4073539, as well as to try to handle the case 
	 *  of a computer whose IP address keeps changing.
	 */
	public void setAddress(InetAddress address) {
        //Ignore localhost.
		byte[] byteAddr = address.getAddress();
		if(byteAddr[0] == 127 && 
           ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
            return;
        }
        

		synchronized(Acceptor.class) {
			_address = byteAddr;
		}
	}

    /**
     * Launches the port monitoring thread, MulticastService, and UDPService.
     */
	public void start() {
	    MulticastService.instance().start();
	    UDPService.instance().start();
		Thread at = new Thread(this, "Acceptor");
		at.setDaemon(true);
		at.start();
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
		if(checkForce && SettingsManager.instance().getForceIPAddress())
			return SettingsManager.instance().getForcedIPAddress();
        synchronized (Acceptor.class) {
            return _address;
        }
    }

    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     *
     * @param checkForce whether or not to check if the port is forced.     
     * @return the listening port
     */
    public int getPort(boolean checkForce) {
        if(checkForce && SettingsManager.instance().getForceIPAddress())
			return SettingsManager.instance().getForcedPort();
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
        debug("Acceptor.setListeningPort(): entered.");
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
            debug("Acceptor.setListeningPort(): shutting off service.");
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            synchronized (SOCKET_LOCK) {
                _socket=null;
                _port=0;
                SOCKET_LOCK.notify();
            }

            //Shut off UDPService also!
            UDPService.instance().setListeningSocket(null);
            //Shut off MulticastServier too!
            MulticastService.instance().setListeningSocket(null);            

            debug("Acceptor.setListeningPort(): service OFF.");
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
            
            debug("Acceptor.setListeningPort(): changing port to " + port);

            DatagramSocket udpServiceSocket = 
                UDPService.instance().newListeningSocket(port);

            debug("Acceptor.setListeningPort(): UDP Service is ready.");
            
            MulticastSocket mcastServiceSocket = null;
            try {
                InetAddress mgroup = InetAddress.getByName(
                    ConnectionSettings.MULTICAST_ADDRESS.getValue()
                );
                mcastServiceSocket =                            
                    MulticastService.instance().newListeningSocket(
                        ConnectionSettings.MULTICAST_PORT.getValue(), mgroup
                    );
                debug("Acceptor.setListeningPort(): Multicast Service is ready.");
            } catch(IOException e) {
                mcastServiceSocket = null;
                debug("Acceptor.setListeningPort(): Unable to start multicast service.");
            }
            
        
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new ServerSocket(port);
            } catch (IOException e) {
                udpServiceSocket.close();
                throw e;
            } catch (IllegalArgumentException e) {
                udpServiceSocket.close();
                throw new IOException("could not create a listening socket");
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (SOCKET_LOCK) {
                _socket=newSocket;
                _port=port;
                SOCKET_LOCK.notify();
            }

            debug("Acceptor.setListeningPort(): I am ready.");

            // Commit UDPService's new socket
            UDPService.instance().setListeningSocket(udpServiceSocket);
            // Commit the MulticastService's new socket
            // if we were able to get it
            if ( mcastServiceSocket != null ) {
                MulticastService.instance().setListeningSocket(
                    mcastServiceSocket
                );
            }

            debug("Acceptor.setListeningPort(): listening UDP/TCP on " + _port);
        }
    }


	/**
	 * This method lets you know if this class has accepted
	 * an incoming connection at any point during the session.
	 * The boolean variable _acceptedIncoming is set to false
	 * by default, and true as soon as a connection is established.
	 */
	public boolean acceptedIncoming() {
		return _acceptedIncoming;
	}


    /** @modifies this, network, SettingsManager
     *  @effects accepts new incoming connections on a designated port
     *   and services incoming requests.  If the port was changed
     *   in order to accept incoming connections, SettingsManager is
     *   changed accordingly.
     */
    public void run() {
		SettingsManager settings = SettingsManager.instance();
		int tempPort = settings.getPort();

        //0. Get local address.  This must be done here because it can
        //   block under certain conditions.
        //   See the notes for _address.
        try {
            setAddress(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
		int oldPort = tempPort;
        Exception socketError = null;
        try {
			setListeningPort(tempPort);
			_port = tempPort;
        } catch (IOException e) {
            socketError = e;
            //2. Try 10 different ports
            int numToTry = 10;
            for (int i=0; i<numToTry; i++) {
				tempPort = i+6346;
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
                    socketError = e2;
                }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null) {
                ErrorService.error(socketError);
            }
        }
        socketError = null;

        if (_port!=oldPort) {
            settings.setPort(_port);
            settings.writeProperties();
        }

        while (true) {
            try {
                //Accept an incoming connection, make it into a
                //Connection object, handshake, and give it a thread
                //to service it.  If not bound to a port, wait until
                //we are.  If the port is changed while we are
                //waiting, IOException will be thrown, forcing us to
                //release the lock.
                Socket client=null;
                synchronized (SOCKET_LOCK) {
                    if (_socket!=null) {
                        try {
                            client=_socket.accept();
                        } catch (IOException e) {
                            continue;
                        }
                    } else {
                        // When the socket lock is notified, the socket will
                        // be available.  So, just wait for that to happen and
                        // go around the loop again.
                        try {
                            SOCKET_LOCK.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                //Check if IP address of the incoming socket is in _badHosts
				
				InetAddress address = client.getInetAddress();
                if (isBannedIP(address.getHostAddress())) {
                    client.close();
                    continue;
                }
				
				// we have accepted an incoming socket -- only record
                // that we've accepted incoming if it's definitely
                // not from our local subnet
                if(!NetworkUtils.isCloseIP(address.getAddress(), 
                                           getAddress(false)) ||
                   !ConnectionSettings.LOCAL_IS_PRIVATE.getValue()) {
                    _acceptedIncoming = true;
                    ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(_acceptedIncoming);
                }

                //Dispatch asynchronously.
                ConnectionDispatchRunner dispatcher =
					new ConnectionDispatchRunner(client);
				Thread dispatchThread = 
                    new Thread(dispatcher, "ConnectionDispatchRunner");
				dispatchThread.setDaemon(true);
				dispatchThread.start();

            } catch (SecurityException e) {
                ErrorService.error(e);
            } catch (Throwable e) {
                ErrorService.error(e);
            }
        }
    }

    /**
     * Specialized class for dispatching incoming TCP connections to their
     * appropriate handlers.  Gnutella connections are handled via 
     * <tt>ConnectionManager</tt>, and HTTP connections are handled
     * via <tt>UploadManager</tt> and <tt>DownloadManager</tt>.
     */
    private static class ConnectionDispatchRunner implements Runnable {

        /**
         * The <tt>Socket</tt> instance for the connection.
         */
        private final Socket _socket;

        /**
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {
            _socket = socket;
        }

        /**
         * Dispatches the new connection based on connection type, such
         * as Gnutella, HTTP, or MAGNET.
         */
        public void run() {
			ConnectionManager cm = RouterService.getConnectionManager();
			UploadManager um     = RouterService.getUploadManager();
			DownloadManager dm   = RouterService.getDownloadManager();
            try {
                //The try-catch below is a work-around for JDK bug 4091706.
                InputStream in=null;
                try {
                    in=_socket.getInputStream(); 
                } catch (Exception e) {
                    throw new IOException("could not access socket stream");
                }
                _socket.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word = IOUtils.readWord(in,8);
                _socket.setSoTimeout(0);

				// Only selectively allow localhost connections
				if ( !word.equals("MAGNET") ) {
					InetAddress address = _socket.getInetAddress();
					byte[] addressBytes = address.getAddress();
					if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() &&
					    (addressBytes[0] == 127)) {
						_socket.close();
						return;
					}
				}

                //1. Gnutella connection.  If the user hasn't changed the
                //   handshake string, we accept the default ("GNUTELLA 
                //   CONNECT/0.4") or the proprietary limewire string
                //   ("LIMEWIRE CONNECT/0.4").  Otherwise we just accept
                //   the user's value.
                boolean useDefaultConnect=
                    SettingsManager.instance().getConnectString().equals(
                         SettingsManager.DEFAULT_CONNECT_STRING);


                if (word.equals(SettingsManager.instance().
                        getConnectStringFirstWord())) {
                    cm.acceptConnection(_socket);
                }
                else if (useDefaultConnect && word.equals("LIMEWIRE")) {
                    cm.acceptConnection(_socket);
                }
                //2. Incoming upload via HTTP
                else if (word.equals("GET")) {
					um.acceptUpload(HTTPRequestMethod.GET, _socket, false);
					if(!CommonUtils.isJava118()) {
						HTTPStat.HTTP_GET_REQUESTS.incrementStat();
					}
                }
				else if (word.equals("HEAD")) {
					um.acceptUpload(HTTPRequestMethod.HEAD, _socket, false);
					if(!CommonUtils.isJava118()) {
						HTTPStat.HTTP_HEAD_REQUESTS.incrementStat();
					}
				}
                //3. Incoming download via push/HTTP.
                else if (word.equals("GIV")) {
                    dm.acceptDownload(_socket);
                }
				else if (word.equals("CHAT")) {
					ChatManager.instance().accept(_socket);
				}
			    else if (word.equals("MAGNET")) {
                    ExternalControl.fireMagnet(_socket);
                }	
                //4. Unknown protocol
                else {
                    throw new IOException("UNKOWN PROTOCOL: "+word);
                }
            } catch (IOException e) {
                //handshake failed: try to close connection.
                try { _socket.close(); } catch (IOException e2) { }
            } catch(Throwable e) {
				ErrorService.error(e);
			}
        }
    }

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(String ip) {        
        return !IPFilter.instance().allow(ip);
    }

    
    private static final boolean debug = false;
    private static void debug(String out) {
        if (debug)
            System.out.println(out);
    }


}
