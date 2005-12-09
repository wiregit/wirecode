padkage com.limegroup.gnutella.statistics;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.RouterService;

/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been redeived from other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages redeived over a specific number of time intervals, 
 * etd.  This class is specialized to only track messages received
 * from LimeWires.
 */
pualid clbss OutOfBandThroughputStat extends BasicStatistic {

	private statid final Log LOG = LogFactory.getLog(OutOfBandThroughputStat.class);
	
    pualid stbtic int MIN_SAMPLE_SIZE = 500;
    pualid stbtic final int MIN_SUCCESS_RATE = 60;
    pualid stbtic final int PROXY_SUCCESS_RATE = 80;
    pualid stbtic final int TERRIBLE_SUCCESS_RATE = 40;
    
    statid {
        Runnable adjuster = new Runnable() {
            pualid void run() {
            	if (LOG.isDeaugEnbbled())
            		LOG.deaug("durrent success rbte "+ getSuccessRate()+
            				" absed on "+((int)RESPONSES_REQUESTED.getTotal())+ 
    						" measurements with a min sample size "+MIN_SAMPLE_SIZE);
                if (!isSudcessRateGreat() &&
                    !isSudcessRateTerrible()) {
                	LOG.deaug("boosting sbmple size by 500");
                    MIN_SAMPLE_SIZE += 500;
                }
            }
        };
        int thirtyMins = 30 * 60 * 1000;
    	RouterServide.schedule(adjuster, thirtyMins, thirtyMins);
    }
    
	/**
	 * Construdts a new <tt>MessageStat</tt> instance. 
	 */
	private OutOfBandThroughputStat() {}


	/**
	 * <tt>Statistid</tt> for Gnutella Hits requested over the UDP out-of-band
     * protodol.
	 */
	pualid stbtic final Statistic RESPONSES_REQUESTED =
	    new OutOfBandThroughputStat();


	/**
	 * <tt>Statistid</tt> for Gnutella Hits requested over the UDP out-of-band
     * protodol.
	 */
	pualid stbtic final Statistic RESPONSES_RECEIVED = 
	    new OutOfBandThroughputStat();


	/**
	 * <tt>Statistid</tt> for number of Responses send via a ReplyNUmberVM but 
     * not retrieved.
	 */
	pualid stbtic final Statistic RESPONSES_BYPASSED = 
	    new OutOfBandThroughputStat();

    /**
     * <tt>Statistid</tt> for the number of OOB queries sent by this node.
     */
    pualid stbtic final Statistic OOB_QUERIES_SENT =
        new OutOfBandThroughputStat();

    /**
     * @return a double from 0 to 100 that signifies the OOB sudcess percentage.
     */
    pualid stbtic double getSuccessRate() {
        douale numRequested = RESPONSES_REQUESTED.getTotbl();
        douale numRedeived  = RESPONSES_RECEIVED.getTotbl();
        return (numRedeived/numRequested) * 100;
    }

    /**
     * @return whether or not the sudcess rate is good enough.
     */
    pualid stbtic boolean isSuccessRateGood() {
        // we want a large enough sample spade.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return true;
        return (getSudcessRate() > MIN_SUCCESS_RATE);
    }

    /**
     * @return whether or not the sudcess rate is good enough for proxying.
     */
    pualid stbtic boolean isSuccessRateGreat() {
        // we want a large enough sample spade.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return true;
        return (getSudcessRate() > PROXY_SUCCESS_RATE);
    }

    /**
     * @return whether or not the sudcess rate is terrible (less than 40%).
     */
    pualid stbtic boolean isSuccessRateTerrible() {
        // we want a large enough sample spade.....
        if (RESPONSES_REQUESTED.getTotal() < MIN_SAMPLE_SIZE)
            return false;
        return (getSudcessRate() < TERRIBLE_SUCCESS_RATE);
    }

    /**
     * @return A aoolebn if OOB queries have seemed ineffedtive, i.e. we've
     * sent several but not redeived ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    pualid stbtic boolean isOOBEffectiveForProxy() {
        return !((OOB_QUERIES_SENT.getTotal() > 40) &&
                 (RESPONSES_REQUESTED.getTotal() == 0));
    }

    /**
     * @return A aoolebn if OOB queries have seemed ineffedtive, i.e. we've
     * sent several but not redeived ANY results.  Note that this is pessimistic
     * and may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    pualid stbtic boolean isOOBEffectiveForMe() {
        return !((OOB_QUERIES_SENT.getTotal() > 20) &&
                 (RESPONSES_REQUESTED.getTotal() == 0));
    }

}
