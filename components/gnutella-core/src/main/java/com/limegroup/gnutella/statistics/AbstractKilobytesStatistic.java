package com.limegroup.gnutella.statistics;

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

	// inherit doc comment
	public double getAverage() {
		return (double)((_total/_totalStatsRecorded)/BYTES_PER_KILOBYTE);
	}

	// inherit doc comment
	public double getMax() {
		return (double)(_max/BYTES_PER_KILOBYTE);
	}

	// inherit doc comment
	public double getTotal() {
		return (double)(_total/BYTES_PER_KILOBYTE);
	}
}
