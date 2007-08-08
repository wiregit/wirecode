package com.limegroup.gnutella.statistics;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.statistic.BasicStatistic;
import org.limewire.statistic.Statistic;

import com.limegroup.gnutella.ProviderHacks;

/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been received from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages received over a specific number of time intervals, 
 * etc.  This class is specialized to only track messages received
 * from LimeWires.
 */
public class OutOfBandThroughputStat extends BasicStatistic {

	private static final Log LOG = LogFactory.getLog(OutOfBandThroughputStat.class);
	
    public static int MIN_SAMPLE_SIZE = 500;
    public static final int MIN_SUCCESS_RATE = 60;
    public static final int PROXY_SUCCESS_RATE = 80;
    public static final int TERRIBLE_SUCCESS_RATE = 40;
    
    static {
        Runnable adjuster = new Runnable() {
            public void run() {
            	if (LOG.isDebugEnabled())
            		LOG.debug("current success rate "+ getSuccessRate()+
            				" based on "+((int)RESPONSES_REQUESTED.getTotal())+ 
    						" measurements with a min sample size "+MIN_SAMPLE_SIZE);
                if (!isSuccessRateGreat() &&
                    !isSuccessRateTerrible()) {
                	LOG.debug("boosting sample size by 500");
                    MIN_SAMPLE_SIZE += 500;
                }
            }
        };
        int thirtyMins = 30 * 60 * 1000;
        
        //DPINJ: Move this somewhere else!!
    	ProviderHacks.getBackgroundExecutor().scheduleWithFixedDelay(adjuster, thirtyMins, thirtyMins, TimeUnit.MILLISECONDS);
    }
    
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
     * <tt>Statistic</tt> for the number of OOB queries sent by this node.
     */
    public static final Statistic OOB_QUERIES_SENT =
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

    /**
     * @return whether or not the success rate is good enough for proxying.
     */
    public static boolean isSuccessRateGreat() {
        // we want a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return true;
        return (getSuccessRate() > PROXY_SUCCESS_RATE);
    }

    /**
     * @return whether or not the success rate is terrible (less than 40%).
     */
    public static boolean isSuccessRateTerrible() {
        // we want a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return false;
        return (getSuccessRate() < TERRIBLE_SUCCESS_RATE);
    }

    /**
     * @return A boolean if OOB queries have seemed ineffective, i.e. we've
     * sent several but not received ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public static boolean isOOBEffectiveForProxy() {
        return !((OOB_QUERIES_SENT.getTotal() > 40) &&
                 (RESPONSES_REQUESTED.getTotal() == 0));
    }

    /**
     * @return A boolean if OOB queries have seemed ineffective, i.e. we've
     * sent several but not received ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public static boolean isOOBEffectiveForMe() {
        return !((OOB_QUERIES_SENT.getTotal() > 20) &&
                 (RESPONSES_REQUESTED.getTotal() == 0));
    }

}
