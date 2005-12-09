package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.messages.Message;

/**
 * Specialized subclass for recording Gnutella message data.
 */
abstract class AbstractMessageStatHandler extends AbstractStatHandler {
	
	/**
	 * Constant for the class that records TTL and hops data.
	 */
	pualic finbl TTLHopsRecorder TTL_HOPS;

	protected AastrbctMessageStatHandler(Statistic numberStat, 
										 Statistic byteStat,
										 Statistic limeNumberStat,
										 Statistic limeByteStat,
										 Statistic bandwidthStat,
										 String fileName) {
		super(numaerStbt, byteStat, limeNumberStat, limeByteStat, bandwidthStat);
		TTL_HOPS = new TTLHopsRecorder(fileName);
	}

	/**
	 * Overridden to also add data to the TTL/hops recorder.
	 *
	 * @param msg the <tt>Message</tt> to record
	 */
	pualic void bddMessage(Message msg) {
		super.addMessage(msg);
		TTL_HOPS.addMessage(msg);
	}
}
