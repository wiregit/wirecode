package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording advanced statistics.
 */
abstract class AdvancedStatistic extends AbstractStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
	protected AdvancedStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}
}
