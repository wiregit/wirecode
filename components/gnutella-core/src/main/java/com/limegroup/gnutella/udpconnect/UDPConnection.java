package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.BadPacketException;

/** 
 *  Create a reliable udp connection interface.
 */
public class UDPConnection extends Socket {

    public static final byte VERSION = (byte) 1;

	private UDPConnectionProcessor _processor;

    /**
     *  Create the UDPConnection.
     */
    public UDPConnection(String ip, int port) throws IOException {
		// Handle the real work in the processor
		this(InetAddress.getByName(ip), port);
    }

    /**
     *  Create the UDPConnection.
     */
    public UDPConnection(InetAddress ip, int port) throws IOException {
		// Handle the real work in the processor
		_processor = new UDPConnectionProcessor(ip, port);
    }

	public InputStream getInputStream() throws IOException {
		return _processor.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		return _processor.getOutputStream();
	}

	public void setSoTimeout(int timeout) throws SocketException {
		_processor.setSoTimeout(timeout);
	}

	public void close() throws IOException {
		_processor.close();
	}

    public InetAddress getInetAddress() {
        return _processor.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return _processor.getLocalAddress();
    }
    
    //-------  Mostly Unimplemented  ----------------

    public UDPConnection() throws IOException {
        throw new IOException("not implemented");
    }


    public UDPConnection(String host, int port, InetAddress localAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress address, int port, InetAddress localAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    public UDPConnection(String host, int port, boolean stream) 
      throws IOException {
      throw new IOException("not implemented");
    }

    public UDPConnection(InetAddress host, int port, boolean stream) 
      throws IOException {
        throw new IOException("not implemented");
    }

    // These won't compile in Java 1.3
    //public void connect(SocketAddress endpoint) throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void connect(SocketAddress endpoint, int timeout) 
      //throws IOException {
        //throw new IOException("not implemented");
    //}

    //public void bind(SocketAddress bindpoint) throws IOException {
        //throw new IOException("not implemented");
    //}


    public int getPort() {
        return -1;
    }

    public int getLocalPort() {
        return -1;
    }

    // These won't compile in Java 1.3
    //public SocketAddress getRemoteSocketAddress() {
        //return null;
    //}

    //public SocketAddress getLocalSocketAddress() {
        //return null;
    //}

    //public SocketChannel getChannel() {
        //return null;
    //}

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
    
    public void shutdownOutput() throws IOException
    {
        throw new IOException("not implemented");
    }

    public String toString() {
        return "UDPConnection";
    }

    public boolean isConnected() {
        return _processor.isConnected();
    }

    public boolean isBound() {
        return true;
    }

    public boolean isClosed() {
        return !_processor.isConnected();
    }

    public boolean isInputShutdown() {
        return !_processor.isConnected();
    }

    public boolean isOutputShutdown() {
        return !_processor.isConnected();
    }

    public static void setSocketImplFactory(SocketImplFactory fac)
      throws IOException {
        throw new IOException("not implemented");
    }
}
