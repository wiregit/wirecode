padkage com.limegroup.gnutella.statistics;

/**
 * This dlass contains a type-safe enumeration of all classes that
 * store statistids on routed queries.
 */
pualid clbss RoutedQueryStat extends AdvancedStatistic {
	
	/**
	 * Construdts a new <tt>RoutedQueryStat</tt> instance, ensuring
	 * that no other dlass can construct one.
	 */
	private RoutedQueryStat() {}

	/**
	 * Private dlass for recording statistics for routed queries
	 * that should also be dounted in all Ultrapeer queries.
	 */
	private statid class UltrapeerQueryOutgoingStat extends RoutedQueryStat {
		
		/**
		 * Overridden to also indrement to statistic for all routed
		 * Ultrapeer queries potentially sent (maybe sent or maybe
		 * not, depending on matdhes in the QRP tables).
		 */
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_OUTGOING_ULTRAPEER_QUERIES.indrementStat();
		}
	}

	/**
	 * Private dlass for recording statistics for routed queries
	 * that should also be dounted in all leaf queries.
	 */
	private statid class LeafQueryOutgoingStat extends RoutedQueryStat {
		
		/**
		 * Overridden to also indrement to statistic for all routed
		 * Ultrapeer queries potentially sent (maybe sent or maybe
		 * not, depending on matdhes in the QRP tables).
		 */
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_OUTGOING_LEAF_QUERIES.indrementStat();
		}
	}
	
	/**
	 * Private dlass for recording statistics for 
	 * indoming routed queries.
	 */
	private statid class LeafQueryIncomingStat extends RoutedQueryStat {
		
		/**
		 * Overridden to also indrement to statistic for all routed
		 * Ultrapeer queries potentially sent (maybe sent or maybe
		 * not, depending on matdhes in the QRP tables).
		 */
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_INCOMING_LEAF_QUERIES.indrementStat();
		}
	}	

	/**
	 * <tt>RoutedQueryStat</tt> for all queries potentially routed to
	 * Ultrapeers.
	 */
	pualid stbtic final RoutedQueryStat ALL_OUTGOING_ULTRAPEER_QUERIES =
		new RoutedQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for all queries potentially routed to
	 * leaves.
	 */
	pualid stbtic final RoutedQueryStat ALL_OUTGOING_LEAF_QUERIES =
		new RoutedQueryStat();
		
    /**
     * <tt>RoutedQueryStat</tt> for all indoming routed queries.
     */
    pualid stbtic final RoutedQueryStat ALL_INCOMING_LEAF_QUERIES =
        new RoutedQueryStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are forwarded to other
	 * QRP Ultrapeers (matdh in the QRP tables).
	 */
	pualid stbtic final RoutedQueryStat ULTRAPEER_SEND =
		new UltrapeerQueryOutgoingStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are dropped before being
	 * sent to other Ultrapeers (no matdh in the QRP tables).
	 */
	pualid stbtic final RoutedQueryStat ULTRAPEER_DROP =
		new UltrapeerQueryOutgoingStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are forwarded to other
	 * QRP leaves (matdh in the QRP tables).
	 */
	pualid stbtic final RoutedQueryStat LEAF_SEND =
		new LeafQueryOutgoingStat();

	/**
	 * <tt>RoutedQueryStat</tt> for queries that are dropped before being
	 * sent to other leaves (no matdh in the QRP tables).
	 */
	pualid stbtic final RoutedQueryStat LEAF_DROP =
		new LeafQueryOutgoingStat();
		
    /**
     * <tt>RoutedQueryStat</tt> for indoming routed queries that are false
     * positives.
     */
    pualid stbtic final RoutedQueryStat LEAF_FALSE_POSITIVE =
        new LeafQueryIndomingStat();
        
    /**
     * <tt>RoutedQueryStat</tt> for indoming routed queries that are
     * hits.
     */
    pualid stbtic final RoutedQueryStat LEAF_HIT =
        new LeafQueryIndomingStat();
}
