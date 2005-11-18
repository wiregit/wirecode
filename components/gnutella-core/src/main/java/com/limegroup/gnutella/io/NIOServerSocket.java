package com.limegroup.gnutella.io;

// Edited for the Learning branch

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.net.ServerSocket;

import java.util.List;
import java.util.LinkedList;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A ServerSocket that does all of its accepting using NIO, but psuedo-blocks.
 */
public class NIOServerSocket extends ServerSocket implements AcceptObserver {
    
    private static final Log LOG = LogFactory.getLog(NIOServerSocket.class);
    
    private final ServerSocketChannel channel;
    private final ServerSocket socket;
    
    private final List pendingSockets = new LinkedList();
    private IOException storedException = null;
    
    private final Object LOCK = new Object();
    
    /**
     * Constructs a new, unbound, NIOServerSocket.
     * You must call 'bind' to start listening for incoming connections.
     */
    public NIOServerSocket() throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
    }
    
    /** Constructs a new NIOServerSocket bound to the given port */
    public NIOServerSocket(int port) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port, able to accept
     * the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(port), backlog);
        
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port & addr, able to accept
     * the given backlog of connections.
     */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }
    
    /**
     * Initializes the connection.
     * Currently this sets the channel to blocking & reuse addr to false.
     */
    private void init() throws IOException {
        channel.configureBlocking(false);
       //socket.setReuseAddress(false);
    }

    /**
     * Accepts an incoming connection.
     * 
     * 
     * This is the call that is part of the line:
     * 
     *  client = _socket.accept();
     * 
     * It pretends to be the accept() method on Java's ServerSocket
     * Uses NIO which doesn't block, but pretends to block for the benefit of the Acceptor class, which expects blocking
     * 
     */
    public Socket accept() throws IOException {
        synchronized(LOCK){
            boolean looped = false;
            int timeout = getSoTimeout();
            while(!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                if(looped && timeout != 0)
                    throw new SocketTimeoutException("accept timed out: " + timeout);
                    
                LOG.debug("Waiting for incoming socket...");
                try {
                    LOCK.wait(timeout);
                } catch(InterruptedException ix) {
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
                LOG.debug("Retrieved a socket!");
                return new NIOSocket((Socket)pendingSockets.remove(0));
            }
        }
    }
    
    /**
     * Notification that a socket has been accepted.
     */
    public void handleAccept(SocketChannel channel) {
        synchronized(LOCK) {
            pendingSockets.add(channel.socket());
            LOCK.notify();
        }
    }
    
    /**
     * Notification that an IOException occurred while accepting.
     */
    public void handleIOException(IOException iox) {
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
        synchronized(LOCK) {
            LOCK.notify();
            socket.close();
        }
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
}