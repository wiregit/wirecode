package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

/**
 * Abstract class that allows a Socket to provide
 * a connect method that takes a ConnectObserver.
 */
public abstract class NBSocket extends Socket {
    /**
     * Connects to the specified address within the given timeout (in milliseconds).
     * The given ConnectObserver will be notified of success or failure.
     * In the event of success, observer.handleConnect is called.  In a failure,
     * observer.shutdown is called.  observer.handleIOException is never called.
     *
     * Returns true if this was able to connect immediately.  The observer is still
     * notified about the success even it it was immediate.
     * Returns false if it was unable to connect immediately.  The observer will
     * receive the connection events.
     * 
     * This method always returns immediately.
     * 
     * @throws IOException if the address cannot be resolved.
     */
    public abstract boolean connect(SocketAddress addr, int timeout, ConnectObserver observer) throws IOException;
    
    // a bunch of Constructors.
    
    public NBSocket() {
        super();
    }
    
    public NBSocket(InetAddress addr, int port) throws IOException {
        super(addr, port);
    }
    
    public NBSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }
    
    public NBSocket(String addr, int port) throws UnknownHostException, IOException {
        super(addr, port);
    }
    
    public NBSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        super(addr, port, localAddr, localPort);
    }    
}
