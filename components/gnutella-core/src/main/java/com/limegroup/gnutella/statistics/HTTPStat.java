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
	 * Specialized class for increment the number of HTTP requests
	 * received.  In addition to increment the actual stat,
	 * this increments the total number of HTTP Requests.
	 */
	private static class HTTPRequestStat extends HTTPStat {
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
		new HTTPRequestStat();

	/**
	 * <tt>Statistic</tt> for all HTTP GET requests that have
	 * been made in this session.
	 */
	public static final Statistic HTTP_GET_REQUESTS =
		new HTTPRequestStat();
		
    /**
     * <tt>Statistic</tt> for all HTTP GIV requests that have been made
     * in this session.
     */
    public static final Statistic HTTP_GIV_REQUESTS =
        new HTTPRequestStat();
}
