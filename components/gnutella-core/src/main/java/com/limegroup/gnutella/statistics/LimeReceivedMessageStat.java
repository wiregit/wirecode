package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been received from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages received over a specific number of time intervals, 
 * etc.  This class is specialized to only track messages received
 * from LimeWires.
 */
public class LimeReceivedMessageStat extends AbstractStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private LimeReceivedMessageStat() {}

	/**
	 * Private class for handling byte statistics.
	 */
	private static class LimeReceivedMessageStatBytes 
		extends AbstractKilobytesStatistic {}

	/**
	 * Private class for keeping track of filtered messages.
	 */
	private static class FilteredReceivedMessageStat 
		extends LimeReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of duplicate queries.
	 */
	private static class DuplicateQueriesReceivedMessageStat
		extends LimeReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_DUPLICATE_QUERIES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPReceivedMessageStat extends LimeReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_UDP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPReceivedMessageStat extends LimeReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_TCP_MESSAGES.incrementStat();
		}
	}


	///// BYTES STATISTICS -- override addData method /////

	/**
	 * Private class for keeping track of filtered messages, in bytes.
	 */
	private static class FilteredReceivedMessageStatBytes
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_FILTERED_MESSAGES_BYTES.addData(data);
		}
	}

	/**
	 * Private class for keeping track of duplicate queries, in bytes.
	 */
	private static class DuplicateQueriesReceivedMessageStatBytes
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_DUPLICATE_QUERIES_BYTES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * UDP messages.
	 */
	private static class UDPReceivedMessageStatBytes 
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES_BYTES.addData(data);
			ALL_UDP_MESSAGES_BYTES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * TCP messages.
	 */
	private static class TCPReceivedMessageStatBytes 
		extends AbstractKilobytesStatistic {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES_BYTES.addData(data);
			ALL_TCP_MESSAGES_BYTES.addData(data);
		}
	}

	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	public static final Statistic ALL_MESSAGES =
		new LimeReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic ALL_UDP_MESSAGES =
		new LimeReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic ALL_TCP_MESSAGES =
		new LimeReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new LimeReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all duplicate quereies.
	 */
	public static final Statistic ALL_DUPLICATE_QUERIES =
		new LimeReceivedMessageStat();



	/////// individual message stats ///////

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	public static final Statistic UDP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	public static final Statistic TCP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStat();


	////////////// START OF BYTE STATISTICS //////////////////

	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	public static final Statistic ALL_MESSAGES_BYTES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic ALL_UDP_MESSAGES_BYTES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic ALL_TCP_MESSAGES_BYTES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES_BYTES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all duplicate queries, in bytes.
	 */
	public static final Statistic ALL_DUPLICATE_QUERIES_BYTES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES_BYTES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES_BYTES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES_BYTES = 
	    new FilteredReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES_BYTES = 
	    new FilteredReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	public static final Statistic UDP_DUPLICATE_QUERIES_BYTES =
		new DuplicateQueriesReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	public static final Statistic TCP_DUPLICATE_QUERIES_BYTES =
		new DuplicateQueriesReceivedMessageStatBytes();
}
