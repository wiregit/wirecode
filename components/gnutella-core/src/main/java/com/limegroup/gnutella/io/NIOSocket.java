package com.limegroup.gnutella.io;


import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import org.apache.commons.logging.*;

/**
 * A Socket that does all of its connecting/reading/writing using NIO, but psuedo-blocks.
 *
 * Phase-1 in converting to NIO.
 */
public class NIOSocket extends Socket implements ConnectHandler, ReadHandler, WriteHandler {
    
    private static final Log LOG = LogFactory.getLog(NIOSocket.class);
    
    private final SocketChannel channel;
    private final Socket socket;
    private final NIOOutputStream writer;
    private final NIOInputStream reader;
    private IOException storedException = null;
    private InetAddress connectedTo;
    
    private final Object LOCK = new Object();
    
    
    /**
     * Constructs an NIOSocket using a pre-existing Socket.
     * To be used by NIOServerSocket while accepting incoming connections.
     */
    NIOSocket(Socket s) throws IOException {
        channel = s.getChannel();
        socket = s;
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        writer.init();
        reader.init();
        NIODispatcher.instance().registerReadWrite(channel, this);
        connectedTo = s.getInetAddress();
    }
    
    /** Creates an unconnected NIOSocket. */
    public NIOSocket() throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
    }
    
    /** Creates an NIOSocket and connects (with no timeout) to addr/port */
    public NIOSocket(InetAddress addr, int port) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        connect(new InetSocketAddress(addr, port));
    }
    
    /** Creates an NIOSocket locally bound to localAddr/localPort and connects (with no timeout) to addr/port */
    public NIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        bind(new InetSocketAddress(localAddr, localPort));
        connect(new InetSocketAddress(addr, port));
    }
    
    /** Creates an NIOSocket and connects (with no timeout) to addr/port */
    public NIOSocket(String addr, int port) throws UnknownHostException, IOException {
        this(InetAddress.getByName(addr), port);
    }
    
    /** Creates an NIOSocket locally bound to localAddr/localPort and connects (with no timeout) to addr/port */
    public NIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        this(InetAddress.getByName(addr), port, localAddr, localPort);
    }
    
    /**
     * Performs initialization for this NIOSocket.
     * Currently just makes the channel non-blocking.
     */
    private void init() throws IOException {
        channel.configureBlocking(false);
    }
    
    /**
     * Notification that a connect can occur.
     *
     * This notifies the waiting lock so that connect can continue.
     */
    public void handleConnect() throws IOException {
        synchronized(LOCK) {
            LOCK.notify();
        }
    }
    
    /**
     * Notification that a read can occur.
     *
     * This passes it off to the NIOInputStream.
     */
    public boolean handleRead() throws IOException {
        return reader.readChannel();
    }
    
    /**
     * Notification that a write can occur.
     *
     * This passes it off to the NIOOutputStream.
     */
    public boolean handleWrite() throws IOException {
        return writer.writeChannel();
    }
    
    /**
     * Notification that an IOException occurred while processing a
     * read, connect, or write.
     *
     * This wakes up any waiting locks and shuts down the socket & all streams.
     */
    public void handleIOException(IOException iox) {
        synchronized(LOCK) {
            storedException = iox;
            LOCK.notify();
        }
        
        shutdown();
    }
    
    /**
     * Shuts down this socket & all its streams.
     */
    void shutdown() {
        try {
            shutdownInput();
        } catch(IOException ignored) {}
            
        try {
            shutdownOutput();
        } catch(IOException ignored) {}
            
        reader.shutdown();
        writer.shutdown();
        
        try {
            socket.close();
        } catch(IOException ignored) {}
            
        try {
            channel.close();
        } catch(IOException ignored) {}
    }
    
    /** Binds the socket to the SocketAddress */
    public void bind(SocketAddress endpoint) throws IOException {
        socket.bind(endpoint);
    }
    
    /** Closes the socket & all streams, waking up any waiting locks.  */
    public void close() throws IOException {
        shutdown();
        
        synchronized(LOCK) {
            LOCK.notify();
        }
    }
    
    /** Connects to addr with no timeout */
    public void connect(SocketAddress addr) throws IOException {
        connect(addr, 0);
    }
    
    /** Connects to addr with the given timeout (in milliseconds) */
    public void connect(SocketAddress addr, int timeout) throws IOException {
        connectedTo = ((InetSocketAddress)addr).getAddress();

        synchronized(LOCK) {
            if(!channel.connect(addr)) {
                NIODispatcher.instance().registerConnect(channel, this);
                try {
                    LOCK.wait(timeout);
                } catch(InterruptedException ix) {
                    throw new InterruptedIOException(ix);
                }
                
                IOException x = storedException;
                storedException = null;
                if(x != null)
                    throw x;
                if(!isConnected())
                    throw new SocketTimeoutException("couldn't connect in " + timeout + " milliseconds");
                    
            }
        }
        
        if(LOG.isTraceEnabled())
            LOG.trace("Connected to: " + addr);
        writer.init();
        reader.init();
    }
    
     /**
      * Retrieves the host this is connected to.
      * The separate variable for storage is necessary because Sockets created
      * with SocketChannel.open() return null when there's no connection.
      */
     public InetAddress getInetAddress() {
        return connectedTo;
    }
    
    /**
     * Returns the InputStream from the NIOInputStream.
     *
     * Internally, this is a blocking Pipe from the non-blocking SocketChannel.
     */
    public InputStream getInputStream() throws IOException {
        if(isClosed())
            throw new IOException("Socket closed.");
        
        return reader.getInputStream();
    }
    
    /**
     * Returns the OutputStream from the NIOOutputStream.
     *
     * Internally, this is a blcoking Pipe from the non-blocking SocketChannel.
     */
    public OutputStream getOutputStream() throws IOException {
        if(isClosed())
            throw new IOException("Socket closed.");
        
        return writer.getOutputStream();
    }
    
    
    ///////////////////////////////////////////////
    /// BELOW ARE ALL WRAPPERS FOR SOCKET.
    ///////////////////////////////////////////////
    
    
    public SocketChannel getChannel() {
        return socket.getChannel();
    }
 
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    public boolean getOOBInline() throws SocketException {
        return socket.getOOBInline();
    }
    
    public int getPort() {
        return socket.getPort();
    }
    
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    public int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }
    
    public int getSoLinger() throws SocketException {
        return socket.getSoLinger();
    }
    
    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }
    
    public boolean getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay();
    }
    
    public int getTrafficClass()  throws SocketException {
        return socket.getTrafficClass();
    }
    
    public boolean isBound() {
        return socket.isBound();
    }
    
    public boolean isClosed() {
        return socket.isClosed();
    }
    
    public boolean isConnected() {
        return socket.isConnected();
    }
    
    public boolean isInputShutdown() {
        return socket.isInputShutdown();
    }
    
    public boolean isOutputShutdown() {
        return socket.isOutputShutdown();
    }
    
    public void sendUrgentData(int data) {
        throw new UnsupportedOperationException("No urgent data.");
    }
    
    public void setKeepAlive(boolean on) throws SocketException {
        socket.setKeepAlive(on);
    }
    
    public void setOOBInline(boolean on) throws SocketException {
        socket.setOOBInline(on);
    }
    
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }
    
    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }
    
    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }
    
    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket.setSoLinger(on, linger);
    }
    
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    public void setTcpNoDelay(boolean on) throws SocketException {
        socket.setTcpNoDelay(on);
    }
    
    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }
    
    public void shutdownInput() throws IOException {
        socket.shutdownInput();
    }
    
    public void shutdownOutput() throws IOException {
        socket.shutdownOutput();
    }
    
    public String toString() {
        return "NIOSocket::" + channel.toString();
    }
}