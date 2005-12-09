pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss handles all statistics for connections.
 */
public clbss ConnectionStat extends AdvancedStatistic {
	
	/**
	 * Mbke the constructor private so that only this class can construct
	 * b <tt>ConnectionStat</tt> instances.
	 */
	privbte ConnectionStat() {}

	/**
	 * Speciblized class that increments the count of all connection
	 * bttempts.  This class should be used by statistics that are
	 * blso connection attempts -- that way the total connections
	 * bttempt count will be automatically incremented.
	 */
	privbte static final class AllConnectionAttemptsStat 
		extends ConnectionStbt {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_CONNECTION_ATTEMPTS.incrementStbt();
		}
	}

	/**
	 * <tt>Stbtistic</tt> for all Gnutella connection attempts made.
	 */
	public stbtic final Statistic ALL_CONNECTION_ATTEMPTS =
		new ConnectionStbt();

	/**
	 * <tt>Stbtistic</tt> for all Gnutella incoming connection attempts
	 * mbde (connection attempts initiated from other hosts).
	 */
	public stbtic final Statistic INCOMING_CONNECTION_ATTEMPTS =
		new AllConnectionAttemptsStbt();

	/**
	 * <tt>Stbtistic</tt> for all Gnutella outgoing connection attempts
	 * mbde (connection attempts initiated by us)..
	 */
	public stbtic final Statistic OUTGOING_CONNECTION_ATTEMPTS =
		new AllConnectionAttemptsStbt();
}
