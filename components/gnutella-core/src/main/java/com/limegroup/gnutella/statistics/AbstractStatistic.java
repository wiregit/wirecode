package com.limegroup.gnutella.statistics;

import com.sun.java.util.collections.*;

public abstract class AbstractStatistic implements Statistic {

	/**
	 * <tt>List</tt> of all message statistics classes, allowing
	 * them to be easily iterated over.
	 */
	protected static List ALL_STATS = new LinkedList();

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
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	protected AbstractStatistic() {
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
	public void incrementStat() {
		_current++;
		_total++;
	}

	/**
	 * Accessor for the array of stored data for this message.
	 *
	 * @return an array of <tt>Integer</tt> instances containing
	 *  the data for the stored history of this message
	 */
	public Integer[] getStatHistory() {
		_statHistory = (Integer[])STAT_HISTORY.toArray(new Integer[0]); 
		return _statHistory;
	}

	/**
	 * Stores the accumulated statistics for the current time period
	 * into the accumulated historical data.
	 */
	public void storeCurrentStat() {
		STAT_HISTORY.remove(0);
		STAT_HISTORY.add(new Integer(_current));
		_current = 0;
	}


	/**
	 * Stores the accumulated statistics for all messages into
	 * their collections of historical data.
	 */
	public static void storeCurrentStats() {
		Iterator iter = ALL_STATS.iterator();
		while(iter.hasNext()) {
			Statistic stat = (Statistic)iter.next();
			stat.storeCurrentStat();
		}
	}
}
