package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.net.*;
import java.io.*;

/**
 * This class is an implementation of <tt>ReplyHandler</tt> that is 
 * specialized for handling UDP messages.
 */
public final class UDPReplyHandler implements ReplyHandler {

	/**
	 * Constant for the <tt>InetAddress</tt> of the host to reply to.
	 */
	private final InetAddress IP;

	/**
	 * Constant for the port of the host to reply to.
	 */
	private final int PORT;

	/**
	 * Constant for the empty set of security domains that this 
	 * <tt>ReplyHandler</tt> belongs to.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

	/**
	 * Constant for the <tt>UDPService</tt>.
	 */
	private static final UDPService UDP_SERVICE = UDPService.instance();
	
	/**
	 * Constructor that sets the ip and port to reply to.
	 *
	 * @param ip the <tt>InetAddress</tt> to reply to
	 * @param port the port to reply to
	 */
	public UDPReplyHandler(InetAddress ip, int port) {
		IP   = ip;
		PORT = port;
	}

	
	/**
	 * Sends the <tt>PingReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>PingReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handlePingReply(PingReply pong, ReplyHandler handler) {
		UDP_SERVICE.send(pong, IP, PORT);
	}

	/**
	 * Sends the <tt>QueryReply</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param hit the <tt>QueryReply</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handleQueryReply(QueryReply hit, ReplyHandler handler) {
		UDP_SERVICE.send(hit, IP, PORT);
	}

	/**
	 * Sends the <tt>QueryRequest</tt> via a UDP datagram to the IP and port
	 * for this handler.<p>
	 *
	 * Implements <tt>ReplyHandler</tt>.
	 *
	 * @param request the <tt>QueryRequest</tt> to send
	 * @param handler the <tt>ReplyHandler</tt> to use for sending the reply
	 */
	public void handlePushRequest(PushRequest request, ReplyHandler handler) {
		UDP_SERVICE.send(request, IP, PORT);
	}

	public void countDroppedMessage() {}

	public Set getDomains() {
		return EMPTY_SET;
	}

	public boolean isPersonalSpam(Message m) {
		// TODO: do something else here
        //return !_personalFilter.allow(m);
		return false;
	}

	public boolean isOpen() {
		return true;
	}

	public int getNumMessagesReceived() {
		return 0;
	}

	public boolean isOutgoing() {
		return false;
	}

	public boolean isKillable() {
		return false;
	}

	/**
	 * Implements <tt>ReplyHandler</tt>.  This always returns <tt>false</tt>
	 * for UDP reply handlers, as leaves are always connected via TCP.
	 *
	 * @return <tt>false</tt>, as all leaves are connected via TCP, so
	 *  directly connected leaves will not have <tt>UDPReplyHandler</tt>s
	 */
	public boolean isSupernodeClientConnection() {
		return false;
	}

	/**
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	public String toString() {
		return ("UDPReplyHandler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
}
