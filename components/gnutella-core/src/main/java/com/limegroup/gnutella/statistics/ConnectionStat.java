package com.limegroup.gnutella.statistics;

/**
 * This class handles all statistics for connections.
 */
public class ConnectionStat extends AbstractStatistic {
	
	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>ConnectionStat</tt> instances.
	 */
	private ConnectionStat() {}

	/**
	 * <tt>Statistic</tt> for all Gnutella connection attempts made.
	 */
	public static final Statistic ALL_CONNECTION_ATTEMPTS =
		new ConnectionStat();
}
