package com.limegroup.gnutella;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.messages.*;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

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
 *
 */
public final class UDPService implements Runnable {

	/**
	 * Constant for the single <tt>UDPService</tt> instance.
	 */
	private final static UDPService INSTANCE = new UDPService();
    
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
    
    /** True if the UDPService has ever received a solicited incoming UDP
     *  packet.
     */
    private boolean _acceptedSolicitedIncoming = false;
    
    /** True if the UDPService has ever received a unsolicited incoming UDP
     *  packet.
     */
    private boolean _acceptedUnsolicitedIncoming = false;
    
	/**
	 * The thread for listening of incoming messages.
	 */
	private final Thread UDP_RECEIVE_THREAD;
    
    /**
     * The thread for sending of outgoing messages.  Useful because
     * we don't want the receive thread to block while processing
     * messages....
     */
    private final Thread UDP_SEND_THREAD;

    /**
     * Used for communication between send() calls and the send thread.
     */ 
    private final List PACKETS_TO_SEND;

    /**
     * The GUID that we advertise out for UDPConnectBack requests.
     */
    private final GUID CONNECT_BACK_GUID = new GUID(GUID.makeGuid());

	/**
	 * Instance accessor.
	 */
	public static UDPService instance() {
		return INSTANCE;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	private UDPService() {	    
        PACKETS_TO_SEND = new LinkedList();
        UDP_RECEIVE_THREAD = new Thread(this, "UDPService-Receiver");
        UDP_RECEIVE_THREAD.setDaemon(true);
        UDP_SEND_THREAD = new Thread(new Sender(), "UDPService-Sender");
        UDP_SEND_THREAD.setDaemon(true);
    }
	
	/**
	 * Starts the UDP Service.
	 */
	public void start() {
        UDP_RECEIVE_THREAD.start();
        UDP_SEND_THREAD.start();
    }
    
    /** @return The GUID to send for UDPConnectBack attempts....
     */
    public GUID getConnectBackGUID() {
        return CONNECT_BACK_GUID;
    }

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
            throw new IOException("socket could not be set on port: "+port);
        }
        catch (SecurityException se) {
            throw new IOException("security exception on port: "+port);
        }
    }


	/** 
     * Changes the DatagramSocket used for sending/receiving.  Typically called
     * by Acceptor to commit to the new port.
     * @param datagramSocket the new listening socket, which must be be the
     *  return value of newListeningSocket(int).  A value of null disables 
     *  UDP sending and receiving.
	 */
	void setListeningSocket(DatagramSocket datagramSocket) {
        // we used to check if we were GUESS capable according to the
        // SettingsManager.  but in general we want to have the SERVER side of
        // GUESS active always.  the client side should be shut off from 
		// MessageRouter.

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
            byte[] datagramBytes = new byte[BUFFER_SIZE];
            MessageRouter router = RouterService.getMessageRouter();
            while (true) {
                // prepare to receive
                DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                             BUFFER_SIZE);
                
                // when you first can, try to recieve a packet....
                // *----------------------------
                synchronized (_receiveLock) {
                    while (_socket == null) {
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
                byte[] data = datagram.getData();
                try {
                    // we do things the old way temporarily
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in, Message.N_UDP);
                    if(message == null) continue;                    
                    if (!isGUESSCapable()) {
                        if (message instanceof PingRequest) {
                            GUID guidReceived = new GUID(message.getGUID());
                            if (CONNECT_BACK_GUID.equals(guidReceived))
                                _acceptedUnsolicitedIncoming = true;
                        }
                        else
                            _acceptedSolicitedIncoming = true;
                    }
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
            ErrorService.error(t);
        }
	}

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
     *
     * If sending fails for reasons such as a BindException,
     * NoRouteToHostException or specific IOExceptions such as
     * "No buffer space available", this message is silently dropped.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
	 */
    public void send(Message msg, InetAddress ip, int port) 
        throws IllegalArgumentException {
        send(msg, ip, port, ErrorService.getErrorCallback());
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
     *
     * If sending fails for reasons such as a BindException,
     * NoRouteToHostException or specific IOExceptions such as
     * "No buffer space available", this message is silently dropped.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
     * @param err  an <tt>ErrorCallback<tt> if you want to be notified errors
     * @throws IllegalArgumentException if msg, ip, or err is null.
	 */
    public void send(Message msg, InetAddress ip, int port, ErrorCallback err) 
        throws IllegalArgumentException {
        if (err == null)
            throw new IllegalArgumentException("Null ErrorCallback");
        if (msg == null)
            throw new IllegalArgumentException("Null Message");
        if (ip == null)
            throw new IllegalArgumentException("Null InetAddress");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("Invalid Port: " + port);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } 
        catch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorService.error(e);
            // can't send the hit, so return
            return;
        }

        byte[] data = baos.toByteArray();
        DatagramPacket dg = new DatagramPacket(data, data.length, ip, port);
        synchronized (PACKETS_TO_SEND) {
            if (PACKETS_TO_SEND.isEmpty())
                PACKETS_TO_SEND.notify();
            PACKETS_TO_SEND.add(new SendBundle(dg, err));
        }
	}

    // Just a simple container class
    private class SendBundle {
        DatagramPacket _dp;
        ErrorCallback _err;
        public SendBundle(DatagramPacket dp, ErrorCallback custom) {
            _dp = dp;
            _err = custom;
        }
    }
    
    // the runnable that actually sends the UDP packets.  didn't wany any
    // potential blocking in send to slow down the receive thread.  also allows
    // received packets to be handled much more quickly
    private class Sender implements Runnable {
        
        public void run() {
            SendBundle currBundle = null;
            while (true) {

                // get something to send
                // ------
                synchronized (PACKETS_TO_SEND) {
                    while (PACKETS_TO_SEND.isEmpty()) {
                        try {
                            PACKETS_TO_SEND.wait();
                        }
                        catch (InterruptedException ignored) {}
                    }
                    currBundle = (SendBundle) PACKETS_TO_SEND.remove(0);
                }
                // ------

                // send away
                // ------
                synchronized (_sendLock) {
                    // we could be changing ports, just drop the message, 
                    //tough luck
                    if (_socket == null) 
                        return;
                    try {
                        _socket.send(currBundle._dp);
                    } 
                    catch(BindException be) {
                        // oh well, if we can't bind our socket, ignore it.. 
                    } 
                    catch(NoRouteToHostException nrthe) {
                        // oh well, if we can't find that host, ignore it ...
                    } 
                    catch(IOException ioe) {
                        //If we're full, just drop it.  UDP is unreliable like 
                        //that.
                        if("No buffer space available".equals(ioe.getMessage()))
                            return;
                        String errString = "ip/port: " + 
                                           currBundle._dp.getAddress() + ":" + 
                                           currBundle._dp.getPort();
                        currBundle._err.error(ioe, errString);
                    }
                }
                // ------

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
		return canReceiveUnsolicited() && canReceiveSolicited();
	}

	/**
	 * Returns whether or not this node is capable of receiving UNSOLICITED
     * UDP packets.  It is false until a UDP ConnectBack ping has been received.
	 *
	 * @return <tt>true</tt> if this node has accepted a UNSOLICITED UDP packet.
	 */	
	public boolean canReceiveUnsolicited() {
		return _acceptedUnsolicitedIncoming;
	}

	/**
	 * Returns whether or not this node is capable of receiving SOLICITED
     * UDP packets.  
	 *
	 * @return <tt>true</tt> if this node has accepted a SOLICITED UDP packet.
	 */	
	public boolean canReceiveSolicited() {
		return _acceptedSolicitedIncoming;
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
