package com.limegroup.gnutella;

import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.messages.*;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * This class allows the creation of a UDPService instances with 
 * controlled delay times and loss rates for testing UDP communication.
 * It routes outgoing messages to itself after the delay time.
 */
public final class UDPServiceStub extends UDPService {

	/**
	 * Constant for the single <tt>UDPService</tt> instance.
	 */
	private final static UDPService INSTANCE1 = new UDPService();
    
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 1024 * 32;
    

	/**
	 * The thread for listening of incoming messages.
	 */
	private final Thread UDP_RECEIVE_THREAD;
	
	/**
	 * Instance accessor.
	 */
	public static UDPServiceStub instance() {
		return INSTANCE1;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	private UDPServiceStub() {	    
        UDPSTUB_RECEIVE_THREAD1 = 
            new ManagedThread(this,"UDPService-Receiver1");
        UDP_RECEIVE_THREAD1.setDaemon(true);

        SEND_QUEUE = new ProcessingQueue("UDPService-Sender");
        RouterService.schedule(new IncomingValidator(), 
                               Acceptor.TIME_BETWEEN_VALIDATES,
                               Acceptor.TIME_BETWEEN_VALIDATES);
    }
	
	/**
	 * 
	 */
	public void start() {
        UDP_RECEIVE_THREAD.start();
    }
    
    /** 
     */
    public GUID getConnectBackGUID() {
        return null;
    }

    /** 
     */
    public GUID getSolicitedGUID() {
        return null;
    }

    /** 
     */
    DatagramSocket newListeningSocket(int port) throws IOException {
        throw new IOException("no one should be calling me :"+port);
    }


	/** 
     *  Does nothing
	 */
	void setListeningSocket(DatagramSocket datagramSocket) {
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
    }


	/**
	 */	
	public boolean isGUESSCapable() {
		return false;
	}

	/**
	 * Returns whether or not this node is capable of receiving UNSOLICITED
     * UDP packets.  
	 */	
	public boolean canReceiveUnsolicited() {
		return false;
	}

	/**
	 * Returns whether or not this node is capable of receiving SOLICITED
     * UDP packets.  
	 */	
	public boolean canReceiveSolicited() {
		return true;
	}

	/**
	 */	
	public void setReceiveSolicited(boolean value) {
	}

	/**
	 */
	public boolean isListening() {
        return true;
	}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 */
	public String toString() {
		return "UDPServerStub\r\n loopback";
	}
}
