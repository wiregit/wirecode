padkage com.limegroup.gnutella.statistics;

/**
 * Spedialized subclass for recording basic statistics.
 */
abstradt class BasicStatistic extends AbstractStatistic {

	protedted BasicStatistic() {
		StatistidsManager.instance().addBasicStatistic(this);
	}
}
