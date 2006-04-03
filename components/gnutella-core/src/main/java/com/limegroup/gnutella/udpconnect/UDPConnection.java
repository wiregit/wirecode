
// Commented for the Learning branch

package com.limegroup.gnutella.udpconnect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImplFactory;

import com.limegroup.gnutella.UDPService;

/**
 * Make a UDPConnection object to create a reliable stream connection with a remote computer that's actually made of UDP packets.
 * A UDP connection works even if both computers are behind network address translation devices.
 * UDPConnection extends Socket, so you can use the same methods you're familiar with from Socket.
 * 
 * A UDPConnection object represents a UDP connection to a remote computer on the Internet.
 * The UDPConnection class extends Socket to implement its interface, letting code use a UDPConnection as it would a Socket.
 * 
 * A UDPConnection object makes a private UDPConnectionProcessor named _processor.
 * The UDPConnectionProcessor actually has the code that makes the UDP connection work.
 */
public class UDPConnection extends Socket {

    /**
     * 1, the byte value of the GGEP "FW" extension that indicates a computer supports the UDP connection feature.
     * This version 1 is used outside the UDP connection code.
     * Within UDP connection packets, the protocol version is UDPConnectionMessage.PROTOCOL_VERSION_NUMBER, 0.
     */
    public static final byte VERSION = (byte)1;

    /** The UDPConnectionProcessor object that represents the UDP connection and has the code to make it work. */
	private UDPConnectionProcessor _processor;

    /**
     * Make a new UDPConnection object, establishing a new UDP connection to a remote computer.
     * Blocks until we've established the connection, or times out and throws an IOException.
     * 
     * When 2 LimeWire programs on the Internet try to open a UDP connection between them, they both call this constructor at the same time.
     * 
     * @param ip   The IP address to connect to in a String, like "1.2.3.4"
     * @param port The port number to connect to
     */
    public UDPConnection(String ip, int port) throws IOException {

        // Convert the given IP address text into an InetAddress object, and call the next UDPConnection constructor
		this(InetAddress.getByName(ip), port);
    }

    /**
     * Make a new UDPConnection object, establishing a new UDP connection to a remote computer.
     * Blocks until we've established the connection, or times out and throws an IOException.
     * 
     * @param ip   The IP address to connect to as a Java InetAddress object
     * @param port The port number to connect to
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {

        // Make a new UDPConnectionProcessor object that will try to open the connection, and save it here as _processor
		_processor = new UDPConnectionProcessor(ip, port);
    }

    /**
     * Get the InputStream object from this UDPConnection you can call inputStream.read(b) on to get data from the remote computer.
     * 
     * @return The InputStream object
     */
	public InputStream getInputStream() throws IOException {

        // Get it from the UDPConnectionProcessor
		return _processor.getInputStream();
	}

    /**
     * Get the OutputStream object from this UDPConnection you can call outputStream.write(b) on to send data to the remote computer.
     * 
     * @return The OutputStream object
     */
	public OutputStream getOutputStream() throws IOException {

        // Get it from the UDPConnectionProcessor
		return _processor.getOutputStream();
	}

    /**
     * Specify how long this UDPConnection should wait without hearing from the remote computer before throwing an exception.
     * 
     * @param The time in milliseconds
     */
	public void setSoTimeout(int timeout) throws SocketException {

        // Have the UDPConnectionProcessor do this
		_processor.setSoTimeout(timeout);
	}

    /**
     * Close this UDP connection with the remote computer.
     */
	public void close() throws IOException {

        // Have the UDPConnectionProcessor close it
		_processor.close();
	}

    /**
     * The IP address of the remote computer we're communicating with through this UDP connection.
     * 
     * @return The remote computer's IP address as a Java InetAddress object
     */
    public InetAddress getInetAddress() {

        // Get it from the UDPConnectionProcessor
        return _processor.getInetAddress();
    }

    /**
     * Get our IP address.
     * 
     * @return Our IP address as a Java InetAddress object
     */
    public InetAddress getLocalAddress() {

        // Get it from the UDPConnectionProcessor
        return _processor.getLocalAddress();
    }

    /**
     * Find out how long this UDPConnection object will wait without hearing from the remote computer before throwing an exception.
     * 
     * @return The timeout in milliseconds
     */
    public int getSoTimeout() {

        // Get it from the UDPConnectionProcessor
    	return _processor.getReadTimeout();
    }

