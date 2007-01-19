package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class handles all statistics for connections.
 */
public class ConnectionStat extends AdvancedStatistic {
	
	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>ConnectionStat</tt> instances.
	 */
	private ConnectionStat() {}

	/**
	 * Specialized class that increments the count of all connection
	 * attempts.  This class should be used by statistics that are
	 * also connection attempts -- that way the total connections
	 * attempt count will be automatically incremented.
	 */
	private static final class AllConnectionAttemptsStat 
		extends ConnectionStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_CONNECTION_ATTEMPTS.incrementStat();
		}
	}

	/**
	 * <tt>Statistic</tt> for all Gnutella connection attempts made.
	 */
	public static final Statistic ALL_CONNECTION_ATTEMPTS =
		new ConnectionStat();

	/**
	 * <tt>Statistic</tt> for all Gnutella incoming connection attempts
	 * made (connection attempts initiated from other hosts).
	 */
	public static final Statistic INCOMING_CONNECTION_ATTEMPTS =
		new AllConnectionAttemptsStat();

	/**
	 * <tt>Statistic</tt> for all Gnutella outgoing connection attempts
	 * made (connection attempts initiated by us)..
	 */
	public static final Statistic OUTGOING_CONNECTION_ATTEMPTS =
		new AllConnectionAttemptsStat();
}
