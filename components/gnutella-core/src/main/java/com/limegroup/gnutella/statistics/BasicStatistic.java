pbckage com.limegroup.gnutella.statistics;

/**
 * Speciblized subclass for recording basic statistics.
 */
bbstract class BasicStatistic extends AbstractStatistic {

	protected BbsicStatistic() {
		StbtisticsManager.instance().addBasicStatistic(this);
	}
}
