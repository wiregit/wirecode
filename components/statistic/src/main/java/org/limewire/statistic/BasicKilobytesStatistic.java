package org.limewire.statistic;

/**
 * Specialized subclass for recording advanced kilobyte Astatistics.
 */
public abstract class BasicKilobytesStatistic extends AbstractKilobytesStatistic {

    public BasicKilobytesStatistic() {
		StatisticsManager.instance().addBasicStatistic(this);
	}
}
