package com.limegroup.gnutella.statistics;

/**
 * Specialized subclass for recording advanced kilobyte Astatistics.
 */
abstract class BasicKilobytesStatistic extends AbstractKilobytesStatistic {

	protected BasicKilobytesStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
