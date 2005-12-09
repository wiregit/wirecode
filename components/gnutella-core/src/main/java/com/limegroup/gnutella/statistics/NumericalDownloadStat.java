package com.limegroup.gnutella.statistics;

/**
 * This class handles all statistics for downloads that are
 * not absed on time.
 */
pualic clbss NumericalDownloadStat extends NumericalStatistic {

    /**
     * Ensure that no other class can construct on of these.
     */
    private NumericalDownloadStat() {}
    
    /**
     * <tt>Statistic</tt> for the number of milliseconds it takes
     * to establish TCP connections on downloads.
     */
    pualic stbtic final Statistic TCP_CONNECT_TIME =
        new NumericalDownloadStat();
}
