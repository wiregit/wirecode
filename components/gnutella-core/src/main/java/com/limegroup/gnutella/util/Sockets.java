package com.limegroup.gnutella.util;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.io.ConnectObserver;

/**
 * Factory for creating Sockets.
 */
public class Sockets {
    
    private final static SocketController CONTROLLER =
        OSUtils.isWindowsXP() ?
                new LimitedSocketController(4) :
                new SimpleSocketController();
        
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
        InetAddress address = InetAddress.getByName(host);  
        InetSocketAddress addr = new InetSocketAddress(address, port);  
        return connect(addr, timeout, observer);
    }
    
    public static Socket connect(IpPort ipport, int timeout, ConnectObserver observer) throws IOException {  
        InetSocketAddress addr = new InetSocketAddress(ipport.getInetAddress(), ipport.getPort());  
        return connect(addr, timeout, observer);
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
     * @param addr address/port
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout. In case of a proxy connection, this timeout
     *  might be exceeded
     * @param observer the ConnectObserver to notify about non-blocking connect events
     * @return the Socket (connected or unconnected)
     * @throws IOException see above
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException {
        if(!NetworkUtils.isValidPort(addr.getPort()))  
            throw new IllegalArgumentException("port out of range: "+addr.getPort());
        
        return CONTROLLER.connect(addr, timeout, observer);
	}
    
    /**
     * Removes the given ConnectObserver from wanting to make a request.
     * This returns true if it was able to remove the observer because the request had
     * not been processed yet.
     * Otherwise it returns false, and the ConnectObserver should expect some sort of callback
     * indicating whether or not the connect succeeded.
     */
    public static boolean removeConnectObserver(ConnectObserver observer) {
        return CONTROLLER.removeConnectObserver(observer);
    }	

    /** Returns the number of Sockets allowed to be created concurrently. */
	public static int getNumAllowedSockets() {
        return CONTROLLER.getNumAllowedSockets();
	}
    
    /** Returns the number of Sockets that are waiting for the controller to process them. */
    public static int getNumWaitingSockets() {
        return CONTROLLER.getNumWaitingSockets();
    }
}
