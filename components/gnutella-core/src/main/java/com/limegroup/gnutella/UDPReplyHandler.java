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
	 * Constructor that sets the ip and port to reply to.
	 *
	 * @param ip the <tt>InetAddress</tt> to reply to
	 * @param port the port to reply to
	 */
	public UDPReplyHandler(InetAddress ip, int port) {
		IP   = ip;
		PORT = port;
	}

	public void handlePingReply(PingReply pong, ReplyHandler handler) {
	}

	public void handleQueryReply(QueryReply hit, ReplyHandler handler) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			hit.write(baos);
		} catch(IOException e) {
			// can't send the hit, so return
			return;
		}

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, IP, PORT); 
		try {
			UDPAcceptor.instance().sendDatagram(dg);
		} catch(IOException e) {
			// not sure what to do here -- try again??
		}
		//SOCKET.send(dg);
	}

	public void handlePushRequest(PushRequest reques, ReplyHandler handlert) {
	}

	public void countDroppedMessage() {
	}

	public Set getDomains() {
		return EMPTY_SET;
	}

	public boolean isPersonalSpam(Message m) {
		// TODO: do something else here
        //return !_personalFilter.allow(m);
		return false;
	}

	public boolean isOpen() {

		// TODO:  really what we want??
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
	 * Overrides toString to print out more detailed information about
	 * this <tt>UDPReplyHandler</tt>
	 */
	public String toString() {
		return ("UDPReplyHandler:\r\n"+
				IP.toString()+"\r\n"+
				PORT+"\r\n");
	}
}
