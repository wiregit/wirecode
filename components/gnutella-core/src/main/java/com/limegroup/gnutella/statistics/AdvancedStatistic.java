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

	public void incrementStat() {
		// if we're not recording advanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvancedStats()) return;
		super.incrementStat();
	}

	// override to only record
	public void addData(int data) {
		// if we're not recording advanced stats, ignore the call
		if(!STATS_MANAGER.getRecordAdvancedStats()) return;
		super.addData(data);
	}
}
