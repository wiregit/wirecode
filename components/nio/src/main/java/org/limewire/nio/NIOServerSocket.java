package org.limewire.nio;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.observer.AcceptChannelObserver;
import org.limewire.nio.observer.AcceptObserver;

/**
 * A ServerSocket that does all of its accepting using NIO, but psuedo-blocks.
 */
public class NIOServerSocket extends ServerSocket implements AcceptChannelObserver {
    
    private static final Log LOG = LogFactory.getLog(NIOServerSocket.class);
    
    /** Channel backing this NIOServerSocket. */
    private final ServerSocketChannel channel;
    /** Socket associate of the channel */
    private final ServerSocket socket;
    /** AcceptObserver that will be retrieving the sockets. */
    private final AcceptObserver observer;
    
    /**
     * Constructs a new, unbound, NIOServerSocket.
     * You must call 'bind' to start listening for incoming connections.
     */
    public NIOServerSocket() throws IOException {
        this(null);
    }
    
    /**
     * Constructs a new, unbound, NIOServerSocket.
     * You must call 'bind' to start listening for incoming connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public NIOServerSocket(AcceptObserver observer) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        if(observer == null)
            this.observer = new BlockingObserver();
        else
            this.observer = observer;
    }
    
    /** Constructs a new NIOServerSocket bound to the given port */
    public NIOServerSocket(int port) throws IOException {
        this(port, null);
    }

    /** 
     * Constructs a new NIOServerSocket bound to the given port 
     * All accepted connections will be routed to the given AcceptObserver
     */
    public NIOServerSocket(int port, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port, able to accept
     * the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, (AcceptObserver)null);
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port, able to accept
     * the given backlog of connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public NIOServerSocket(int port, int backlog, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port), backlog);
        
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port & addr, able to accept
     * the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(port, backlog, bindAddr, null);
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port & addr, able to accept
     * the given backlog of connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }
    
    /**
     * Initializes the connection.
     * Currently this sets the channel to blocking & reuse addr to true.
     */
    private void init() throws IOException {
        channel.configureBlocking(false);
        socket.setReuseAddress(true);
    }

    /**
     * Accepts an incoming connection.
     * THIS CAN ONLY BE USED IF NO AcceptObserver WAS PROVIDED WHEN CONSTRUCTING
     * THIS NIOServerSocket.  All other attempts will cause an immediate RuntimeException.
     */
    public Socket accept() throws IOException {
        if(observer instanceof BlockingObserver)
            return ((BlockingObserver)observer).accept();
        else
            throw new IllegalBlockingModeException(); 
    }
    
    /**
     * Notification that a socket has been accepted.
     */
    public void handleAcceptChannel(SocketChannel channel) throws IOException {
        observer.handleAccept(createClientSocket(channel.socket()));
    }
    
    /**
     * Notification that an IOException occurred while accepting.
     */
    public void handleIOException(IOException iox) {
        observer.handleIOException(iox);
    }
    
    /**
     * Closes this socket. 
     */
    public void shutdown() {
        try {
            close();
        } catch(IOException ignored) {}
    }
    
    /** Binds the socket to the endpoint & starts listening for incoming connections */
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
        NIODispatcher.instance().registerAccept(channel, this);
    }
     
    /** Binds the socket to the endpoint & starts listening for incoming connections */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        socket.bind(endpoint, backlog);
        NIODispatcher.instance().registerAccept(channel, this);
    }
    
    /** Shuts down this NIOServerSocket */
    public void close() throws IOException {
        IOException exception = null;
        try {
            socket.close();
        } catch(IOException iox) {
            exception = iox;
        }
        
        observer.shutdown();
        
        if(exception != null)
            throw exception;
    }
    
    /** Wraps the accepted Socket in a delegating socket. */
    protected Socket createClientSocket(Socket socket) {
        return new NIOSocket(socket);
    }


    /////////////////////////////////////////////////////////////
    /////////// Below are simple wrappers for the socket.
    /////////////////////////////////////////////////////////////    

    public ServerSocketChannel getChannel() {
        return socket.getChannel();
    }
 
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
    
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    public int getSoTimeout() throws IOException {
        return socket.getSoTimeout();
    }
    
    public boolean isBound() {
        return socket.isBound();
    }
    
    public boolean isClosed() {
        return socket.isClosed();
    }
    
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
    
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }
    
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    public String toString() {
        return "NIOServerSocket::" + socket.toString();
    }
    
    /**
     * An AcceptObserver that stores up connections for use with blocking accepts.
     */
    private class BlockingObserver implements AcceptObserver {
        /** List of all pending sockets that can be accepted. */
        private final List<Socket> pendingSockets = new LinkedList<Socket>();
        /** An exception that was stored and should be thrown during the next accept */
        private IOException storedException = null;
        /** Lock to be used for synchronizing access to pendingSockets. */
        private final Object LOCK = new Object();
        
        /**
         * Gets the next socket that was accepted, or throws IOException if an
         * exception occurred in this ServerSocket.
         */
        public Socket accept() throws IOException {
            synchronized (LOCK) {
                boolean looped = false;
                int timeout = getSoTimeout();
                while (!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                    if (looped && timeout != 0)
                        throw new SocketTimeoutException("accept timed out: " + timeout);

                    LOG.debug("Waiting for incoming socket...");
                    try {
                        LOCK.wait(timeout);
                    } catch (InterruptedException ix) {
                        throw new InterruptedIOException(ix);
                    }
                    looped = true;
                }

                IOException x = storedException;
                storedException = null;

                if (isClosed())
                    throw new SocketException("Socket Closed");
                else if (x != null)
                    throw x;
                else if (!isBound())
                    throw new SocketException("Not Bound!");
                else {
                    LOG.debug("Retrieved a socket!");
                    return pendingSockets.remove(0);
                }
            }
        }        

        /** Stores up the next Socket for use with accept. */
        public void handleAccept(Socket socket) throws IOException {
            synchronized (LOCK) {
                pendingSockets.add(socket);
                LOCK.notify();
            }
        }

        /** Notification an exception occurred. */
        public void handleIOException(IOException iox) {
            synchronized(LOCK) {
                storedException = iox;
                LOCK.notify();
            }
        }

        /** Notification that the socket was shutdown. */
        public void shutdown() {
            synchronized(LOCK) {
                // Shutdown all sockets it created.
                for(Socket next : pendingSockets) {
                    try {
                        next.close(); 
                    } catch(IOException ignored) {}
                }
                pendingSockets.clear();
                
                LOCK.notify();
            }
        }
    }
    
}