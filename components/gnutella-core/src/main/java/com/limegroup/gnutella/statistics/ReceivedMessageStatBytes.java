package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * Class for recording all received message statistics by the number
 * of bytes transferred.
 */
public class ReceivedMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs a new <tt>ReceivedMessageStatBytes</tt> instance.
	 */
	private ReceivedMessageStatBytes() {}

	/**
	 * Private class for keeping track of filtered messages, in bytes.
	 */
	private static class FilteredReceivedMessageStat
		extends ReceivedMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_FILTERED_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for keeping track of duplicate queries, in bytes.
	 */
	private static class DuplicateQueriesReceivedMessageStat
		extends ReceivedMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_DUPLICATE_QUERIES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * UDP messages.
	 */
	private static class UDPReceivedMessageStat
		extends ReceivedMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * TCP messages.
	 */
	private static class TCPReceivedMessageStat
		extends ReceivedMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}


	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	public static final Statistic ALL_MESSAGES =
		new ReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new ReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new ReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new ReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all duplicate queries, in bytes.
	 */
	public static final Statistic ALL_DUPLICATE_QUERIES =
		new ReceivedMessageStatBytes();

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

}
