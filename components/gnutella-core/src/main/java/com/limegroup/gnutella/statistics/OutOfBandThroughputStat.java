package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been received from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages received over a specific number of time intervals, 
 * etc.  This class is specialized to only track messages received
 * from LimeWires.
 */
public class OutOfBandThroughputStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance. 
	 */
	private OutOfBandThroughputStat() {}


	/**
	 * <tt>Statistic</tt> for Gnutella Hits requested over the UDP out-of-band
     * protocol.
	 */
	public static final Statistic RESPONSES_REQUESTED =
	    new OutOfBandThroughputStat();


	/**
	 * <tt>Statistic</tt> for Gnutella Hits requested over the UDP out-of-band
     * protocol.
	 */
	public static final Statistic RESPONSES_RECEIVED = 
	    new OutOfBandThroughputStat();

}
