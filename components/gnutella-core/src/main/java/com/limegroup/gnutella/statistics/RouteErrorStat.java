package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of all classes that
 * store statistics on routing errors.
 */
public class RouteErrorStat extends AdvancedStatistic {
	
	/**
	 * Constructs a new <tt>RouteErrorStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private RouteErrorStat() {}

	/**
	 * Private class for keeping track of routing error statistics.
	 */
  	private static class GeneralRouteErrorStat extends RouteErrorStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_ROUTE_ERRORS.incrementStat();
		}
	}
	
	/**
	 * <tt>Statistic</tt> for all route errors.
	 */
	public static final Statistic ALL_ROUTE_ERRORS =
		new RouteErrorStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pong routing errors.
	 */
	public static final Statistic PING_REPLY_ROUTE_ERRORS = 
	    new GeneralRouteErrorStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query reply routing errors.
	 */
	public static final Statistic QUERY_REPLY_ROUTE_ERRORS = 
	    new GeneralRouteErrorStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push routing errors.
	 */
	public static final Statistic PUSH_REQUEST_ROUTE_ERRORS = 
	    new GeneralRouteErrorStat();
}
