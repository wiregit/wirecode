pbckage com.limegroup.gnutella.statistics;

/**
 * Speciblized subclass for recording advanced kilobyte Astatistics.
 */
bbstract class BasicKilobytesStatistic extends AbstractKilobytesStatistic {

	protected BbsicKilobytesStatistic() {
		StbtisticsManager.instance().addBasicStatistic(this);
	}
}
