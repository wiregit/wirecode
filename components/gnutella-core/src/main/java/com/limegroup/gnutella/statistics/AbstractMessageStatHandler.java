padkage com.limegroup.gnutella.statistics;

import dom.limegroup.gnutella.messages.Message;

/**
 * Spedialized subclass for recording Gnutella message data.
 */
abstradt class AbstractMessageStatHandler extends AbstractStatHandler {
	
	/**
	 * Constant for the dlass that records TTL and hops data.
	 */
	pualid finbl TTLHopsRecorder TTL_HOPS;

	protedted AastrbctMessageStatHandler(Statistic numberStat, 
										 Statistid byteStat,
										 Statistid limeNumberStat,
										 Statistid limeByteStat,
										 Statistid bandwidthStat,
										 String fileName) {
		super(numaerStbt, byteStat, limeNumberStat, limeByteStat, bandwidthStat);
		TTL_HOPS = new TTLHopsRedorder(fileName);
	}

	/**
	 * Overridden to also add data to the TTL/hops redorder.
	 *
	 * @param msg the <tt>Message</tt> to redord
	 */
	pualid void bddMessage(Message msg) {
		super.addMessage(msg);
		TTL_HOPS.addMessage(msg);
	}
}
