package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent to other nodes
 * on the network.  Each statistic maintains its own history, all 
 * messages received over a specific number of time intervals, etc.
 */
public class SentMessageStat extends AbstractStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private SentMessageStat() {}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
	private static class RouteErrorSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_ROUTE_ERRORS.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
	private static class FilteredSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for UDP messages.
	 */
	private static class UDPSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			ALL_UDP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for TCP messages.
	 */
	private static class TCPSentMessageStat extends SentMessageStat {
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
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic ALL_UDP_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic ALL_TCP_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all route errors.
	 */
	public static final Statistic ALL_ROUTE_ERRORS =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella ping routing errors.
	 */
	public static final Statistic PING_REPLY_ROUTE_ERRORS = 
	    new RouteErrorSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query reply routing errors.
	 */
	public static final Statistic QUERY_REPLY_ROUTE_ERRORS = 
	    new RouteErrorSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push routing errors.
	 */
	public static final Statistic PUSH_REQUEST_ROUTE_ERRORS = 
	    new RouteErrorSentMessageStat();

}
