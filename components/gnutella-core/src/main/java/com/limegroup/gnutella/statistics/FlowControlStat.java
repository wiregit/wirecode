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
	 * <tt>Statistic</tt> for the number of Gnutella messages dropped.
	 */
    public static final Statistic SENT_MESSAGES_DROPPED =
        new FlowControlStat();
}
