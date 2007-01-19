package com.limegroup.gnutella.statistics;

import org.limewire.statistic.Statistic;

import com.limegroup.gnutella.messages.Message;

/**
 * Specialized subclass for recording Gnutella message data.
 */
abstract class AbstractMessageStatHandler extends AbstractStatHandler {
	
	/**
	 * Constant for the class that records TTL and hops data.
	 */
	public final TTLHopsRecorder TTL_HOPS;

	protected AbstractMessageStatHandler(Statistic numberStat, 
										 Statistic byteStat,
										 Statistic limeNumberStat,
										 Statistic limeByteStat,
										 Statistic bandwidthStat,
										 String fileName) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat, bandwidthStat);
		TTL_HOPS = new TTLHopsRecorder(fileName);
	}

	/**
	 * Overridden to also add data to the TTL/hops recorder.
	 *
	 * @param msg the <tt>Message</tt> to record
	 */
	public void addMessage(Message msg) {
		super.addMessage(msg);
		TTL_HOPS.addMessage(msg);
	}
}
