package com.limegroup.gnutella;

import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.statistics.*;

import java.net.*;
import java.io.*;

/**
 * This class handles UDP messaging services.  It both sends and
 * receives messages, routing received messages to their appropriate
 * handlers.  This also handles issues related to the GUESS proposal, 
 * such as making sure that the UDP and TCP port match and sending
 * UDP acks for queries.
 *
 * @see UDPReplyHandler
 * @see MessageRouter
 * @see QueryUnicaster
 */
public final class UDPService implements Runnable {

	/**
	 * Constant for the single <tt>UDPService</tt> instance.
	 */
	private static final UDPService INSTANCE = new UDPService();

	/**
	 * The socket that handles sending and receiving messages over UDP.
	 */
	private volatile DatagramSocket _socket;
	
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 1024 * 32;

	/**
	 * Variable for whether or not this node is capable of running its
	 * own GUESS-style searches, dependent upon whether or not it
	 * has successfully received an incoming UDP packet.
	 */
	private boolean _isGUESSCapable = false;

	//private final int SOCKET_TIMEOUT = 2*1000;

	/**
	 * The thread for listening of incoming messages.
	 */
	private Thread _udpThread = new Thread(this);

	/**
	 * Cached <tt>QueryUnicaster</tt> instnace.
	 */
	private QueryUnicaster UNICASTER = QueryUnicaster.instance();

	/**
	 * Instance accessor.
	 */
	public static UDPService instance() {
		return INSTANCE;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	private UDPService() {}


	/**
	 * If the UDP listening socket thread is not already running, this
	 * starts the thread, listening for incoming Gnutella messages
	 * over UDP.
	 */
	public void start() {
		// if we're already listening, return
		if(_udpThread.isAlive()) return;
        
        if (SettingsManager.instance().getGuessEnabled()) {
            _udpThread.setDaemon(true);		
            _udpThread.start();
        }
	}

	/**
	 * Busy loop that accepts incoming messages sent over UDP and 
	 * dispatches them to their appropriate handlers.
	 */
	public void run() {
		while(RouterService.getPort() == -1) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				return;
			}
		}
		int port = RouterService.getPort();
		try {
			_socket = new DatagramSocket(port);
			//_socket.setSoTimeout(SOCKET_TIMEOUT);
		} catch(SocketException e) {
			e.printStackTrace();
			return;
		}		
		
		MessageRouter router = RouterService.getMessageRouter();
		byte[] datagramBytes = new byte[BUFFER_SIZE];

		while(port == RouterService.getPort()) {
			try {
                DatagramPacket datagram = 
                    new DatagramPacket(datagramBytes, BUFFER_SIZE);
				_socket.receive(datagram);
				_isGUESSCapable = true;
				byte[] data = datagram.getData();
				int length = datagram.getLength();
				try {
					// we do things the old way temporarily
					InputStream in = new ByteArrayInputStream(data);
					Message message = Message.read(in);		
					if(message == null) continue;
                    if (message instanceof QueryRequest)
                        sendAcknowledgement(datagram, message.getGUID());
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
	 * Sends an ack back to the GUESS client node.  
	 */
	private void sendAcknowledgement(DatagramPacket datagram, byte[] guid) {
		ConnectionManager manager = RouterService.getConnectionManager();
		Endpoint host = manager.getConnectedGUESSUltrapeer();
		PingReply reply;
		if(host != null) {
			try {
				reply = new PingReply(guid, (byte)1,
									  host.getPort(),
									  host.getHostBytes(),
									  (long)0, (long)0, true);
			} catch(UnknownHostException e) {
				reply = createPingReply(guid);
			}
		} else {
			reply = createPingReply(guid);
		}

		send(reply, datagram.getAddress(), datagram.getPort());
		SentMessageStatHandler.UDP_PING_REPLIES.addMessage(reply);
	}

	/**
	 * Creates a new <tt>PingReply</tt> from the set of cached
	 * GUESS endpoints, or a <tt>PingReply</tt> for localhost
	 * if no GUESS endpoints are available.
	 */
	private PingReply createPingReply(byte[] guid) {
		GUESSEndpoint endpoint = UNICASTER.getUnicastEndpoint();
		if(endpoint == null) {
			return new PingReply(guid, (byte)1,
								 RouterService.getPort(),
								 RouterService.getAddress(),
								 RouterService.getNumSharedFiles(),
								 RouterService.getSharedFileSize()/1024,
								 RouterService.isSupernode(),
								 Statistics.instance().calculateDailyUptime());		
		} else {
			return new PingReply(guid, (byte)1,
								 endpoint.getPort(),
								 endpoint.getAddress().getAddress(),
								 0, 0, true, 0);
		}
	}

	/**
	 * Notifies the UDP socket that the port has been changed, requiring
	 * the UDP socket to be recreated.
	 */
	public synchronized void resetPort() {
		if(_udpThread.isAlive()) _udpThread.interrupt();
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
	 * Returns whether or not this node is capable of sending its own
	 * GUESS queries.  This would not be the case only if this node
	 * has not successfully received an incoming UDP packet.
	 *
	 * @return <tt>true</tt> if this node is capable of running its own
	 *  GUESS queries, <tt>false</tt> otherwise
	 */	
	public boolean isGUESSCapable() {
		return _isGUESSCapable;
	}

	/**
	 * Returns whether or not the UDP socket is listening for incoming
	 * messsages.
	 *
	 * @return <tt>true</tt> if the UDP socket is listening for incoming
	 *  UDP messages, <tt>false</tt> otherwise
	 */
	public boolean isListening() {
		return (_socket.getLocalPort() != -1);
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
