package com.limegroup.gnutella;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.messages.*;
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
 *
 * //TODO: We need to create a Worker Thread that actually does a send() and
 * frees up the run() method from potentially blocking send() calls.  At that
 * point we should stop catching and discarding all exceptions....
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
	private final Thread UDP_THREAD;

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
        UDP_THREAD = new Thread(this, "UDPService");
		UDP_THREAD.setDaemon(true);
    }
	
	/**
	 * Starts the UDP Service.
	 */
	public void start() {
        UDP_THREAD.start();
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
    public synchronized void send(Message msg, InetAddress ip, int port) 
      throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } catch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorService.error(e);

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
            } catch(BindException be) {
                // oh well, if we can't bind our socket, ignore it.. 
                return;
            } catch(NoRouteToHostException nrthe) {
                // oh well, if we can't find that host, ignore it ...
                return;
            } catch(IOException ioe) {
                //If we're full, just drop it.  UDP is unreliable like that.
                if( "No buffer space available".equals(ioe.getMessage()) )
                    return;
                // there seems to be a windows specific java issue with UDP
                // stuff, though i've never witnessed it meself. since this is
                // UDP and unreliable, continually throwing the exception prolly
                // isn't necessary
                // TODO: see major TODO up top....
                if( CommonUtils.isWindows() &&
                    (ioe.getMessage() != null) &&
                    (ioe.getMessage().indexOf("Datagram send failed") >= 0) )
                    return;
                throw ioe;
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
