package com.limegroup.gnutella.statistics;

import com.sun.java.util.collections.*;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages.  Each statistic maintains its 
 * own history, all messages received over a specific number of
 * time intervals.
 */
public abstract class MessageStat implements Statistic {

	/**
	 * <tt>List</tt> of all message statistics classes, allowing
	 * them to be easily iterated over.
	 */
	private static List ALL_STATS = new LinkedList();

	/**
	 * List of all statistics stored over intervals for this
	 * specific <tt>MessageStat</tt> instance.
	 */
	private final List STAT_HISTORY = new LinkedList();

	/**
	 * Long for the statistic currently being added to.
	 */
	protected int _current = 0;

	/**
	 * Variable for the array of <tt>Integer</tt> instance for the
	 * history of statistics for this message.  Each 
	 * <tt>Integer</tt> stores the statistic for one time interval.
	 */
	private Integer[] _statHistory;

	/**
	 * Variable for the total number of messages received for this 
	 * statistic.
	 */
	protected long _total = 0;

	/**
	 * Constant for the number of records to hold for each statistic.
	 */
	public static final int HISTORY_LENGTH = 120;

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private MessageStat() {
		for(int i=0; i<HISTORY_LENGTH; i++) {
			STAT_HISTORY.add(new Integer(0));
		}
		ALL_STATS.add(this);
	}


	/**
	 * Accessor for the total number of this statistic type received so 
	 * far.
	 *
	 * @return the total number of this message type received so far
	 */
	public long getTotal() {
		return _total;
	}

	/**
	 * Increments the statistic by one.
	 */
	public abstract void incrementStat();

	/**
	 * Accessor for the array of stored data for this message.
	 *
	 * @return an array of <tt>Integer</tt> instances containing
	 *  the data for the stored history of this message
	 */
	public Integer[] getStatHistory() {
		return _statHistory;
	}

	/**
	 * Stores the accumulated statistics for the current time period
	 * into the accumulated historical data.
	 */
	private void storeCurrentStat() {
		STAT_HISTORY.remove(0);
		STAT_HISTORY.add(new Integer(_current));
		_statHistory = (Integer[])STAT_HISTORY.toArray(new Integer[0]); 
		_current = 0;
	}


	/**
	 * Stores the accumulated statistics for all messages into
	 * their collections of historical data.
	 */
	public static void storeCurrentStats() {
		Iterator iter = ALL_STATS.iterator();
		while(iter.hasNext()) {
			MessageStat stat = (MessageStat)iter.next();
			stat.storeCurrentStat();
		}
	}


	/**
	 * Private class for all messages of a specific type, allowing
	 * storing of more general messages to still follow the same 
	 * generic <tt>MessageStat</tt> API.
	 */
	private static class GeneralMessageStat extends MessageStat {
		public void incrementStat() {
			_current++;
			_total++;
		}
	}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
	private static class RouteErrorMessageStat extends MessageStat {
		public void incrementStat() {
			_current++;
			_total++;
			ALL_ROUTE_ERRORS.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
	private static class FilteredMessageStat extends MessageStat {
		public void incrementStat() {
			_current++;
			_total++;
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for UDP messages.
	 */
	private static class UDPMessageStat extends MessageStat {
		public void incrementStat() {
			_current++;
			_total++;
			ALL_MESSAGES.incrementStat();
			ALL_UDP_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for TCP messages.
	 */
	private static class TCPMessageStat extends MessageStat {
		public void incrementStat() {
			_current++;
			_total++;
			ALL_MESSAGES.incrementStat();
			ALL_TCP_MESSAGES.incrementStat();
		}
	}

	/**
	 * <tt>MessageStat</tt> for all messages received.
	 */
	public static final MessageStat ALL_MESSAGES =
		new GeneralMessageStat();

	/**
	 * <tt>MessageStat</tt> for all UPD messages received.
	 */
	public static final MessageStat ALL_UDP_MESSAGES =
		new GeneralMessageStat();

	/**
	 * <tt>MessageStat</tt> for all TCP messages received.
	 */
	public static final MessageStat ALL_TCP_MESSAGES =
		new GeneralMessageStat();

	/**
	 * <tt>MessageStat</tt> for all route errors.
	 */
	public static final MessageStat ALL_ROUTE_ERRORS =
		new GeneralMessageStat();

	/**
	 * <tt>MessageStat</tt> for all filtered messages.
	 */
	public static final MessageStat ALL_FILTERED_MESSAGES =
		new GeneralMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella pings received over UDP.
	 */
	public static final MessageStat UDP_PING_REQUESTS = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella pings received over TCP.
	 */
	public static final MessageStat TCP_PING_REQUESTS = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella pongs received over UDP.
	 */
	public static final MessageStat UDP_PING_REPLIES = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella pongs received over TCP.
	 */
	public static final MessageStat TCP_PING_REPLIES = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public static final MessageStat UDP_QUERY_REQUESTS = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public static final MessageStat TCP_QUERY_REQUESTS = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final MessageStat UDP_QUERY_REPLIES = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final MessageStat TCP_QUERY_REPLIES = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final MessageStat UDP_PUSH_REQUESTS = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final MessageStat TCP_PUSH_REQUESTS = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final MessageStat UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final MessageStat TCP_ROUTE_TABLE_MESSAGES = 
	    new TCPMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final MessageStat UDP_FILTERED_MESSAGES = 
	    new FilteredMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final MessageStat TCP_FILTERED_MESSAGES = 
	    new FilteredMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella ping routing errors.
	 */
	public static final MessageStat PING_REPLY_ROUTE_ERRORS = 
	    new RouteErrorMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella query reply routing errors.
	 */
	public static final MessageStat QUERY_REPLY_ROUTE_ERRORS = 
	    new RouteErrorMessageStat();

	/**
	 * <tt>MessageStat</tt> for Gnutella push routing errors.
	 */
	public static final MessageStat PUSH_REQUEST_ROUTE_ERRORS = 
	    new RouteErrorMessageStat();

}
