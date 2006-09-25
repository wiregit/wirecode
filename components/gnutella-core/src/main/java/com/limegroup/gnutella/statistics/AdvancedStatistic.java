package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording advanced statistics.
 */
public class AdvancedStatistic extends AbstractStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
	protected AdvancedStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the statistic to the list of advanced statistics, with
	 * a specified file name to write to.
	 *
	 * @param fileName the file name to write to
	 */
	protected AdvancedStatistic(String fileName) {
		StatisticsManager.instance().addAdvancedStatistic(this);
		_fileName = fileName;
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
