package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class LimeSentMessageStat extends AbstractStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private LimeSentMessageStat() {}

	/**
	 * Private class for handling byte statistics.
	 */
	private static class LimeSentMessageStatBytes 
		extends AbstractKilobytesStatistic {}

	/**
	 * Private class for keeping track of filtered messages.
	 */
	private static class FilteredLimeSentMessageStat 
		extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPLimeSentMessageStat extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_UDP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPLimeSentMessageStat extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_TCP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of filtered messages, in bytes.
	 */
	private static class FilteredLimeSentMessageStatBytes
		extends AbstractKilobytesStatistic {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES_BYTES.incrementStat();
		}
	}

	/**
	 * Private class for the total number of bytes in sent 
	 * UDP messages.
	 */
	private static class UDPLimeSentMessageStatBytes 
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES_BYTES.addData(data);
			ALL_UDP_MESSAGES_BYTES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in sent 
	 * TCP messages.
	 */
	private static class TCPLimeSentMessageStatBytes 
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES_BYTES.addData(data);
			ALL_TCP_MESSAGES_BYTES.addData(data);
		}
	}

	/**
	 * <tt>Statistic</tt> for all messages sent.
	 */
	public static final Statistic ALL_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic ALL_UDP_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic ALL_TCP_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();


	////////////// START OF BYTE STATISTICS //////////////////

	/**
	 * <tt>Statistic</tt> for all messages sent.
	 */
	public static final Statistic ALL_MESSAGES_BYTES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic ALL_UDP_MESSAGES_BYTES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic ALL_TCP_MESSAGES_BYTES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES_BYTES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES_BYTES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES_BYTES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES_BYTES = 
	    new FilteredLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES_BYTES = 
	    new FilteredLimeSentMessageStatBytes();
}
