package com.limegroup.gnutella.udpconnect;

import java.io.*;
import java.net.*;
import com.limegroup.gnutella.messages.BadPacketException;

/** 
 *  Create a reliable udp connection interface.
 */
public class UDPConnection {

	private UDPConnectionProcessor _processor;

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
}
