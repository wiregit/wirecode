package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * Specialized subclass for recording Gnutella message data.
 */
abstract class AbstractMessageStatHandler extends AbstractStatHandler {
	
	/**
	 * Constant for the class that records TTL and hops data.
	 */
	public final TTLHopsRecorder TTL_HOPS = new TTLHopsRecorder();

	protected AbstractMessageStatHandler(Statistic numberStat, 
										 Statistic byteStat,
										 Statistic limeNumberStat,
										 Statistic limeByteStat,
										 Statistic bandwidthStat) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat, bandwidthStat);
		
	}

	public void addMessage(Message msg) {
		super.addMessage(msg);
		TTL_HOPS.addMessage(msg);
	}
}
