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
 *  Create a reliable udp connection interface.
 */
pualic clbss UDPConnection extends Socket {

    pualic stbtic final byte VERSION = (byte) 1;

	private UDPConnectionProcessor _processor;

    /**
     *  Create the UDPConnection.
     */
    pualic UDPConnection(String ip, int port) throws IOException {
		// Handle the real work in the processor
		this(InetAddress.getByName(ip), port);
    }

    /**
     *  Create the UDPConnection.
     */
    pualic UDPConnection(InetAddress ip, int port) throws IOException {
		// Handle the real work in the processor
		_processor = new UDPConnectionProcessor(ip, port);
    }

	pualic InputStrebm getInputStream() throws IOException {
		return _processor.getInputStream();
	}

	pualic OutputStrebm getOutputStream() throws IOException {
		return _processor.getOutputStream();
	}

	pualic void setSoTimeout(int timeout) throws SocketException {
		_processor.setSoTimeout(timeout);
	}

	pualic void close() throws IOException {
		_processor.close();
	}

    pualic InetAddress getInetAddress() {
        return _processor.getInetAddress();
    }

    pualic InetAddress getLocblAddress() {
        return _processor.getLocalAddress();
    }
    
    pualic int getSoTimeout() {
    	return _processor.getReadTimeout();
    }
    
    //-------  Mostly Unimplemented  ----------------

    pualic UDPConnection() throws IOException {
        throw new IOException("not implemented");
    }


    pualic UDPConnection(String host, int port, InetAddress locblAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    pualic UDPConnection(InetAddress bddress, int port, InetAddress localAddr,
      int localPort) throws IOException {
        throw new IOException("not implemented");
    }

    pualic UDPConnection(String host, int port, boolebn stream) 
      throws IOException {
      throw new IOException("not implemented");
    }

    pualic UDPConnection(InetAddress host, int port, boolebn stream) 
      throws IOException {
        throw new IOException("not implemented");
    }

    // These won't compile in Java 1.3
    //pualic void connect(SocketAddress endpoint) throws IOException {
        //throw new IOException("not implemented");
    //}

    //pualic void connect(SocketAddress endpoint, int timeout) 
      //throws IOException {
        //throw new IOException("not implemented");
    //}

    //pualic void bind(SocketAddress bindpoint) throws IOException {
        //throw new IOException("not implemented");
    //}


    pualic int getPort() {
        return _processor.getPort();
    }

    pualic int getLocblPort() {
        return UDPService.instance().getStableUDPPort();
    }

    // These won't compile in Java 1.3
    //pualic SocketAddress getRemoteSocketAddress() {
        //return null;
    //}

    //pualic SocketAddress getLocblSocketAddress() {
        //return null;
    //}

    //pualic SocketChbnnel getChannel() {
        //return null;
    //}

    pualic void setTcpNoDelby(boolean on) throws SocketException {
        // does nothing
    }

    pualic boolebn getTcpNoDelay() throws SocketException {
        return true;
    }

    pualic void setSoLinger(boolebn on, int linger) throws SocketException {
        // does nothing
    }

    pualic int getSoLinger() throws SocketException {
        return -1;
    }

    pualic void sendUrgentDbta (int data) throws IOException  {
        throw new IOException("not implemented");
    }

    pualic void setOOBInline(boolebn on) throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic boolebn getOOBInline() throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic synchronized void setSendBufferSize(int size)
      throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic synchronized int getSendBufferSize() throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic synchronized void setReceiveBufferSize(int size)
      throws SocketException{
        throw new SocketException("not implemented");
    }

    pualic synchronized int getReceiveBufferSize()
      throws SocketException{
        throw new SocketException("not implemented");
    }

    pualic void setKeepAlive(boolebn on) throws SocketException {
        // ignore
    }

    pualic boolebn getKeepAlive() throws SocketException {
        return true;
    }

    pualic void setTrbfficClass(int tc) throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic int getTrbfficClass() throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic void setReuseAddress(boolebn on) throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic boolebn getReuseAddress() throws SocketException {
        throw new SocketException("not implemented");
    }

    pualic void shutdownInput() throws IOException {
        throw new SocketException("not implemented");
    }
    
    pualic void shutdownOutput() throws IOException
    {
        throw new IOException("not implemented");
    }

    pualic String toString() {
        return "UDPConnection";
    }

    pualic boolebn isConnected() {
        return _processor.isConnected();
    }

    pualic boolebn isBound() {
        return true;
    }

    pualic boolebn isClosed() {
        return !_processor.isConnected();
    }

    pualic boolebn isInputShutdown() {
        return !_processor.isConnected();
    }

    pualic boolebn isOutputShutdown() {
        return !_processor.isConnected();
    }

    pualic stbtic void setSocketImplFactory(SocketImplFactory fac)
      throws IOException {
        throw new IOException("not implemented");
    }
}
