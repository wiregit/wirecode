package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
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
public class UDPService implements Runnable {

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
     * Used for case where _socket is null on startup and the send thread
     * is trying to send but encounters null socket.  It should only report
     * if the socket has been set before (cuz then the socket will never be
     * null)
     */
    private boolean _socketSetOnce = false;

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
    
    /** The last time the _acceptedUnsolicitedIncoming was set.
     */
    private long _lastUnsolicitedIncomingTime = 0;

    /** The last time we sent a UDP Connect Back.
     */
    private long _lastConnectBackTime = System.currentTimeMillis();
    void resetLastConnectBackTime() {
        _lastConnectBackTime = 
             System.currentTimeMillis() - Acceptor.INCOMING_EXPIRE_TIME;
    }


	/**
	 * The thread for listening of incoming messages.
	 */
	private final Thread UDP_RECEIVE_THREAD;
	
	/**
	 * The queue that processes packets to send.
	 */
	private final ProcessingQueue SEND_QUEUE;

    /**
     * The GUID that we advertise out for UDPConnectBack requests.
     */
    private final GUID CONNECT_BACK_GUID = new GUID(GUID.makeGuid());

    /**
     * The GUID that we send for Pings, useful to test solicited support.
     */
    private final GUID SOLICITED_PING_GUID = new GUID(GUID.makeGuid());


    /**
     * The time between UDP pings.  Used by the PeriodicPinger.  This is
     * useful for nodes behind certain firewalls (notably the MS firewall).
     */
    private static final long PING_PERIOD = 85 * 1000;  // 85 seconds

	/**
	 * Instance accessor.
	 */
	public static UDPService instance() {
		return INSTANCE;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	protected UDPService() {	    
        UDP_RECEIVE_THREAD = new ManagedThread(this, "UDPService-Receiver");
        UDP_RECEIVE_THREAD.setDaemon(true);
        SEND_QUEUE = new ProcessingQueue("UDPService-Sender");
        RouterService.schedule(new IncomingValidator(), 
                               Acceptor.TIME_BETWEEN_VALIDATES,
                               Acceptor.TIME_BETWEEN_VALIDATES);
        if (CommonUtils.isWindowsXP())
            RouterService.schedule(new PeriodicPinger(), 0, PING_PERIOD);
    }
	
	/**
	 * Starts the UDP Service.
	 */
	public void start() {
        UDP_RECEIVE_THREAD.start();
    }
    
    /** @return The GUID to send for UDPConnectBack attempts....
     */
    public GUID getConnectBackGUID() {
        return CONNECT_BACK_GUID;
    }

    /** @return The GUID to send for Solicited Ping attempts....
     */
    public GUID getSolicitedGUID() {
        return SOLICITED_PING_GUID;
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
                if (_socket == null)
                    _socketSetOnce = true;
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
                if(!NetworkUtils.isValidAddress(datagram.getAddress()))
                    continue;
                if(!NetworkUtils.isValidPort(datagram.getPort()))
                    continue;
                
                byte[] data = datagram.getData();
                try {
                    // we do things the old way temporarily
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in, Message.N_UDP);
                    if(message == null) continue;                    
                    if (!isGUESSCapable()) {
                        if (message instanceof PingRequest) {
                            GUID guid = new GUID(message.getGUID());
                            if(isValidForIncoming(CONNECT_BACK_GUID, guid,
                                                  datagram))
                                _acceptedUnsolicitedIncoming = true;
                            _lastUnsolicitedIncomingTime =
                                System.currentTimeMillis();
                        }
                        else if (message instanceof PingReply) {
                            GUID guid = new GUID(message.getGUID());
                            if(isValidForIncoming(SOLICITED_PING_GUID, guid,
                                                  datagram))
                                _acceptedSolicitedIncoming = true;
                        }
                    }
                    // ReplyNumberVMs are always sent in an unsolicited manner,
                    // so we can use this fact to keep the last unsolicited up
                    // to date
                    if (message instanceof ReplyNumberVendorMessage)
                        _lastUnsolicitedIncomingTime = 
                            System.currentTimeMillis();
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
	 * Determines whether or not the specified message is valid for setting
	 * LimeWire as accepting UDP messages (solicited or unsolicited).
	 */
	private boolean isValidForIncoming(GUID match, GUID guidReceived,
	                                   DatagramPacket d) {
        if(!match.equals(guidReceived))
            return false;
            
	    String host = d.getAddress().getHostAddress();

        return !ConnectionSettings.LOCAL_IS_PRIVATE.getValue()  ||
               !RouterService.getConnectionManager().isConnectedTo(host);
    }
    
    /**
     * Sends the specified <tt>Message</tt> to the specified host.
     * 
     * @param msg the <tt>Message</tt> to send
     * @param host the host to send the message to
     */
    public void send(Message msg, IpPort host) {
        send(msg, host.getInetAddress(), host.getPort());
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
        } catch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorService.error(e);
            // can't send the hit, so return
            return;
        }

        byte[] data = baos.toByteArray();
        DatagramPacket dg = new DatagramPacket(data, data.length, ip, port);
        SEND_QUEUE.add(new Sender(dg, err));
	}
    
