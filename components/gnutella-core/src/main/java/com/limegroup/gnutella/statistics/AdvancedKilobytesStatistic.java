package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording advanced kilobytes statistics.
 */
abstract class AdvancedKilobytesStatistic extends AbstractKilobytesStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
	protected AdvancedKilobytesStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}
}
