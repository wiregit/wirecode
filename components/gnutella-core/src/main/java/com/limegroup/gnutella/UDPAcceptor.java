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

	private final int SOCKET_TIMEOUT = 2*1000;

	/**
	 * The thread for listening of incoming messages.
	 */
	private Thread _udpThread;

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


	public void initialize() {
		_udpThread = new Thread(this);
		_udpThread.setDaemon(true);		
		_udpThread.start();
	}

	/**
	 * Busy loop that accepts incoming messages sent over UDP and 
	 * dispatches them to their appropriate handlers.
	 */
	public void run() {
		while(RouterService.getTCPListeningPort() == -1) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				return;
			}
		}
		int port = RouterService.getTCPListeningPort();
		try {
			_socket = new DatagramSocket(port);
			//_socket.setSoTimeout(SOCKET_TIMEOUT);
		} catch(SocketException e) {
			e.printStackTrace();
			return;
		}		
		
		MessageRouter router = RouterService.getMessageRouter();

		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = 
		    new DatagramPacket(datagramBytes, BUFFER_SIZE);

		
		while(port == RouterService.getTCPListeningPort()) {
			try {				
				_socket.receive(datagram);
				byte[] data = datagram.getData();
				int length = datagram.getLength();
				// TODO: send an ack??
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
			} catch(InterruptedIOException e) {
				continue;
			} catch(IOException e) {
				continue;
			} 
			//catch(Exception e) {
			//continue;
			//}
		}
		_socket.close();
	}

	/**
	 * Notifies the UDP socket that the port has been changed, requiring
	 * the UDP socket to be recreated.
	 *
	 * TODO: work on the threading
	 */
	public void resetPort() {
		if(_udpThread.isAlive()) _udpThread.interrupt();
		initialize();
	}


	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the <tt>port</tt> to send to
	 */
    public synchronized void send(Message msg, InetAddress ip, int port) {
		if(_socket == null) {
			throw new NullPointerException("socket null");
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			msg.write(baos);
		} catch(IOException e) {
			e.printStackTrace();
			// can't send the hit, so return
			return;
		}

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
		try {
            _socket.send(dg);
		} catch(IOException e) {
			e.printStackTrace();
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