    // the runnable that actually sends the UDP packets.  didn't wany any
    // potential blocking in send to slow down the receive thread.  also allows
    // received packets to be handled much more quickly
    private class Sender implements Runnable {
        private final DatagramPacket _dp;
        private final ErrorCallback _err;
        
        Sender(DatagramPacket dp, ErrorCallback err) {
            _dp = dp;
            _err = err;
        }
        
        public void run() {
            // send away
            // ------
            synchronized (_sendLock) {
                // we could be changing ports, just drop the message, 
                // tough luck
                if (_socket == null) {
                    if (_socketSetOnce) {
                        Exception npe = 
                            new NullPointerException("Null UDP Socket!!");
                        ErrorService.error(npe);
                    }
                    return;
                }
                try {
                    _socket.send(_dp);
                } catch(BindException be) {
                    // oh well, if we can't bind our socket, ignore it.. 
                } catch(NoRouteToHostException nrthe) {
                    // oh well, if we can't find that host, ignore it ...
                } catch(IOException ioe) {
                    if(isIgnoreable(ioe.getMessage()))
                        return;
                        
                    String errString = "ip/port: " + 
                                       _dp.getAddress() + ":" + 
                                       _dp.getPort();
                    _err.error(ioe, errString);
                }
            }
        }
        
        /**
         * Determines whether or not the given IOException can
         * can be ignored.
         *
         * Visit http://www.dte.net/winsock_error.htm for explanations
         * of each code/message.
         *
         * Most of these have no meaning when applied to UDP, but
         * it doesn't hurt to check for ones that aren't harmful.
         * Depending on the version of Java or the OS, the error may
         * either be "Datagram send failed (code=<code>)"
         * or simply the text of the error.
         */
        private boolean isIgnoreable(final String message) {
            if(message == null)
                return false;

            // For easier comparison, make everything lowercase
            final String msg = message.toLowerCase();
            
            if(scan(msg, 10013, "permission denied"))
                return true;
            // propogate 10048 / Address already in use
            if(scan(msg, 10049, "cannot assign requested address"))
                return true;
            // propogate 10047 / Address family not supported by protocol family
            // propogate 10037 / Operation already in progress
            if(scan(msg, 10053, "software caused connection abort"))
                return true;
            if(scan(msg, 10054, "connection reset by peer"))
                return true;
            if(scan(msg, 10061, "connection refused"))
                return true;
            // propogate 10039 / Destination address required
            // propogate 10014 / Bad address
            if(scan(msg, 10064, "host is down"))
                return true;
            if(scan(msg, 10065, "no route to host"))
                return true;
            // propogate 10036 / Operation now in progress
            if(scan(msg, 10004, "interrupted function call"))
                return true;
            // propogate 10022 / Invalid Argument
            // propogate 10056 / Socket is already connected
            // propogate 10024 / Too many open files
            // propogate 10040 / Message too long
            if(scan(msg, 10050, "network is down"))
                return true;
            if(scan(msg, 10052, "network dropped connection on reset"))
                return true;
            if(scan(msg, 10051, "network is unreachable"))
                return true;
            if(scan(msg, 10055, "no buffer space available"))
                return true;
            // propogate 10042 / Bad protocol option
            // propogate 10057 / Socket is not connected
            // propogate 10038 / Socket operation on non-socket
            // propogate 10045 / Operation not supported
            // propogate 10046 / Protocl family not supported
            // propogate 10067 / Too many processes
            // propogate 10043 / Protocol not supported
            // propogate 10041 / Protocol wrong type for socket
            // propogate 10058 / Cannot send after socket shutdown
            // propogate 10044 / Socket type not supported
            if(scan(msg, 10060, "connection timed out"))
                return true;
            if(scan(msg, 10035, "resource temporarily unavailable"))
                return true;
            if(scan(msg, 11001, "host not found"))
                return true;
            if(scan(msg, 10091, "network subsystem is unavailable"))
                return true;
                
            // General invalid error on Linux
            if(msg.indexOf("operation not permitted") > -1)
                return true;
                
            return false;
        }
        
