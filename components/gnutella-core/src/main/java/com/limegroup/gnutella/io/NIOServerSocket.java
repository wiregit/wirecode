package com.limegroup.gnutella.io;


import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import java.util.*;

import org.apache.commons.logging.*;

/**
 * A ServerSocket that does all of its accepting using NIO, but psuedo-blocks.
 *
 * Phase-1 in converting to NIO.
 */
public class NIOServerSocket extends ServerSocket implements AcceptHandler {
    
    private static final Log LOG = LogFactory.getLog(NIOServerSocket.class);
    
    private final ServerSocketChannel channel;
    private final ServerSocket socket;
    
    private final List pendingSockets = new LinkedList();
    private IOException storedException = null;
    
    private final Object LOCK = new Object();
    
    
    public NIOServerSocket() throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
    }
    
    public NIOServerSocket(int port) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(port));
    }
    
    public NIOServerSocket(int port, int backlog) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(port), backlog);
        
    }
    
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }
    
    private void init() throws IOException {
        channel.configureBlocking(false);
        socket.setReuseAddress(false);
    }
        
    public Socket accept() throws IOException {
        synchronized(LOCK){
            while(!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {
                LOG.debug("Waiting for incoming socket...");
                try {
                    LOCK.wait();
                } catch(InterruptedException ix) {
                    throw new InterruptedIOException(ix);
                }
            }
                
            IOException x = storedException;
            storedException = null;
            
            if(x != null)
                throw x;
            else if(isClosed())
                throw new SocketException("Socket Closed");
            else if(!isBound())
                throw new SocketException("Not Bound!");
            else {
                LOG.debug("Retrieved a socket!");
                return new NIOSocket((Socket)pendingSockets.remove(0));
            }
        }
    }
    
    public void handleAccept(SocketChannel channel) {
        synchronized(LOCK) {
            pendingSockets.add(channel.socket());
            LOCK.notify();
        }
    }
    
    public void handleIOException(IOException iox) {
        synchronized(LOCK) {
            storedException = iox;
        }
    }
    
    public int interestOps() {
        return SelectionKey.OP_ACCEPT;
    }
    
    public SelectableChannel getSelectableChannel() {
        return channel;
    }
    
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
        NIODispatcher.instance().register(this);
    }
     
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        socket.bind(endpoint, backlog);
        NIODispatcher.instance().register(this);
    }
    
    public void close() throws IOException {
        synchronized(LOCK) {
            LOCK.notify();
            socket.close();
        }
    }
    
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