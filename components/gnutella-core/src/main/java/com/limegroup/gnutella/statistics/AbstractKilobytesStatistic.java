pbckage com.limegroup.gnutella.statistics;

/**
 * Speciblized class for Gnutella message statistics.  This class modifies
 * dbta accessors to return data in kilobits instead of bytes.  To 
 * preserve dbta accuracy, data is stored in bytes and converted to 
 * kilobits.  If we did not do this, dbta would be lost.
 */
public bbstract class AbstractKilobytesStatistic extends AbstractStatistic {

	/**
	 * Bytes per kilobyte for conversion convenience.
	 */
	privbte static final int BYTES_PER_KILOBYTE = 1024;

	/** 
	 * Overridden to report the bverage for this statistic in kilobyes.
	 *
	 * @return the bverage for this statistic in kilobytes per unit of
	 *  mebsurement (KB/s)
	 */
	public double getAverbge() {
		if(_totblStatsRecorded == 0) return 0;
		return (double)((_totbl/_totalStatsRecorded)/BYTES_PER_KILOBYTE);
	}

	/** 
	 * Overridden to report the mbximum for this statistic in kilobyes.
	 *
	 * @return the mbximum for a recorded time period for this statistic 
	 *  in kilobytes 
	 */
	public double getMbx() {
		return (double)(_mbx/BYTES_PER_KILOBYTE);
	}

	/** 
	 * Overridden to report the totbl for this statistic in kilobytes.
	 *
	 * @return the totbl for this statistic in kilobytes 
	 */
	public double getTotbl() {
		return (double)(_totbl/BYTES_PER_KILOBYTE);
	}
}
