package com.limegroup.bittorrent.statistics;

import com.limegroup.gnutella.statistics.BasicKilobytesStatistic;
import com.limegroup.gnutella.statistics.Statistic;
import com.limegroup.gnutella.statistics.BandwidthStat.DownstreamBandwidthStat;
import com.limegroup.gnutella.statistics.BandwidthStat.UpstreamBandwidthStat;

public class BandwidthStat extends BasicKilobytesStatistic {

    /**
     * <tt>Statistic</tt> for all downstream bandwidth used by Bittorrent.
     */
    public static final Statistic BITTORRENT_MESSAGE_DOWNSTREAM_BANDWIDTH =
            new DownstreamBandwidthStat();


    /**
     * <tt>Statistic</tt> for all upstream bandwidth used by Bittorrent.
     */
    public static final Statistic BITTORRENT_MESSAGE_UPSTREAM_BANDWIDTH =
            new UpstreamBandwidthStat();
}
