package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.browser.ExternalControl;


/**
 * Listens on ports, accepts incoming connections, and dispatches threads to
 * handle those connections.  Currently supports Gnutella messaging, HTTP, and
 * chat connections over TCP; more may be supported in the
 * future.<p> 
 */
public class Acceptor extends Thread {
    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     *
     * LOCKING: obtain _socketLock before modifying either.  Notify _socketLock
     * when done.
     */
    private volatile ServerSocket _socket=null;
    private int _port=0;
    private Object _socketLock=new Object();

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
    private static byte[] _address=new byte[4];

    private IPFilter _filter=new IPFilter();;
    
    private ConnectionManager _connectionManager;
    private DownloadManager _downloadManager;
    private UploadManager _uploadManager;
    private MessageRouter _router;
    private ActivityCallback _callback;

	private volatile boolean _acceptedIncoming = false;


	/**
     * @modifes this
     * @effects sets the IP address to use in pongs and query replies.  If addr
     *  is localhost (127.0.0.1), this is not modified.  This method must be
	 *  to get around JDK bug #4073539, as well as to try to handle the case 
	 *  of a computer whose IP address keeps changing.
	 */
	public static synchronized void setAddress(byte[] addr) {
        //Ignore localhost.
        if (addr[0]==(byte)127)
            return;

	    _address = addr; 
	}

    

    /**
     * Creates an acceptor that tries to listen to incoming connections
     * on the given port.  If this is a bad port, the port will be
     * changed when run is called and SettingsManager will be updated.
     * If that fails, ActivityCallback.error will be called.  A port
     * of 0 means do not accept incoming connections.
     */
    public Acceptor(int port, ActivityCallback callback) {
        _port = port;
        _callback = callback;
    }

    /**
     * Links the HostCatcher up with the other back end pieces and launches
     * the port monitoring thread
     */
    public void initialize(ConnectionManager connectionManager,
                           MessageRouter router,
                           DownloadManager downloadManager,
                           UploadManager uploadManager) {
        _connectionManager = connectionManager;
        _router = router;
        _downloadManager = downloadManager;
        _uploadManager = uploadManager;
		
        setDaemon(true);
        start();
    }

    /**
     * Returns this' address to use for ping replies, query replies,
     * and pushes.
     */
    public byte[] getAddress() {
		if(SettingsManager.instance().getForceIPAddress())
			return SettingsManager.instance().getForcedIPAddress();
        synchronized (Acceptor.class) {
            return _address;
        }
    }

