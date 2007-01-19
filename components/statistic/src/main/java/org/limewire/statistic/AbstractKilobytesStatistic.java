package org.limewire.statistic;


/**
 * Specialized class for Gnutella message statistics.  This class modifies
 * data accessors to return data in kilobits instead of bytes.  To 
 * preserve data accuracy, data is stored in bytes and converted to 
 * kilobits.  If we did not do this, data would be lost.
 */
public abstract class AbstractKilobytesStatistic extends AbstractStatistic {

	/**
	 * Bytes per kilobyte for conversion convenience.
	 */
	private static final int BYTES_PER_KILOBYTE = 1024;

	/** 
	 * Overridden to report the average for this statistic in kilobyes.
	 *
	 * @return the average for this statistic in kilobytes per unit of
	 *  measurement (KB/s)
	 */
	public double getAverage() {
		if(_totalStatsRecorded == 0) return 0;
		return (_total/_totalStatsRecorded)/BYTES_PER_KILOBYTE;
	}

	/** 
	 * Overridden to report the maximum for this statistic in kilobyes.
	 *
	 * @return the maximum for a recorded time period for this statistic 
	 *  in kilobytes 
	 */
	public double getMax() {
		return _max/BYTES_PER_KILOBYTE;
	}

	/** 
	 * Overridden to report the total for this statistic in kilobytes.
	 *
	 * @return the total for this statistic in kilobytes 
	 */
	public double getTotal() {
		return _total/BYTES_PER_KILOBYTE;
	}
}
