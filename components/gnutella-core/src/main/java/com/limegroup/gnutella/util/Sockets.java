package com.limegroup.gnutella.util;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.limewire.io.NetworkUtils;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.NIOSocketFactory;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.ssl.TLSSocketFactory;
import org.limewire.util.OSUtils;


/** Factory for creating Sockets. */
public class Sockets {
    
    private final static SocketController CONTROLLER =
        OSUtils.isSocketChallengedWindows() ?
                new LimitedSocketController(4) :
                new SimpleSocketController();
                
    /** The different ways a connection can be attempted. */
    public static enum ConnectType {    
        PLAIN(new NIOSocketFactory()), TLS(new TLSSocketFactory());
        
        private final NBSocketFactory factory;
        
        ConnectType(NBSocketFactory factory) {
            this.factory = factory;
        }
        
        private NBSocketFactory getFactory() {
            return factory;
        }
    }
        
	/**
	 * Ensure this cannot be constructed.
	 */
	private Sockets() {}
    
    /**
     * Connects and returns a socket to the given host, with a timeout.
     * The timeout only applies to network conditions.  More time might be spent
     * waiting for an available slot to connect with.
     *
     * @param addr the host/port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout. In case of a proxy connection, this timeout
     *  might be exceeded
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(InetSocketAddress addr, int timeout) throws IOException {
        return connect(addr, timeout, ConnectType.PLAIN);
    }
    
    /**
     * Connects and returns a socket to the given host, with a timeout.
     * The timeout only applies to network conditions.  More time might be spent
     * waiting for an available slot to connect with.
     * 
     * The connection will be attempted with the specified connection type.
     * For example, to make a plain socket, use ConnectType.PLAIN.
     * To connect with a TLS Socket, use ConnectType.TLS.
     *
     * @param addr the host/port to connect to
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout. In case of a proxy connection, this timeout
     *  might be exceeded
     * @param type the type of connection to attempt
     * @return the connected Socket
     * @throws IOException the connections couldn't be made in the 
     *  requested time
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(InetSocketAddress addr, int timeout, ConnectType type) throws IOException {
        return connect(addr, timeout, null, type);
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
        return connect(addr, timeout, observer, ConnectType.PLAIN);
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
     * The ConnectType determines the kind of connection that is attempted.
     * For example, ConnectType.PLAIN will create a plaintext socket, whereas
     * ConnectType.TLS will create a TLS socket.
     *
     * @param addr address/port
     * @param timeout the desired timeout for connecting, in milliseconds,
     *  or 0 for no timeout. In case of a proxy connection, this timeout
     *  might be exceeded
     * @param observer the ConnectObserver to notify about non-blocking connect events
     * @param type the type of connection to attempt
     * @return the Socket (connected or unconnected)
     * @throws IOException see above
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    public static Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer, ConnectType type) throws IOException {
        if(!NetworkUtils.isValidPort(addr.getPort()))  
            throw new IllegalArgumentException("port out of range: "+addr.getPort());
        if(addr.isUnresolved())
            throw new IOException("address must be resolved!");
        
        return CONTROLLER.connect(type.getFactory(), addr, timeout, observer);
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
