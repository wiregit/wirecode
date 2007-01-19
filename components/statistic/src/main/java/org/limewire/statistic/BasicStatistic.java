package org.limewire.statistic;


/**
 * Specialized subclass for recording basic statistics.
 */
public abstract class BasicStatistic extends AbstractStatistic {

	protected BasicStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
