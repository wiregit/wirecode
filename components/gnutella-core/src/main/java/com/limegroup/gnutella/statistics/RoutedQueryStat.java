package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of all classes that
 * store statistics on routed queries.
 */
public class RoutedQueryStat extends AdvancedStatistic {
	
	/**
	 * Constructs a new <tt>RoutedQueryStat</tt> instance, ensuring
	 * that no other class can construct one.
	 */
	private RoutedQueryStat() {}

	/**
	 * Private class for recording statistics for routed queries
	 * that should also be counted in all Ultrapeer queries.
	 */
	private static class UltrapeerQueryStat extends RoutedQueryStat {
		
		/**
		 * Overridden to also increment to statistic for all routed
		 * Ultrapeer queries potentially sent (maybe sent or maybe
		 * not, depending on matches in the QRP tables).
		 */
		public void incrementStat() {
			super.incrementStat();
			ALL_ULTRAPEER_QUERIES.incrementStat();
		}
	}

	/**
	 * Private class for recording statistics for routed queries
	 * that should also be counted in all leaf queries.
	 */
	private static class LeafQueryStat extends RoutedQueryStat {
		
		/**
		 * Overridden to also increment to statistic for all routed
		 * Ultrapeer queries potentially sent (maybe sent or maybe
		 * not, depending on matches in the QRP tables).
		 */
		public void incrementStat() {
			super.incrementStat();
			ALL_LEAF_QUERIES.incrementStat();
		}
	}

	/**
	 * <tt>RoutedQueryStat</tt> for all queries potentially routed to
	 * Ultrapeers.
	 */
	public static final RoutedQueryStat ALL_ULTRAPEER_QUERIES =
		new RoutedQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for all queries potentially routed to
	 * leaves.
	 */
	public static final RoutedQueryStat ALL_LEAF_QUERIES =
		new RoutedQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are forwarded to other
	 * QRP Ultrapeers (match in the QRP tables).
	 */
	public static final RoutedQueryStat ULTRAPEER_SEND =
		new UltrapeerQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are dropped before being
	 * sent to other Ultrapeers (no match in the QRP tables).
	 */
	public static final RoutedQueryStat ULTRAPEER_DROP =
		new UltrapeerQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are forwarded to other
	 * QRP leaves (match in the QRP tables).
	 */
	public static final RoutedQueryStat LEAF_SEND =
		new LeafQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are dropped before being
	 * sent to other leaves (no match in the QRP tables).
	 */
	public static final RoutedQueryStat LEAF_DROP =
		new LeafQueryStat();


}
