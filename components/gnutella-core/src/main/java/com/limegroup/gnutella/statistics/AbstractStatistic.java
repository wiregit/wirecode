package com.limegroup.gnutella.statistics;

import com.sun.java.util.collections.*;

/**
 * This class provides a default implementation of the <tt>Statistic</tt>
 * interface, providing such functionality as keeping track of the
 * history for the given statistic, providing access to the average
 * value, the maximum value, etc.
 */
public abstract class AbstractStatistic implements Statistic {

	/**
	 * <tt>List</tt> of all message statistics classes, allowing
	 * them to be easily iterated over.
	 */
	protected static List ALL_STATS = new LinkedList();

	/**
	 * List of all statistics stored over intervals for this
	 * specific <tt>Statistic</tt> instance.
	 */
	private final List STAT_HISTORY = new LinkedList();

	/**
	 * Long for the statistic currently being added to.
	 */
	protected int _current = 0;

	/**
	 * Variable for the array of <tt>Integer</tt> instances for the
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
	 * The total number of stats recorded.
	 */
	protected int _totalStatsRecorded = 0;

	/**
	 * The maximum value ever recorded for any time period.
	 */
	protected int _max = 0;

	/**
	 * Constructs a new <tt>Statistic</tt> instance with 0 for all 
	 * historical data fields.
	 */
	protected AbstractStatistic() {
		for(int i=0; i<HISTORY_LENGTH; i++) {
			STAT_HISTORY.add(new Integer(0));
		}
		ALL_STATS.add(this);
	}

	// inherit doc comment
	public long getTotal() {
		return _total;
	}

	// inherit doc comment
	public float getAverage() {
		return _total/_totalStatsRecorded;
	}

	// inherit doc comment
	public int getMax() {
		return _max;
	}

	// inherit doc comment
	public void incrementStat() {
		_current++;
		_total++;		
	}

	// inherit doc comment
	public void addData(int data) {
		_current += data;
		_total += data;
	}
		
	// inherit doc comment
	public Integer[] getStatHistory() {
		_statHistory = (Integer[])STAT_HISTORY.toArray(new Integer[0]); 
		return _statHistory;
	}

	// inherit doc comment
	public void storeCurrentStat() {
		STAT_HISTORY.remove(0);
		STAT_HISTORY.add(new Integer(_current));
		if(_current > _max) {
			_max = _current;
		}
		_current = 0;
		_totalStatsRecorded++;
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
