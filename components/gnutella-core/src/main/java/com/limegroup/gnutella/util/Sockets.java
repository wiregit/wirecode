package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.io.ConnectObserver;
import com.limegroup.gnutella.io.NIOSocket;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Factory for creating Sockets.
 */
public class Sockets {
    
    private static final Log LOG = LogFactory.getLog(Sockets.class);
    
    /**
     * The maximum number of concurrent connection attempts.
     */
    private static final int MAX_CONNECTING_SOCKETS = 4;
    
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
     * The timeout only applies to network conditions.  More time might be spent
     * waiting for an available slot to connect with.
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
        return connect(host, port, timeout, null, false);
    }
    
    /**
     * Connects and returns a socket to the given host, with a timeout.
     * Any time spent waiting for available socket is counted towards the timeout.
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
    public static Socket connectHardTimeout(String host, int port, int timeout)  throws IOException {
      return connect(host, port, timeout, null, true);
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
     * @param observer the ConnectObserver to notify about non-blocking connect events
     * @return the Socket (connected or unconnected)
     * @throws IOException see above
	 * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(String host, int port, int timeout, ConnectObserver observer) throws IOException {
        return connect(host, port, timeout, observer, false);
    }
    
    /**
     * Same as above, except with an extra parameter for 'hard' timeouts.
     * A 'hard' timeout cannot be used if observer is non-null.
     * If a hard timeout can be used and hard is true, this will only block for a total time of 'timeout'.
     * Otherwise, the 'timeout' only applies to the network value -- more time may be spent waiting internally
     * until a slot for connecting is available.
     */
    private static Socket connect(String host, int port, int timeout, ConnectObserver observer, boolean hard) throws IOException {        
        if(!NetworkUtils.isValidPort(port))  
            throw new IllegalArgumentException("port out of range: "+port);  
  
        InetAddress address = InetAddress.getByName(host);  
        InetSocketAddress addr = new InetSocketAddress(address, port);  
        int proxyType = ProxyUtils.getProxyType(address);  
                       
        if (proxyType != ConnectionSettings.C_NO_PROXY)  
            return connectProxy(proxyType, addr, timeout, observer, hard);  
        else
            return connectPlain(addr, timeout, observer, hard);  
	}

    /** 
     * Establishes a connection to the given host.
     *
     * If observer is null, this will block until a connection is established or an IOException is thrown.
     * Otherwise, this will return immediately and the Observer will be notified of success or failure.
     */
    private static Socket connectPlain(InetSocketAddress addr, int timeout, ConnectObserver observer, boolean hard)
        throws IOException {
        
        // needs to be declared as an NIOSocket for the non-blocking connect.
        NIOSocket socket = new NIOSocket();
        
        if(observer == null) {
            // BLOCKING.
            
            if(hard) {
                long waitTime = System.currentTimeMillis();
                boolean waited = waitForSocketHard(timeout, waitTime);
                if (waited) {
                    waitTime = System.currentTimeMillis() - waitTime;
                    timeout -= waitTime;
                    if (timeout <= 0)
                        throw new IOException("timed out");
                }
            } else {
                waitForSocket();
            }
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
    private static Socket connectProxy(int type, InetSocketAddress addr, int timeout, ConnectObserver observer, boolean hard)
      throws IOException {
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
		InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
		
		if(observer != null) {
		    return connectPlain(proxyAddr, timeout, new ProxyUtils.ProxyConnector(type, observer, addr, timeout), false);
		} else {
		    Socket proxySocket = connectPlain(proxyAddr, timeout, null, hard);
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
            try {
                next.socket.connect(next.addr, next.timeout, new DelegateConnector(next.observer));
            } catch(IOException iox) {
                _socketsConnecting--;
                next.observer.shutdown();
            }
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
     * Waits until we're allowed to do an active outgoing socket
     * connection with a timeout
     * @return true if we had to wait before we could get a connection
     */
    private static boolean waitForSocketHard(int timeout, long now) throws IOException {
        if(!CommonUtils.isWindowsXP())
            return false;
        
        long timeoutTime = now + timeout;
        boolean ret = false;
        synchronized(Sockets.class) {
            while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
                
                if (timeout <= 0)
                    throw new IOException("timed out :(");
                
                try {
                    ret = true;
                    Sockets.class.wait(timeout);
                    timeout = (int)(timeoutTime - System.currentTimeMillis());
                } catch(InterruptedException ignored) {
                    throw new IOException(ignored.getMessage());
                }
            }
            _socketsConnecting++;           
        }
        
        return ret;
    }
	
	/**
	 * Waits until we're allowed to do an active outgoing socket
	 * connection.
	 */
	private static void waitForSocket() throws IOException {
		if (!CommonUtils.isWindowsXP())
			return;
		
		synchronized(Sockets.class) {
			while(_socketsConnecting >= MAX_CONNECTING_SOCKETS) {
				try {
					Sockets.class.wait();
				} catch (InterruptedException ix) {
					throw new IOException(ix.getMessage());
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
	

	public static int getNumAllowedSockets() {
		if (CommonUtils.isWindowsXP())
			return MAX_CONNECTING_SOCKETS;
		else
			return Integer.MAX_VALUE; // unlimited
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
