package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of all classes for
 * bandwidth data.
 */
public class BandwidthStat extends AbstractStatistic {

	
	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>BandwidthStat</tt> instances.
	 */
	private BandwidthStat() {}


	public long getTotal() {
		return (long)(_total/128);
	}

	public int getMax() {
		return (int)(_max/128);
	}

	public float getAverage() {
		return (float)(getTotal()/_totalStatsRecorded);
	}

	/**
	 * Specialized class for accumulating all upstream bandwidth data.
	 */
	private static class UpstreamBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			UPSTREAM_BANDWIDTH.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all downstream bandwidth data.
	 */
	private static class DownstreamBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			DOWNSTREAM_BANDWIDTH.addData(data);
		}
	}

	/**
	 * Specialized class for accumulating all downstream bandwidth sent
	 * over HTTP.
	 */
	private static class DownstreamHTTPBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			HTTP_DOWNSTREAM_BANDWIDTH.addData(data);
		}
	}


	/**
	 * Specialized class for accumulating all upstream bandwidth sent
	 * over HTTP.
	 */
	private static class UpstreamHTTPBandwidthStat extends BandwidthStat {
		public void addData(int data) {
			super.addData(data);
			HTTP_UPSTREAM_BANDWIDTH.addData(data);
		}
	}
	

	/**
	 * <tt>Statistic</tt> for all upstream bandwidth.
	 */
	public static final Statistic UPSTREAM_BANDWIDTH =
		new BandwidthStat();

	/**
	 * <tt>Statistic</tt> for all downstream bandwidth.
	 */
	public static final Statistic DOWNSTREAM_BANDWIDTH =
		new BandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP downstream bandwidth.
	 */
	public static final Statistic HTTP_DOWNSTREAM_BANDWIDTH =
		new DownstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP upstream bandwidth.
	 */
	public static final Statistic HTTP_UPSTREAM_BANDWIDTH =
		new UpstreamBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP downstream header bandwidth -- http
	 * headers read from the network.
	 */
	public static final Statistic HTTP_HEADER_DOWNSTREAM_BANDWIDTH =
		new DownstreamHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP message body bytes read from the
	 * network.
	 */
	public static final Statistic HTTP_BODY_DOWNSTREAM_BANDWIDTH =
		new DownstreamHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP upstream header bandwidth -- http
	 * headers read from the network.
	 */
	public static final Statistic HTTP_HEADER_UPSTREAM_BANDWIDTH =
		new UpstreamHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all HTTP message body bytes read from the
	 * network.
	 */
	public static final Statistic HTTP_BODY_UPSTREAM_BANDWIDTH =
		new UpstreamHTTPBandwidthStat();

	/**
	 * <tt>Statistic</tt> for all downstream bandwidth used by Gnutella
	 * message traffic.
	 */
	public static final Statistic GNUTELLA_DOWNSTREAM_BANDWIDTH =
		new DownstreamBandwidthStat();


	/**
	 * <tt>Statistic</tt> for all upstream bandwidth used by Gnutella
	 * message traffic.
	 */
	public static final Statistic GNUTELLA_UPSTREAM_BANDWIDTH =
		new UpstreamBandwidthStat();

}
