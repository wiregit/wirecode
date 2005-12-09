pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss handles all statistics for downloads that are
 * not bbsed on time.
 */
public clbss NumericalDownloadStat extends NumericalStatistic {

    /**
     * Ensure thbt no other class can construct on of these.
     */
    privbte NumericalDownloadStat() {}
    
    /**
     * <tt>Stbtistic</tt> for the number of milliseconds it takes
     * to estbblish TCP connections on downloads.
     */
    public stbtic final Statistic TCP_CONNECT_TIME =
        new NumericblDownloadStat();
}
