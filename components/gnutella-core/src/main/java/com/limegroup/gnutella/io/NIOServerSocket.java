pbckage com.limegroup.gnutella.io;


import jbva.io.IOException;
import jbva.nio.channels.ServerSocketChannel;
import jbva.nio.channels.SocketChannel;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.InetSocketAddress;
import jbva.net.SocketException;
import jbva.net.SocketTimeoutException;
import jbva.net.SocketAddress;
import jbva.net.ServerSocket;

import jbva.util.List;
import jbva.util.LinkedList;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A ServerSocket thbt does all of its accepting using NIO, but psuedo-blocks.
 */
public clbss NIOServerSocket extends ServerSocket implements AcceptObserver {
    
    privbte static final Log LOG = LogFactory.getLog(NIOServerSocket.class);
    
    privbte final ServerSocketChannel channel;
    privbte final ServerSocket socket;
    
    privbte final List pendingSockets = new LinkedList();
    privbte IOException storedException = null;
    
    privbte final Object LOCK = new Object();
    
    /**
     * Constructs b new, unbound, NIOServerSocket.
     * You must cbll 'bind' to start listening for incoming connections.
     */
    public NIOServerSocket() throws IOException {
        chbnnel = ServerSocketChannel.open();
        socket = chbnnel.socket();
        init();
    }
    
    /** Constructs b new NIOServerSocket bound to the given port */
    public NIOServerSocket(int port) throws IOException {
        chbnnel = ServerSocketChannel.open();
        socket = chbnnel.socket();
        init();
        bind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs b new NIOServerSocket bound to the given port, able to accept
     * the given bbcklog of connections.
     */
    public NIOServerSocket(int port, int bbcklog) throws IOException {
        chbnnel = ServerSocketChannel.open();
        socket = chbnnel.socket();
        init();
        bind(new InetSocketAddress(port), bbcklog);
        
    }
    
    /**
     * Constructs b new NIOServerSocket bound to the given port & addr, able to accept
     * the given bbcklog of connections.
     */
    public NIOServerSocket(int port, int bbcklog, InetAddress bindAddr) throws IOException {
        chbnnel = ServerSocketChannel.open();
        socket = chbnnel.socket();
        init();
        bind(new InetSocketAddress(bindAddr, port), bbcklog);
    }
    
    /**
     * Initiblizes the connection.
     * Currently this sets the chbnnel to blocking & reuse addr to false.
     */
    privbte void init() throws IOException {
        chbnnel.configureBlocking(false);
       //socket.setReuseAddress(fblse);
    }

    /**
     * Accepts bn incoming connection.
     */
    public Socket bccept() throws IOException {
        synchronized(LOCK){
            boolebn looped = false;
            int timeout = getSoTimeout();
            while(!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                if(looped && timeout != 0)
                    throw new SocketTimeoutException("bccept timed out: " + timeout);
                    
                LOG.debug("Wbiting for incoming socket...");
                try {
                    LOCK.wbit(timeout);
                } cbtch(InterruptedException ix) {
                    throw new InterruptedIOException(ix);
                }
                looped = true;
            }
                
            IOException x = storedException;
            storedException = null;
            
            if(isClosed())
                throw new SocketException("Socket Closed");
            else if(x != null)
                throw x;
            else if(!isBound())
                throw new SocketException("Not Bound!");
            else {
                LOG.debug("Retrieved b socket!");
                return new NIOSocket((Socket)pendingSockets.remove(0));
            }
        }
    }
    
    /**
     * Notificbtion that a socket has been accepted.
     */
    public void hbndleAccept(SocketChannel channel) {
        synchronized(LOCK) {
            pendingSockets.bdd(channel.socket());
            LOCK.notify();
        }
    }
    
    /**
     * Notificbtion that an IOException occurred while accepting.
     */
    public void hbndleIOException(IOException iox) {
        synchronized(LOCK) {
            storedException = iox;
        }
    }
    
    /**
     * Closes this socket. 
     */
    public void shutdown() {
        try {
            close();
        } cbtch(IOException ignored) {}
    }
    
    /** Binds the socket to the endpoint & stbrts listening for incoming connections */
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
        NIODispbtcher.instance().registerAccept(channel, this);
    }
     
    /** Binds the socket to the endpoint & stbrts listening for incoming connections */
    public void bind(SocketAddress endpoint, int bbcklog) throws IOException {
        socket.bind(endpoint, bbcklog);
        NIODispbtcher.instance().registerAccept(channel, this);
    }
    
    /** Shuts down this NIOServerSocket */
    public void close() throws IOException {
        synchronized(LOCK) {
            LOCK.notify();
            socket.close();
        }
    }


    /////////////////////////////////////////////////////////////
    /////////// Below bre simple wrappers for the socket.
    /////////////////////////////////////////////////////////////    

    public ServerSocketChbnnel getChannel() {
        return socket.getChbnnel();
    }
 
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
    
    public int getLocblPort() {
        return socket.getLocblPort();
    }
    
    public SocketAddress getLocblSocketAddress() {
        return socket.getLocblSocketAddress();
    }
    
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    public boolebn getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    public int getSoTimeout() throws IOException {
        return socket.getSoTimeout();
    }
    
    public boolebn isBound() {
        return socket.isBound();
    }
    
    public boolebn isClosed() {
        return socket.isClosed();
    }
    
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
    
    public void setReuseAddress(boolebn on) throws SocketException {
        socket.setReuseAddress(on);
    }
    
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    public String toString() {
        return "NIOServerSocket::" + socket.toString();
    }
}