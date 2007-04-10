package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

import org.limewire.nio.observer.AcceptObserver;

/** A ServerSocketFactory for use with constructing non-blocking ServerSockets. */
public abstract class NBServerSocketFactory extends ServerSocketFactory {
    
    /**
     * Constructs a new, unconnected ServerSocket that will notify the given
     * AcceptObserver when new connections arive.  You must call 'bind' on the
     * socket to begin accepting new connections.
     * 
     * @param observer
     * @return
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new ServerSocket bound at the given port.
     * The given observer will be notified when new incoming connections are accepted.
     * 
     * @param port
     * @param observer
     * @return
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new ServerSocket bound at the given port, using the given backlog.
     * The given AcceptObserver will be notified when new incoming connections are accepted.
     * 
     * @param port
     * @param backlog
     * @param observer
     * @return
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, AcceptObserver observer) throws IOException;
    
    /**
     * Constructs a new ServerSocket bound at the given port and given address, using the given backlog.
     * The given AcceptObserver will be notified when new incoming connections are accepted.
     * 
     * @param port
     * @param backlog
     * @param bindAddr
     * @param observer
     * @return
     * @throws IOException
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException;
}
