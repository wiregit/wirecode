package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been received from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages received over a specific number of time intervals, 
 * etc.
 */
public class ReceivedMessageStat extends AbstractStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private ReceivedMessageStat() {}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
//  	private static class RouteErrorReceivedMessageStat extends ReceivedMessageStat {
//  		public void incrementStat() {
//  			super.incrementStat();
//  			ALL_ROUTE_ERRORS.incrementStat();
//  		}
//  	}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
	private static class FilteredReceivedMessageStat extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for UDP messages.
	 */
	private static class UDPReceivedMessageStat extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_UDP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for TCP messages.
	 */
	private static class TCPReceivedMessageStat extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_TCP_MESSAGES.incrementStat();
		}
	}

	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	public static final Statistic ALL_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic ALL_UDP_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic ALL_TCP_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all route errors.
	 */
//  	public static final Statistic ALL_ROUTE_ERRORS =
//  		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new ReceivedMessageStat();

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
	 * <tt>Statistic</tt> for Gnutella ping routing errors.
	 */
//  	public static final Statistic PING_REPLY_ROUTE_ERRORS = 
//  	    new RouteErrorReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query reply routing errors.
	 */
//  	public static final Statistic QUERY_REPLY_ROUTE_ERRORS = 
//  	    new RouteErrorReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push routing errors.
	 */
//  	public static final Statistic PUSH_REQUEST_ROUTE_ERRORS = 
//  	    new RouteErrorReceivedMessageStat();

}
