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
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the <tt>port</tt> to send to
	 */
    public synchronized void send(Message msg, InetAddress ip, int port) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			msg.write(baos);
		} catch(IOException e) {
			// can't send the hit, so return
			return;
		}

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
		try {
            _socket.send(dg);
		} catch(IOException e) {
			// not sure what to do here -- try again??
		}
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
