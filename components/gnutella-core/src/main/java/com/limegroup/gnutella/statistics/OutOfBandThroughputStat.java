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
public class OutOfBandThroughputStat extends BasicStatistic {

    public static final int MIN_SAMPLE_SIZE = 275;
    public static final int MIN_SUCCESS_RATE = 60;

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


	/**
	 * <tt>Statistic</tt> for number of Responses send via a ReplyNUmberVM but 
     * not retrieved.
	 */
	public static final Statistic RESPONSES_BYPASSED = 
	    new OutOfBandThroughputStat();

    /**
     * @return a double from 0 to 100 that signifies the OOB success percentage.
     */
    public static double getSuccessRate() {
        double numRequested = RESPONSES_REQUESTED.getTotal();
        double numReceived  = RESPONSES_RECEIVED.getTotal();
        return (numReceived/numRequested) * 100;
    }

    /**
     * @return whether or not the success rate is good enough.
     */
    public static boolean isSuccessRateGood() {
        // we want a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return true;
        return (getSuccessRate() > MIN_SUCCESS_RATE);
    }

}
