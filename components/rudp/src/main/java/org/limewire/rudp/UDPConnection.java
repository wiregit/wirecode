package org.limewire.rudp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

/** 
 *  A reliable UDP connection.
 */
public class UDPConnection extends AbstractNBSocket {
    
    /** The current version of the reliable UDP protocol. */
    public static final byte VERSION = (byte) 1;
    
    /** Channel backing the socket. */
    private final UDPSocketChannel channel;

    /** The default read timeout. */
    private int soTimeout = 1 * 60 * 1000; // default to 1 minute.
    
    /** The context surrounding this connection . */
    private final RUDPContext context;

    /**
     * Creates an unconnected <code>UDPConnection</code>. You must call {@link #connect(SocketAddress) connect(...)} to connect.
     */
    public UDPConnection() {
        this(UDPSelectorProvider.defaultProvider());
    }
    
    public UDPConnection(UDPSelectorProvider provider) {
        this.context = provider.getContext();
        channel = provider.openSocketChannel();
        channel.setSocket(this);
        setInitialReader();
        setInitialWriter();  
    }
    
    /**
     *  Create a <code>UDPConnection</code> connected to the given IP/Port.
     */
    public UDPConnection(String ip, int port) throws IOException {
        // Handle the real work in the processor
        this(InetAddress.getByName(ip), port);
    }     

    /**
     *  Creates a <code>UDPConnection</code> connected to the given IP/Port.
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {
		this();
        connect(new InetSocketAddress(ip, port));
    }

    /** Returns the <code>UDPSocketChannel</code>, since it already implements 
     * <code>InterestReadChannel</code>. */
    protected InterestReadableByteChannel getBaseReadChannel() {
        return channel;
    }
    
    /** Returns the <code>UDPSocketChannel</code>, since it already implements 
     * <code>InterestWriteChannel</code>. */
    protected InterestWritableByteChannel getBaseWriteChannel() {
        return channel;
    }

    /** Doesn't do anything. */
    protected void shutdownImpl() {
    }

    /** Sets the read timeout this socket should use. */
	public void setSoTimeout(int timeout) {
        soTimeout = timeout;
	}
	
	/** Returns the timeout this socket uses when reading. */
	public int getSoTimeout() {
	    return soTimeout;
	}

    /** Returns the local address this socket uses. */
    public InetAddress getLocalAddress() {
        return context.getUDPService().getStableListeningAddress();
    }
    
    public SocketAddress getRemoteSocketAddress() {
        return channel.getRemoteSocketAddress();
    }
    
    public InetAddress getInetAddress() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getAddress();
    }

    public int getPort() {
        return ((InetSocketAddress)getRemoteSocketAddress()).getPort();
    }

    public int getLocalPort() {
        return context.getUDPService().getStableListeningPort();
    }
    
    public SocketAddress getLocalSocketAddress() {
        return new InetSocketAddress(getLocalAddress(), getLocalPort());
    }
    
    public SocketChannel getChannel() {
        return channel;
    }    

    public String toString() {
        return "UDPConnection:" + channel;
    }

    public boolean isConnected() {
        return channel.isConnected();
    }

    public boolean isBound() {
        return true;
    }

    public boolean isClosed() {
        return !channel.isOpen();
    }

    public boolean isInputShutdown() {
        return !channel.isOpen();
    }

    public boolean isOutputShutdown() {
        return !channel.isOpen();
    }
    
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new IOException("not implemented");
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        // does nothing
    }

    public boolean getTcpNoDelay() throws SocketException {
        return true;
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        // does nothing
    }

    public int getSoLinger() throws SocketException {
        return -1;
    }

    public void sendUrgentData (int data) throws IOException  {
        throw new IOException("not implemented");
    }

    public void setOOBInline(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolean getOOBInline() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setSendBufferSize(int size)
      throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized int getSendBufferSize() throws SocketException {
        throw new SocketException("not implemented");
    }

    public synchronized void setReceiveBufferSize(int size)
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public synchronized int getReceiveBufferSize()
      throws SocketException{
        throw new SocketException("not implemented");
    }

    public void setKeepAlive(boolean on) throws SocketException {
        // ignore
    }

    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    public void setTrafficClass(int tc) throws SocketException {
        throw new SocketException("not implemented");
    }

    public int getTrafficClass() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void setReuseAddress(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    public boolean getReuseAddress() throws SocketException {
        throw new SocketException("not implemented");
    }

    public void shutdownInput() throws IOException {
        throw new SocketException("not implemented");
    }

    public void shutdownOutput() throws IOException {
        throw new IOException("not implemented");
    }
    
}
