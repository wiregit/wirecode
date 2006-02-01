package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
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
     * @param observer the ConnectObserver to notify about non-blocking connect events
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
     * Removes the given ConnectObserver from wanting to make a request.
     * This returns true if it was able to remove the observer because the request had
     * not been processed yet.
     * Otherwise it returns false, and the ConnectObserver should expect some sort of callback
     * indicating whether or not the connect succeeded.
     */
    public static boolean removeConnectObserver(ConnectObserver observer) {
        synchronized(Sockets.class) {
            for(Iterator i = WAITING_REQUESTS.iterator(); i.hasNext(); ) {
                Requestor next = (Requestor)i.next();
                if(next.observer == observer) {
                    i.remove();
                    return true;
                // must handle proxy'd kinds also.
                } else if(next.observer instanceof ProxyUtils.ProxyConnector) {
                    if(((ProxyUtils.ProxyConnector)next.observer).getDelegateObserver() == observer) {
                        i.remove();
                        return true;
                    }
                }
            }
        }
        return false;
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
    private static Socket connectProxy(int type, InetSocketAddress addr, int timeout, ConnectObserver observer)
      throws IOException {
		String proxyHost = ConnectionSettings.PROXY_HOST.getValue();
		int proxyPort = ConnectionSettings.PROXY_PORT.getValue();
		InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
		
		if(observer != null) {
		    return connectPlain(proxyAddr, timeout, new ProxyUtils.ProxyConnector(type, observer, addr, timeout));
		} else {
		    Socket proxySocket = connectPlain(proxyAddr, timeout, null);
            try {
                return ProxyUtils.establishProxy(type, proxySocket, addr, timeout);
            } catch(IOException iox) {
                // Ensure the proxySocket is closed.  Not all proxies cleanup correctly.
                try { proxySocket.close(); } catch(IOException ignored) {}
                throw iox;
            }
		}
    }
    
    /**
     * Runs through any waiting Requestors and initiates a connection to them.
     */
    private static void runWaitingRequests() {
        // We must connect outside of the lock, so as not to expose being locked to external
        // entities.
        List toBeProcessed = new ArrayList(Math.min(WAITING_REQUESTS.size(), Math.max(0, MAX_CONNECTING_SOCKETS - _socketsConnecting)));
        synchronized(Sockets.class) {
            while(_socketsConnecting < MAX_CONNECTING_SOCKETS && !WAITING_REQUESTS.isEmpty()) {
                toBeProcessed.add(WAITING_REQUESTS.remove(0));
                _socketsConnecting++;
            }
        }
        
        for(int i = 0; i < toBeProcessed.size(); i++) {
            Requestor next = (Requestor)toBeProcessed.get(i);
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
	    
        // Release this slot.
	    synchronized(Sockets.class) {
	        _socketsConnecting--;
        }
        
        // See if any non-blocking requests are queued.
        runWaitingRequests();
        
        // If there's room, notify blocking requests.
        synchronized(Sockets.class) {
	        if(_socketsConnecting < MAX_CONNECTING_SOCKETS) {
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
