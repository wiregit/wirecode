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
     * LOCKING: Grab the _recieveLock before receiving.  grab the _sendLock
     * before sending.  Moreover, only one thread should be wait()ing on one of
     * these locks at a time or results cannot be predicted.
	 * This is the socket that handles sending and receiving messages over 
	 * UDP.
	 */
	private volatile DatagramSocket _socket;
	
    /**
     * Used for synchronized RECEIVE access to the UDP socket.  Should only be
     * used by the UDP_THREAD.
     */
    private final Object _receiveLock = new Object();

    /**
     * Used for synchronized SEND access to the UDP socket.  Should only be used
     * in the send method.
     */
    private final Object _sendLock = new Object();

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

	/**
	 * The thread for listening of incoming messages.
	 */
	private final Thread UDP_THREAD = new Thread(this);

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
     * Returns a new DatagramSocket that is bound to the given port.  This
     * value should be passed to setListeningSocket(DatagramSocket) to commit
     * to the new port.  If setListeningSocket is NOT called, you should close
     * the return socket.
     * @return a new DatagramSocket that is bound to the specified port.
     * @exception IOException Thrown if the DatagramSocket could not be
     * created.
     */
    DatagramSocket newListeningSocket(int port) throws IOException {
        try {
            return new DatagramSocket(port);
        }
        catch (SocketException se) {
            throw new IOException();
        }
        catch (SecurityException se) {
            throw new IOException();
        }
    }


	/** 
     * Changes the DatagramSocket used for sending/receiving.  Typically called
     * by Acceptor to commit to the new port.
     * @param datagramSocket the new listening socket, which must be be the
     * return value of newListeningSocket(int).  A value of null disables 
     * UDP sending and receiving.
	 */
	void setListeningSocket(DatagramSocket datagramSocket) {
        // we used to check if we were GUESS capable according to the
        // SettingsManager.  but in general we want to have the SERVER side of
        // GUESS active always.  the client side should be shut off from 
		// MessageRouter.
        if (!UDP_THREAD.isAlive()) {
			UDP_THREAD.setDaemon(true);
            UDP_THREAD.start();
		}

        //a) Close old socket (if non-null) to alert lock holders...
        if (_socket != null) 
            _socket.close();
        //b) Replace with new sock.  Notify the udpThread.
        synchronized (_receiveLock) {
            synchronized (_sendLock) {
                // if the input is null, then the service will shut off ;) .
                _socket = (DatagramSocket) datagramSocket;
                _receiveLock.notify();
                _sendLock.notify();
            }
        }
	}


	/**
	 * Busy loop that accepts incoming messages sent over UDP and 
	 * dispatches them to their appropriate handlers.
	 */
	public void run() {
        try {
            MessageRouter router = RouterService.getMessageRouter();
            byte[] datagramBytes = new byte[BUFFER_SIZE];
            
            while (true) {
                // prepare to receive
                DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                             BUFFER_SIZE);
                
                // when you first can, try to recieve a packet....
                // *----------------------------
                synchronized (_receiveLock) {
                    if (_socket == null) {
                        try {
                            _receiveLock.wait();
                        }
                        catch (InterruptedException ignored) {
                            continue;
                        }
                    }
                    try {
                        _socket.receive(datagram);
                    } 
                    catch(InterruptedIOException e) {
                        continue;
                    } 
                    catch(IOException e) {
                        continue;
                    } 
                }
                // ----------------------------*                
                // process packet....
                // *----------------------------
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
                }
                catch (IOException e) {
                    continue;
                }
                catch (BadPacketException e) {
                    continue;
                }
                // ----------------------------*
            }
        } catch(Throwable t) {
            RouterService.error(t);
        }
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
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
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
            e.printStackTrace();
            // can't send the hit, so return
            return;
        }

        byte[] data = baos.toByteArray();
        DatagramPacket dg = new DatagramPacket(data, data.length, ip, port);
        synchronized (_sendLock) {
            if(_socket == null) // just drop it, don't wait - FOR NOW.  when we
                                // thread this, we will wait...
                return;
            try {
                _socket.send(dg);
            } catch(IOException e) {
                e.printStackTrace();
            }
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
		if(_socket == null) return false;
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
