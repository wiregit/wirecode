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
	private final DatagramSocket UDP_SOCKET;
	
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	/**
	 * Constant buffer used for the message header data.
	 */
	private byte[] HEADER_BUF=new byte[23];

	/**
	 * Instance accessor.
	 */
	public static UDPAcceptor instance() {
		return INSTANCE;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>, attempting to open the
	 * <tt>DatagramSocket</tt>.
	 */
	private UDPAcceptor() {
		DatagramSocket tempSocket = null;
		for(int i=6346; i<6357; i++) {
			try {
				tempSocket = new DatagramSocket(i);
				break;
			} catch(SocketException e) {
			}
		}
		
		// this can be null if no socket was created
		UDP_SOCKET = tempSocket;
	}

	/**
	 * Busy loop that accepts incoming messages sent over UDP and 
	 * dispatches them to their appropriate handlers.
	 */
	public void run() {
		// if the socket could not be initialized, return
		if(UDP_SOCKET == null) return;
		MessageRouter router = RouterService.instance().getMessageRouter();

		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = 
		    new DatagramPacket(datagramBytes, BUFFER_SIZE);

		while(true) {
			try {
				UDP_SOCKET.receive(datagram);
				byte[] data = datagram.getData();
				//System.out.println("DATA RECEIVED: "+new String(data)); 
				int length = datagram.getLength();
				try {
					//Message message = Message.readUdpData(data);	
					// we do things the old way temporarily
					InputStream in = new ByteArrayInputStream(data);
					Message message = Message.read(in, HEADER_BUF);		
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

	public void sendDatagram(DatagramPacket datagram) throws IOException {
		UDP_SOCKET.send(datagram);
	}

	public DatagramSocket getDatagramSocket() {
		return UDP_SOCKET;
	}

	/**
	 * Returns the port that the UDP socket is listening on.
	 * 
	 * @param the port that the UDP socket is listening on, in the range
	 *  6346-6356, or -1 if the socket has not yet been initialized
	 */
	//public int getPort() {
	//return UDP_SOCKET.getLocalPort();
	//}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 *
	 * @return the <tt>DatagramSocket</tt> data
	 */
	public String toString() {
		return "UDPAcceptor\r\nsocket: "+UDP_SOCKET;
	}
}
