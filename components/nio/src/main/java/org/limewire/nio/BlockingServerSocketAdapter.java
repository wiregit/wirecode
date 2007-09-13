package org.limewire.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.nio.observer.AcceptObserver;


/**
 * A <code>ServerSocket</code> that allows asynchronous accepts but is backed
 * by a legacy I/O <code>ServerSocket</code>.
 * <p>
 * This is intended primarily for ease of use debugging via swapping out
 * an {@link NIOServerSocket} with a <code>BlockingServerSocketAdapter</code>, so 
 * NIO code can be compared to regular I/O code.
 */
public class BlockingServerSocketAdapter extends ServerSocket {
    
    private final AcceptObserver observer;
    private final ServerSocket delegate;
    
    /**
     * Constructs a new, unbound, BlockingServerSocketAdapter.
     * You must call 'bind' to start listening for incoming connections.
     */
    public BlockingServerSocketAdapter() throws IOException {
        this(null);
    }
    
    /**
     * Constructs a new, unbound, BlockingServerSocketAdapter.
     * You must call 'bind' to start listening for incoming connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public BlockingServerSocketAdapter(AcceptObserver observer) throws IOException {
        this.delegate = new ServerSocket();
        this.observer = observer;
    }
    
    /** Constructs a new BlockingServerSocketAdapter bound to the given port */
    public BlockingServerSocketAdapter(int port) throws IOException {
        this(port, null);
    }

    /** 
     * Constructs a new BlockingServerSocketAdapter bound to the given port 
     * All accepted connections will be routed to the given AcceptObserver
     */
    public BlockingServerSocketAdapter(int port, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port));
    }
    
    /**
     * Constructs a new BlockingServerSocketAdapter bound to the given port, able to accept
     * the given backlog of connections.
     */
    public BlockingServerSocketAdapter(int port, int backlog) throws IOException {
        this(port, backlog, (AcceptObserver)null);
    }
    
    /**
     * Constructs a new BlockingServerSocketAdapter bound to the given port, able to accept
     * the given backlog of connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public BlockingServerSocketAdapter(int port, int backlog, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(port), backlog);
        
    }
    
    /**
     * Constructs a new BlockingServerSocketAdapter bound to the given port & addr, able to accept
     * the given backlog of connections.
     */
    public BlockingServerSocketAdapter(int port, int backlog, InetAddress bindAddr) throws IOException {
        this(port, backlog, bindAddr, null);
    }
    
    /**
     * Constructs a new BlockingServerSocketAdapter bound to the given port & addr, able to accept
     * the given backlog of connections.
     * All accepted connections will be routed to the given AcceptObserver.
     */
    public BlockingServerSocketAdapter(int port, int backlog, InetAddress bindAddr, AcceptObserver observer) throws IOException {
        this(observer);
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        delegate.bind(endpoint, backlog);
        startListening();
    }

    public void bind(SocketAddress endpoint) throws IOException {
        delegate.bind(endpoint);
        startListening();
    }
    
    /**
     * Blocks until a socket is accepted.
     * If this was constructed with an AcceptObserver, this will
     * throw an IllegalBlockingModeException, as the AcceptObserver
     * will be asynchronously routed all the connections.
     */
    public Socket accept() throws IOException {
        if(observer != null)
            throw new IllegalBlockingModeException();
        return delegate.accept();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public ServerSocketChannel getChannel() {
        return delegate.getChannel();
    }

    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    public int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    public int getSoTimeout() throws IOException {
        return delegate.getSoTimeout();
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isBound() {
        return delegate.isBound();
    }

    public boolean isClosed() {
        return delegate.isClosed();
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    public String toString() {
        return delegate.toString();
    }
    
    /**
     * Starts a thread that will route accepted connections
     * to the AcceptObserver.
     */
    private void startListening() {
        if(observer != null) {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    while(!isClosed()) {
                        try {
                            Socket s = delegate.accept();
                            observer.handleAccept(s);
                        } catch(IOException ignored) {}
                    }
                    observer.shutdown();
                }
            }, "BlockingServerSocketEmulator");
        }
    }
    
}
