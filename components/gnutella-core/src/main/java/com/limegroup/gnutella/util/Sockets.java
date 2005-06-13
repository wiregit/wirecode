package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.LinkedList;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.io.ConnectObserver;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Factory for creating Sockets.
 */
public class Sockets {
    
    private static final Log LOG = LogFactory.getLog(Sockets.class);
    
    /**
     * The maximum number of concurrent connection attempts.
     */
    private static final int MAX_CONNECTING_SOCKETS = 8;
    
    /**
     * The current number of waiting socket attempts.
     */
    private static int _socketsConnecting = 0;
    
    /**
     * Any non-blocking Requestors waiting on a pending socket.
     */
    private static final List WAITING_REQUESTS = new LinkedList();
    
	/**
	 * Ensure this cannot be constructed.
	 */
	private Sockets() {}

    /**
     * Connects and returns a socket to the given host, with a timeout.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In case of a proxy connection, this timeout
	 *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(String host, int port, int timeout) throws IOException {
        return connect(host, port, timeout, null);
    }
    
    /**
     * Sets up a socket for connecting.
     * This method may either block or return immediately, depending on if
     * if observer is null or not.
     *
     * If observer is non-null, this returns immediately.  This may either return
     * a connected or unconnected Socket, depending on if a connection was able to
     * be established immediately.  The ConnectObserver will always be notified of
     * success via handleConnect(Socket), and failure via shutdown().  If the connection
     * was established immediately, it is possible that handleConnect(Socket) is called
     * before this method returns.
     *
     * If observer is null, this method blocks until a connection can be established. 
     * If no connection can be established, an IOException is thrown.
     *
     * @param host the address of the host to connect to
     * @param port the port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
	 *  or 0 for no timeout. In case of a proxy connection, this timeout
	 *  might be exceeded
     * @return the Socket (connected or unconnected)
     * @throws IOException see above
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(String host, int port, int timeout, ConnectObserver observer) throws IOException {
        if(!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("port out of range: "+port);

		InetAddress address = InetAddress.getByName(host);
		InetSocketAddress addr = new InetSocketAddress(address, port);
		int proxyType = ProxyUtils.getProxyType(address);
		
		if (proxyType != ConnectionSettings.C_NO_PROXY)
            return connectProxy(proxyType, addr, timeout, observer);
        else
            return connectPlain(addr, timeout, observer);
	}

	/** 
	 * Establishes a connection to the given host.
	 *
	 * If observer is null, this will block until a connection is established or an IOException is thrown.
	 * Otherwise, this will return immediately and the Observer will be notified of success or failure.
	 */
	private static Socket connectPlain(InetSocketAddress addr, int timeout, ConnectObserver observer)
		throws IOException {
        
        // needs to be declared as an NIOSocket for the non-blocking connect.
        NIOSocket socket = new NIOSocket();
        
        if(observer == null) {
            // BLOCKING.
            waitForSocket();
            try {
                socket.connect(addr, timeout);
            } finally {
                releaseSocket();
            }
        } else {
            // NON BLOCKING
            if(addWaitingSocket(socket, addr, timeout, observer)) {
                socket.connect(addr, timeout, new DelegateConnector(observer));
            }
        }
        
        return socket;
    }
    
    /**
     * Connects to a host using a proxy.
     */
    private static Socket connectProxy(int type, InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
		InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
		
		if(observer != null) {
		    return connectPlain(proxyAddr, timeout, new ProxyUtils.ProxyConnector(type, observer, addr, timeout));
		} else {
		    Socket proxySocket = connectPlain(proxyAddr, timeout, null);
		    return ProxyUtils.establishProxy(type, proxySocket, addr, timeout);
		}
    }

    
    /**
     * Runs through any waiting Requestors and initiates a connection to them.
     */
    private static void runWaitingRequests() {
        while(_socketsConnecting < MAX_CONNECTING_SOCKETS && !WAITING_REQUESTS.isEmpty()) {
            Requestor next = (Requestor)WAITING_REQUESTS.remove(0);
            _socketsConnecting++;
            next.socket.connect(next.addr, next.timeout, new DelegateConnector(next.observer));
        }
    }       
	
	/**
	 * Determines if the given requestor can immediately connect.
	 * If not, adds it to a pool of future connection-wanters.
	 */
	private static boolean addWaitingSocket(NIOSocket socket, InetSocketAddress addr,
	                                        int timeout, ConnectObserver observer) {
	    if(!CommonUtils.isWindowsXP())
	        return true;
	        
	    synchronized(Sockets.class) {
	        if(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
	            WAITING_REQUESTS.add(new Requestor(socket, addr, timeout, observer));
	            return false;
	        } else {
	            _socketsConnecting++;
	            return true;
	        }
	    }
	}
	
	/**
	 * Waits until we're allowed to do an active outgoing socket connection.
	 *
	 * Non-blocking requests will get priority over blocking requests.
	 */
	private static void waitForSocket() throws IOException {
	    if(!CommonUtils.isWindowsXP())
	        return;
	        
	    synchronized(Sockets.class) {
	        while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
	            try {
	                Sockets.class.wait();
	            } catch(InterruptedException ignored) {
	                throw new IOException(ignored.getMessage());
	            }
	        }
	        _socketsConnecting++;	        
	    }
	}
	
	/**
	 * Notification that a socket has been released.
	 *
	 * If there are waiting non-blocking requests, spawns starts a new connection for them.
	 */
	private static void releaseSocket() {
	    if(!CommonUtils.isWindowsXP())
	        return;
	        
	    synchronized(Sockets.class) {
	        _socketsConnecting--;
	        if(_socketsConnecting < MAX_CONNECTING_SOCKETS) {
	            runWaitingRequests();
	            Sockets.class.notifyAll();
            }
	    }
	}
	
	/** Simple struct to hold data for non-blocking waiting requests. */
	private static class Requestor {
	    private final InetSocketAddress addr;
	    private final int timeout;
	    private final NIOSocket socket;
	    private final ConnectObserver observer;
	    Requestor(NIOSocket socket, InetSocketAddress addr, int timeout, ConnectObserver observer) {
	        this.socket = socket;
	        this.addr = addr;
	        this.timeout = timeout;
	        this.observer = observer;
	    }
	}
	
	/** A ConnectObserver to maintain the _socketsConnecting variable. */
	private static class DelegateConnector implements ConnectObserver {
	    private final ConnectObserver delegate;
	    DelegateConnector(ConnectObserver observer) {
	        delegate = observer;
	    }
	    
	    public void handleConnect(Socket s) throws IOException {
            releaseSocket();
            delegate.handleConnect(s);
        }
        
        public void shutdown()  {
            releaseSocket();
            delegate.shutdown();
        }
        
        // unused.
        public void handleIOException(IOException x) {}
    }  
}
