package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class DroppedSentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs a new <tt>DroppedSentMessageStatBytes</tt> instance.
	 */
	private DroppedSentMessageStatBytes() {}

	/**
	 * Private class for the total number of bytes in sent 
	 * UDP messages.
	 */
	private static class UDPDroppedSentMessageStatBytes 
		extends DroppedSentMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in sent 
	 * TCP messages.
	 */
	private static class TCPDroppedSentMessageStatBytes 
		extends DroppedSentMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}


	/**
	 * <tt>Statistic</tt> for all messages sent.
	 */
	public static final Statistic ALL_MESSAGES =
		new DroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new DroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new DroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new DroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessageStatBytes();
}
