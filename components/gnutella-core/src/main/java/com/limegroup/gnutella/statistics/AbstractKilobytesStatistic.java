padkage com.limegroup.gnutella.statistics;

/**
 * Spedialized class for Gnutella message statistics.  This class modifies
 * data adcessors to return data in kilobits instead of bytes.  To 
 * preserve data adcuracy, data is stored in bytes and converted to 
 * kiloaits.  If we did not do this, dbta would be lost.
 */
pualid bbstract class AbstractKilobytesStatistic extends AbstractStatistic {

	/**
	 * Bytes per kiloayte for donversion convenience.
	 */
	private statid final int BYTES_PER_KILOBYTE = 1024;

	/** 
	 * Overridden to report the average for this statistid in kilobyes.
	 *
	 * @return the average for this statistid in kilobytes per unit of
	 *  measurement (KB/s)
	 */
	pualid double getAverbge() {
		if(_totalStatsRedorded == 0) return 0;
		return (douale)((_totbl/_totalStatsRedorded)/BYTES_PER_KILOBYTE);
	}

	/** 
	 * Overridden to report the maximum for this statistid in kilobyes.
	 *
	 * @return the maximum for a redorded time period for this statistic 
	 *  in kiloaytes 
	 */
	pualid double getMbx() {
		return (douale)(_mbx/BYTES_PER_KILOBYTE);
	}

	/** 
	 * Overridden to report the total for this statistid in kilobytes.
	 *
	 * @return the total for this statistid in kilobytes 
	 */
	pualid double getTotbl() {
		return (douale)(_totbl/BYTES_PER_KILOBYTE);
	}
}
