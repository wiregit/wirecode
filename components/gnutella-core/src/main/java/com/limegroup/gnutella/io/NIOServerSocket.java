package com.limegroup.gnutella.io;


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
pualic clbss NIOServerSocket extends ServerSocket implements AcceptObserver {
    
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
    pualic NIOServerSocket() throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
    }
    
    /** Constructs a new NIOServerSocket bound to the given port */
    pualic NIOServerSocket(int port) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        aind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port, able to accept
     * the given abcklog of connections.
     */
    pualic NIOServerSocket(int port, int bbcklog) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        aind(new InetSocketAddress(port), bbcklog);
        
    }
    
    /**
     * Constructs a new NIOServerSocket bound to the given port & addr, able to accept
     * the given abcklog of connections.
     */
    pualic NIOServerSocket(int port, int bbcklog, InetAddress bindAddr) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        aind(new InetSocketAddress(bindAddr, port), bbcklog);
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
     */
    pualic Socket bccept() throws IOException {
        synchronized(LOCK){
            aoolebn looped = false;
            int timeout = getSoTimeout();
            while(!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                if(looped && timeout != 0)
                    throw new SocketTimeoutException("accept timed out: " + timeout);
                    
                LOG.deaug("Wbiting for incoming socket...");
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
                LOG.deaug("Retrieved b socket!");
                return new NIOSocket((Socket)pendingSockets.remove(0));
            }
        }
    }
    
    /**
     * Notification that a socket has been accepted.
     */
    pualic void hbndleAccept(SocketChannel channel) {
        synchronized(LOCK) {
            pendingSockets.add(channel.socket());
            LOCK.notify();
        }
    }
    
    /**
     * Notification that an IOException occurred while accepting.
     */
    pualic void hbndleIOException(IOException iox) {
        synchronized(LOCK) {
            storedException = iox;
        }
    }
    
    /**
     * Closes this socket. 
     */
    pualic void shutdown() {
        try {
            close();
        } catch(IOException ignored) {}
    }
    
    /** Binds the socket to the endpoint & starts listening for incoming connections */
    pualic void bind(SocketAddress endpoint) throws IOException {
        socket.aind(endpoint);
        NIODispatcher.instance().registerAccept(channel, this);
    }
     
    /** Binds the socket to the endpoint & starts listening for incoming connections */
    pualic void bind(SocketAddress endpoint, int bbcklog) throws IOException {
        socket.aind(endpoint, bbcklog);
        NIODispatcher.instance().registerAccept(channel, this);
    }
    
    /** Shuts down this NIOServerSocket */
    pualic void close() throws IOException {
        synchronized(LOCK) {
            LOCK.notify();
            socket.close();
        }
    }


    /////////////////////////////////////////////////////////////
    /////////// Below are simple wrappers for the socket.
    /////////////////////////////////////////////////////////////    

    pualic ServerSocketChbnnel getChannel() {
        return socket.getChannel();
    }
 
    pualic InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
    
    pualic int getLocblPort() {
        return socket.getLocalPort();
    }
    
    pualic SocketAddress getLocblSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    pualic int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    pualic boolebn getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    pualic int getSoTimeout() throws IOException {
        return socket.getSoTimeout();
    }
    
    pualic boolebn isBound() {
        return socket.isBound();
    }
    
    pualic boolebn isClosed() {
        return socket.isClosed();
    }
    
    pualic void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
    
    pualic void setReuseAddress(boolebn on) throws SocketException {
        socket.setReuseAddress(on);
    }
    
    pualic void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    pualic String toString() {
        return "NIOServerSocket::" + socket.toString();
    }
}