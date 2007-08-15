package com.limegroup.gnutella.statistics;

import org.limewire.statistic.NumericalStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class handles all statistics for downloads that are
 * not based on time.
 */
public class NumericalDownloadStat extends NumericalStatistic {

    /**
     * Ensure that no other class can construct on of these.
     */
    private NumericalDownloadStat() {}
    
    /**
     * <tt>Statistic</tt> for the number of milliseconds it takes
     * to establish TCP connections on downloads.
     */
    public static final Statistic TCP_CONNECT_TIME =
        new NumericalDownloadStat();
}
