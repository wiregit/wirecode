package com.limegroup.gnutella.statistics;

/**
 * Interface for generalized access to a <tt>Statistic</tt>.
 */
public interface Statistic {

	/**
	 * Constant for the number of records to hold for each statistic.
	 */
	public static final int HISTORY_LENGTH = 200;

	/**
	 * Accessor for the total number of this statistic recorded.
	 *
	 * @return the total of this statistic recorded, regardless of any
	 *  time increments
	 */
	long getTotal();

	/**
	 * Increments this statistic by one.
	 */
	void incrementStat();

	/**
	 * Accessor for the <tt>Integer</tt> array of all statistics recorded
	 * over a discrete interval.  Note that this has a finite size, so only
	 * a fixed size array will be returned.
	 *
	 * @return the <tt>Integer</tt> array for all statistics recorded for
	 *  this statistic
	 */
	Integer[] getStatHistory();	

	/**
	 * Stores the current set of gathered statistics into the history set,
	 * setting the currently recorded data back to zero.
	 */
	void storeCurrentStat();
}
