package com.limegroup.gnutella;

import java.net.*;
import java.io.*;

/**
 * This class listens for incoming messages on the open UDP port,
 * dispatching those message the appropriate message routers and 
 * reply handlers.
 */
public final class UDPAcceptor implements Runnable {

	/**
	 * Constant for the single <tt>UDPAcceptor</tt> instance.
	 */
	private static final UDPAcceptor INSTANCE = new UDPAcceptor();

	/**
	 * The socket that handles sending and receiving messages over UDP.
	 */
	private DatagramSocket _socket;
	
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	/**
	 * Instance accessor.
	 */
	public static UDPAcceptor instance() {
		return INSTANCE;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	private UDPAcceptor() {}


	/**
	 * Busy loop that accepts incoming messages sent over UDP and 
	 * dispatches them to their appropriate handlers.
	 */
	public void run() {
		int port = RouterService.instance().getTCPListeningPort();

		try {
			_socket = new DatagramSocket(port);
		} catch(SocketException e) {
			return;
		}

		MessageRouter router = RouterService.instance().getMessageRouter();

		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = 
		    new DatagramPacket(datagramBytes, BUFFER_SIZE);

		while(true) {
			try {
				_socket.receive(datagram);
				byte[] data = datagram.getData();
				//System.out.println("DATA RECEIVED: "+new String(data)); 
				int length = datagram.getLength();
				try {
					//Message message = Message.readUdpData(data);	
					// we do things the old way temporarily
					InputStream in = new ByteArrayInputStream(data);
					Message message = Message.read(in);		
					if(message == null) continue;
					router.handleUDPMessage(message, datagram);					
				} catch(BadPacketException e) {
					continue;
				}
			} catch(IOException e) {
				continue;
			} catch(Exception e) {
				continue;
			}
		}
	}

	/**
	 * Sends the <tt>DatagramPacket</tt> out on the open socket.
	 *
	 * @param datagram the <tt>DatagramPacket</tt> to send
	 * @throws <tt>IOException</tt> if there is an output error writing
	 *  to the socket
	 */
	public void sendDatagram(DatagramPacket datagram) throws IOException {
		_socket.send(datagram);
	}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 *
	 * @return the <tt>DatagramSocket</tt> data
	 */
	public String toString() {
		return "UDPAcceptor\r\nsocket: "+_socket;
	}
}
