package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording basic statistics.
 */
abstract class BasicStatistic extends AbstractStatistic {

	protected BasicStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
