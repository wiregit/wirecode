package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of all classes that
 * store statistics on routing errors.
 */
public class FlowControlStat extends AdvancedStatistic {
	
	/**
	 * Constructs a new <tt>RouteErrorStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private FlowControlStat() {}

	/**
	 * Private class for keeping track of all dropped sent messages.
	 */
  	private static class GeneralSentMessagesDropped extends FlowControlStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_SENT_MESSAGES_DROPPED.incrementStat();
		}
	}

	/**
	 * <tt>Statistic</tt> for the number of Gnutella messages dropped.
	 */
    public static final Statistic ALL_SENT_MESSAGES_DROPPED =
        new FlowControlStat();


	/**
	 * <tt>Statistic</tt> for the number of query hits dropped.
	 */
    public static final Statistic SENT_QUERY_REPLIES_DROPPED =
        new GeneralSentMessagesDropped();
}