        /**
         * Scans the error message for either the code or the name of
         * of the message, returning true if either was found.
         */
        private boolean scan(final String msg, int code, final String name) {
            if(msg.indexOf("code="+code) > -1)
                return true;
            if(msg.indexOf(name) > -1)
                return true;
            return false;
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
	 * Sets whether or not this node is capable of receiving SOLICITED
     * UDP packets.  This is useful for testing UDPConnections.
	 *
	 */	
	public void setReceiveSolicited(boolean value) {
		_acceptedSolicitedIncoming = value;
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

    private static class MLImpl implements MessageListener {
        public boolean _gotIncoming = false;
        private final GUID _guid;
        public MLImpl(GUID guid) {
            _guid = guid;
        }

        public void processMessage(Message m) {
            if ((m instanceof PingRequest) &&
                (_guid.equals(new GUID(m.getGUID()))))
                _gotIncoming = true;
        }
    }

    private class IncomingValidator implements Runnable {
        public IncomingValidator() {}
        public void run() {
            // clear and revalidate if 1) we haven't had in incoming in an hour
            // or 2) we've never had incoming and we haven't checked in an hour
            final long currTime = System.currentTimeMillis();
            final MessageRouter mr = RouterService.getMessageRouter();
            final ConnectionManager cm = RouterService.getConnectionManager();
            if (
                (_acceptedUnsolicitedIncoming && //1)
                 ((currTime - _lastUnsolicitedIncomingTime) > 
                  Acceptor.INCOMING_EXPIRE_TIME)) 
                || 
                (!_acceptedUnsolicitedIncoming && //2)
                 ((currTime - _lastConnectBackTime) > 
                  Acceptor.INCOMING_EXPIRE_TIME))
                ) {
                
                final GUID cbGuid = new GUID(GUID.makeGuid());
                final MLImpl ml = new MLImpl(cbGuid);
                mr.registerMessageListener(cbGuid, ml);
                // send a connectback request to a few peers and clear
                if(cm.sendUDPConnectBackRequests(cbGuid))  {
                    _lastConnectBackTime = System.currentTimeMillis();
                    Runnable checkThread = new Runnable() {
                            public void run() {
                                if ((_acceptedUnsolicitedIncoming && 
                                     (_lastUnsolicitedIncomingTime < currTime))
                                    || (!_acceptedUnsolicitedIncoming))
                                    // we set according to the message listener
                                    _acceptedUnsolicitedIncoming = 
                                        ml._gotIncoming;
                                mr.unregisterMessageListener(cbGuid);
                            }
                        };
                    RouterService.schedule(checkThread, 
                                           Acceptor.WAIT_TIME_AFTER_REQUESTS,
                                           0);
                }
                else
                    mr.unregisterMessageListener(cbGuid);
            }
        }
    }

    private class PeriodicPinger implements Runnable {
        public void run() {
            // straightforward - send a UDP ping to a host.  it doesn't really
            // matter who the guy is - we are just sending to open up any
            // potential firewall to UDP traffic
            GUESSEndpoint ep = QueryUnicaster.instance().getUnicastEndpoint();
            if (ep == null) return;
            // only do this if you can receive some form of UDP traffic.
            if (!canReceiveSolicited() && !canReceiveUnsolicited()) return;

            // good to use the solicited guid
            PingRequest pr = new PingRequest(getSolicitedGUID().bytes(),
                                             (byte)1, (byte)0);
            send(pr, ep.getAddress(), ep.getPort());
        }
    }

}