    /*
     * ------- Mostly Unimplemented -------
     */

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public UDPConnection() throws IOException {
        throw new IOException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public UDPConnection(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public UDPConnection(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public UDPConnection(String host, int port, boolean stream) throws IOException {
      throw new IOException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public UDPConnection(InetAddress host, int port, boolean stream) throws IOException {
        throw new IOException("not implemented");
    }

    /*
     * These won't compile in Java 1.3
     * 
     * public void connect(SocketAddress endpoint) throws IOException {
     *     throw new IOException("not implemented");
     * }
     * 
     * public void connect(SocketAddress endpoint, int timeout) throws IOException {
     *     throw new IOException("not implemented");
     * }
     * 
     * public void bind(SocketAddress bindpoint) throws IOException {
     *     throw new IOException("not implemented");
     * }
     */

    /**
     * The port number of the remote computer on the far end of this UDP connection.
     * 
     * @return The remote computer's port number
     */
    public int getPort() {

        // Ask the UDPConnection processor
        return _processor.getPort();
    }

    /**
     * Get our port number.
     * 
     * @return Our port number
     */
    public int getLocalPort() {

        // Ask the UDPService what port number we're receiving UDP packets on
        return UDPService.instance().getStableUDPPort();
    }

    /*
     * These won't compile in Java 1.3
     * 
     * public SocketAddress getRemoteSocketAddress() {
     *     return null;
     * }
     * 
     * public SocketAddress getLocalSocketAddress() {
     *     return null;
     * }
     * 
     * public SocketChannel getChannel() {
     *     return null;
     * }
     */

    /** Does nothing. */
    public void setTcpNoDelay(boolean on) throws SocketException {}

    /** Always returns true. */
    public boolean getTcpNoDelay() throws SocketException {
        return true;
    }

    /** Does nothing. */
    public void setSoLinger(boolean on, int linger) throws SocketException {}

    /** Always returns -1. */
    public int getSoLinger() throws SocketException {
        return -1;
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void sendUrgentData(int data) throws IOException  {
        throw new IOException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void setOOBInline(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public boolean getOOBInline() throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public synchronized void setSendBufferSize(int size) throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public synchronized int getSendBufferSize() throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public synchronized void setReceiveBufferSize(int size) throws SocketException{
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public synchronized int getReceiveBufferSize() throws SocketException{
        throw new SocketException("not implemented");
    }

    /** Does nothing. */
    public void setKeepAlive(boolean on) throws SocketException {}

    /** Always returns true. */
    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void setTrafficClass(int tc) throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public int getTrafficClass() throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void setReuseAddress(boolean on) throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public boolean getReuseAddress() throws SocketException {
        throw new SocketException("not implemented");
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void shutdownInput() throws IOException {
        throw new SocketException("not implemented");
    }
    
    /** UDPConnection extends Socket, but doesn't implement this method. */
    public void shutdownOutput() throws IOException {
        throw new IOException("not implemented");
    }

    /** Always returns "UDPConnection". */
    public String toString() {
        return "UDPConnection";
    }

    /**
     * Determine if this UDP connection is still connected to the remote computer.
     * 
     * @return True if the connection is open, false if it's closed
     */
    public boolean isConnected() {

        // Ask the UDPConnectionProcessor
        return _processor.isConnected();
    }

    /** Always returns true. */
    public boolean isBound() {
        return true;
    }

    /**
     * Determine if this UDP connection is still connected to the remote computer.
     * 
     * @return True if the connection is closed, false if it's open
     */
    public boolean isClosed() {

        // Ask the UDPConnectionProcessor
        return !_processor.isConnected();
    }

    /**
     * Determine if this UDP connection is still connected to the remote computer.
     * 
     * @return True if the connection is closed, false if it's open
     */
    public boolean isInputShutdown() {

        // Ask the UDPConnectionProcessor
        return !_processor.isConnected();
    }

    /**
     * Determine if this UDP connection is still connected to the remote computer.
     * 
     * @return True if the connection is closed, false if it's open
     */
    public boolean isOutputShutdown() {

        // Ask the UDPConnectionProcessor
        return !_processor.isConnected();
    }

    /** UDPConnection extends Socket, but doesn't implement this method. */
    public static void setSocketImplFactory(SocketImplFactory fac) throws IOException {
        throw new IOException("not implemented");
    }
}
