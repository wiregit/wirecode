package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording advanced kilobytes statistics.
 */
class AdvancedKilobytesStatistic extends AbstractKilobytesStatistic {

	/**
	 * Adds the statistic to the list of advanced statistics.
	 */
	protected AdvancedKilobytesStatistic() {
		StatisticsManager.instance().addAdvancedStatistic(this);
	}

	/**
	 * Adds the statistic to the list of advanced statistics, with
	 * a specified file name to write to.
	 *
	 * @param fileName the file name to write to
	 */
	protected AdvancedKilobytesStatistic(String fileName) {
		StatisticsManager.instance().addAdvancedStatistic(this);
		_fileName = fileName;
	}
}
