package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of statistics for HTTP
 * requests.  Each statistic maintains its own history, all messages 
 * received over a specific number of time intervals.
 */
public class HTTPStat extends AdvancedStatistic {

	/**
	 * Make the constructor private so that only this class can construct
	 * an <tt>HTTPStat</tt> instances.
	 */
	private HTTPStat() {}

	/**
	 * Specialized class for incrementing the number of HEAD requests
	 * received.  In addition to incrementing the number of HEAD requests,
	 * this also increments the total number of HTTP requests stored.
	 */
	private static class HeadRequestStat extends HTTPStat {
		public void incrementStat() {
			super.incrementStat();
			HTTP_REQUESTS.incrementStat();
		}
	}

	/**
	 * Specialized class for incrementing the number of GET requests
	 * received.  In addition to incrementing the number of GET requests,
	 * this also increments the total number of HTTP requests stored.
	 */
	private static class GetRequestStat extends HTTPStat {
		public void incrementStat() {
			super.incrementStat();
			HTTP_REQUESTS.incrementStat();
		}
	}

	/**
	 * <tt>Statistic</tt> for all HTTP requests of any type that have
	 * been made in this session.
	 */
	public static final Statistic HTTP_REQUESTS =
		new HTTPStat();

	/**
	 * <tt>Statistic</tt> for all HTTP HEAD requests that have
	 * been made in this session.
	 */
	public static final Statistic HTTP_HEAD_REQUESTS =
		new HeadRequestStat();

	/**
	 * <tt>Statistic</tt> for all HTTP GET requests that have
	 * been made in this session.
	 */
	public static final Statistic HTTP_GET_REQUESTS =
		new GetRequestStat();
}