    /**
     * Returns the port at which the Connection Manager listens for incoming
     * connections
     * @return the listening port
     */
    public int getPort() {
		if(SettingsManager.instance().getForceIPAddress())
			return SettingsManager.instance().getForcedPort();
        return _port;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening.
     *  If that fails, this is <i>not</i> modified and IOException is thrown.
     *  If port==0, tells this to stop listening to incoming connections.
     *  This is properly synchronized and can be called even while run() is
     *  being called.
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
            //Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            synchronized (_socketLock) {
                _socket=null;
                _port=0;
                _socketLock.notify();
            }
            return;
        }
        //3. Normal case.  See note about locking above.
        else {
            //a) Try new port.
            ServerSocket newSocket=null;
            try {
                newSocket=new ServerSocket(port);
            } catch (IOException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                throw new IOException();
            }
            //b) Close old socket (if non-null)
            if (_socket!=null) {
                try {
                    _socket.close();
                } catch (IOException e) { }
            }
            //c) Replace with new sock.  Notify the accept thread.
            synchronized (_socketLock) {
                _socket=newSocket;
                _port=port;
                _socketLock.notify();
            }
            return;
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
        //0. Get local address.  This must be done here--not in the static
        //   initializer--because it can block under certain conditions.
        //   See the notes for _address.
        try {
            setAddress(InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
        } catch (SecurityException e) {
        }

        // Create the server socket, bind it to a port, and listen for
        // incoming connections.  If there are problems, we can continue
        // onward.
        //1. Try suggested port.
        int oldPort = _port;
        try {
            setListeningPort(_port);
        } catch (IOException e) {
            //2. Try 10 different ports
            for (int i=0; i<10; i++) {
                _port=i+6346;
                try {
                    setListeningPort(_port);
                    break;
                } catch (IOException e2) { }
            }

            // If we still don't have a socket, there's an error
            if(_socket == null)
                _callback.error(ActivityCallback.PORT_ERROR);
        }

        if (_port!=oldPort) {
            SettingsManager.instance().setPort(_port);
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
                synchronized (_socketLock) {
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
                            _socketLock.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }

                //Check if IP address of the incoming socket is in _badHosts
                if (isBannedIP(
                        client.getInetAddress().getHostAddress())) {
                    client.close();
                    continue;
                }

				// we have accepted an incoming socket.
				_acceptedIncoming = true;
				SettingsManager.instance().setAcceptedIncoming(_acceptedIncoming);

                //Dispatch asynchronously.
                new ConnectionDispatchRunner(client);

            } catch (SecurityException e) {
                _callback.error(ActivityCallback.SOCKET_ERROR);
                return;
            } catch (Exception e) {
                //Internal error!
                _callback.error(ActivityCallback.INTERNAL_ERROR, e);
            }
        }
    }

    private class ConnectionDispatchRunner extends Thread {
        private Socket _socket;

        /**
         * @modifies socket, this' managers
         * @effects starts a new thread to handle the given socket and
         *  registers it with the appropriate protocol-specific manager.
         *  Returns once the thread has been started.  If socket does
         *  not speak a known protocol, closes the socket immediately and
         *  returns.
         */
        public ConnectionDispatchRunner(Socket socket) {
            super("ConnectionDispatchRunner");
            _socket=socket;
            setDaemon(true);
            start();
        }

        public void run() {
            try {
                //The try-catch below is a work-around for JDK bug 4091706.
                InputStream in=null;
                try {
                    in=_socket.getInputStream(); 
                } catch (Exception e) {
                    throw new IOException();
                }
                _socket.setSoTimeout(SettingsManager.instance().getTimeout());
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(in,8);
                _socket.setSoTimeout(0);

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
                    _connectionManager.acceptConnection(_socket);
                }
                else if (useDefaultConnect && word.equals("LIMEWIRE")) {
                    _connectionManager.acceptConnection(_socket);
                }
                //2. Incoming upload via HTTP
                else if (word.equals("GET")) {
					_uploadManager.acceptUpload(HTTPRequestMethod.GET, _socket);
                }
				else if (word.equals("HEAD")) {
					_uploadManager.acceptUpload(HTTPRequestMethod.HEAD, _socket);
				}
                //3. Incoming download via push/HTTP.
                else if (word.equals("GIV")) {
                    _downloadManager.acceptDownload(_socket);
                }
				else if (word.equals("CHAT")) {
					ChatManager.instance().accept(_socket);
				}
			    else if (word.equals("MAGNET")) {
                    ExternalControl.fireMagnet(_socket);
                }	
                //4. Unknown protocol
                else {
                    throw new IOException();
                }
            } catch (IOException e) {
                //handshake failed: try to close connection.
                try { _socket.close(); } catch (IOException e2) { }
            } catch(Exception e) {
				_callback.error(ActivityCallback.INTERNAL_ERROR, e);
			}
        }
    }


    /** 
     * Updates this IP filter.  Added to fix bug where banned IP added is not
     * effective until restart (Bug 62001).  Allows a new host to be dynamically
     * added to the Banned IPs list (via a reload from SettingsManager).  
     */
    public void refreshBannedIPs() {
        _filter=new IPFilter();
    }

    /**
     * Returns whether <tt>ip</tt> is a banned address.
     * @param ip an address in resolved dotted-quad format, e.g., 18.239.0.144
     * @return true iff ip is a banned address.
     */
    public boolean isBannedIP(String ip) {        
        return !_filter.allow(ip);
    }


}
