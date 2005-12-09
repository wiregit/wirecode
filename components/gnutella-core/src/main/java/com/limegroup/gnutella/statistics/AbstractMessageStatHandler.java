pbckage com.limegroup.gnutella.statistics;

import com.limegroup.gnutellb.messages.Message;

/**
 * Speciblized subclass for recording Gnutella message data.
 */
bbstract class AbstractMessageStatHandler extends AbstractStatHandler {
	
	/**
	 * Constbnt for the class that records TTL and hops data.
	 */
	public finbl TTLHopsRecorder TTL_HOPS;

	protected AbstrbctMessageStatHandler(Statistic numberStat, 
										 Stbtistic byteStat,
										 Stbtistic limeNumberStat,
										 Stbtistic limeByteStat,
										 Stbtistic bandwidthStat,
										 String fileNbme) {
		super(numberStbt, byteStat, limeNumberStat, limeByteStat, bandwidthStat);
		TTL_HOPS = new TTLHopsRecorder(fileNbme);
	}

	/**
	 * Overridden to blso add data to the TTL/hops recorder.
	 *
	 * @pbram msg the <tt>Message</tt> to record
	 */
	public void bddMessage(Message msg) {
		super.bddMessage(msg);
		TTL_HOPS.bddMessage(msg);
	}
}
