package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import org.limewire.io.NetworkUtils;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;

/** 
 *  Create a reliable udp connection interface.
 */
public class UDPConnection extends AbstractNBSocket {
    
    /** The current version of the reliable UDP protocol. */
    public static final byte VERSION = (byte) 1;
    
    /** Channel backing the socket. */
    private final UDPSocketChannel channel;

    /** The default read timeout. */
    private int soTimeout = 1 * 60 * 1000; // default to 1 minute.

    /**
     * Creates an unconnected UDPConnection. You must call connect(...) to connect.
     */
    public UDPConnection() {
        channel = (UDPSocketChannel)RouterService.getUDPSelectorProvider().openSocketChannel();
        channel.setSocket(this);
        setInitialReader();
        setInitialWriter();  
    }
    
    /**
     *  Create a UDPConnection connected to the given IP/Port.
     */
    public UDPConnection(String ip, int port) throws IOException {
        // Handle the real work in the processor
        this(InetAddress.getByName(ip), port);
    }     

    /**
     *  Creates a UDPConnection connected to the given IP/Port.
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {
		this();
        connect(new InetSocketAddress(ip, port));
    }

    /** Returns the UDPSocketChannel, since it already implements InterestReadChannel. */
    protected InterestReadableByteChannel getBaseReadChannel() {
        return channel;
    }
    
    /** Returns the UDPSocketChannel, since it already implements InterestWriteChannel. */
    protected InterestWritableByteChannel getBaseWriteChannel() {
        return channel;
    }

    /** Does nothing. */
    protected void shutdownImpl() {
    }

    /** Sets the soTimeout this socket should use. */
	public void setSoTimeout(int timeout) {
        soTimeout = timeout;
	}
	
	/** Returns the timeout this socket uses when reading. */
	public int getSoTimeout() {
	    return soTimeout;
	}

    /** Returns the local address this socket uses. */
    public InetAddress getLocalAddress() {
        InetAddress lip = null;
        try {
            lip = InetAddress.getByName(
              NetworkUtils.ip2string(RouterService.getNonForcedAddress()));
        } catch (UnknownHostException uhe) {
            try {
                lip = InetAddress.getLocalHost();
            } catch (UnknownHostException uhe2) {
                lip = null;
            }
        }

        return lip;
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
        return UDPService.instance().getStableUDPPort();
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
