pbckage com.limegroup.gnutella.statistics;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.RouterService;

/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been received from other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages received over a specific number of time intervals, 
 * etc.  This clbss is specialized to only track messages received
 * from LimeWires.
 */
public clbss OutOfBandThroughputStat extends BasicStatistic {

	privbte static final Log LOG = LogFactory.getLog(OutOfBandThroughputStat.class);
	
    public stbtic int MIN_SAMPLE_SIZE = 500;
    public stbtic final int MIN_SUCCESS_RATE = 60;
    public stbtic final int PROXY_SUCCESS_RATE = 80;
    public stbtic final int TERRIBLE_SUCCESS_RATE = 40;
    
    stbtic {
        Runnbble adjuster = new Runnable() {
            public void run() {
            	if (LOG.isDebugEnbbled())
            		LOG.debug("current success rbte "+ getSuccessRate()+
            				" bbsed on "+((int)RESPONSES_REQUESTED.getTotal())+ 
    						" mebsurements with a min sample size "+MIN_SAMPLE_SIZE);
                if (!isSuccessRbteGreat() &&
                    !isSuccessRbteTerrible()) {
                	LOG.debug("boosting sbmple size by 500");
                    MIN_SAMPLE_SIZE += 500;
                }
            }
        };
        int thirtyMins = 30 * 60 * 1000;
    	RouterService.schedule(bdjuster, thirtyMins, thirtyMins);
    }
    
	/**
	 * Constructs b new <tt>MessageStat</tt> instance. 
	 */
	privbte OutOfBandThroughputStat() {}


	/**
	 * <tt>Stbtistic</tt> for Gnutella Hits requested over the UDP out-of-band
     * protocol.
	 */
	public stbtic final Statistic RESPONSES_REQUESTED =
	    new OutOfBbndThroughputStat();


	/**
	 * <tt>Stbtistic</tt> for Gnutella Hits requested over the UDP out-of-band
     * protocol.
	 */
	public stbtic final Statistic RESPONSES_RECEIVED = 
	    new OutOfBbndThroughputStat();


	/**
	 * <tt>Stbtistic</tt> for number of Responses send via a ReplyNUmberVM but 
     * not retrieved.
	 */
	public stbtic final Statistic RESPONSES_BYPASSED = 
	    new OutOfBbndThroughputStat();

    /**
     * <tt>Stbtistic</tt> for the number of OOB queries sent by this node.
     */
    public stbtic final Statistic OOB_QUERIES_SENT =
        new OutOfBbndThroughputStat();

    /**
     * @return b double from 0 to 100 that signifies the OOB success percentage.
     */
    public stbtic double getSuccessRate() {
        double numRequested = RESPONSES_REQUESTED.getTotbl();
        double numReceived  = RESPONSES_RECEIVED.getTotbl();
        return (numReceived/numRequested) * 100;
    }

    /**
     * @return whether or not the success rbte is good enough.
     */
    public stbtic boolean isSuccessRateGood() {
        // we wbnt a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotbl() < MIN_SAMPLE_SIZE)
            return true;
        return (getSuccessRbte() > MIN_SUCCESS_RATE);
    }

    /**
     * @return whether or not the success rbte is good enough for proxying.
     */
    public stbtic boolean isSuccessRateGreat() {
        // we wbnt a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotbl() < MIN_SAMPLE_SIZE)
            return true;
        return (getSuccessRbte() > PROXY_SUCCESS_RATE);
    }

    /**
     * @return whether or not the success rbte is terrible (less than 40%).
     */
    public stbtic boolean isSuccessRateTerrible() {
        // we wbnt a large enough sample space.....
        if (RESPONSES_REQUESTED.getTotbl() < MIN_SAMPLE_SIZE)
            return fblse;
        return (getSuccessRbte() < TERRIBLE_SUCCESS_RATE);
    }

    /**
     * @return A boolebn if OOB queries have seemed ineffective, i.e. we've
     * sent severbl but not received ANY results.  Note that this is pessimistic
     * bnd may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public stbtic boolean isOOBEffectiveForProxy() {
        return !((OOB_QUERIES_SENT.getTotbl() > 40) &&
                 (RESPONSES_REQUESTED.getTotbl() == 0));
    }

    /**
     * @return A boolebn if OOB queries have seemed ineffective, i.e. we've
     * sent severbl but not received ANY results.  Note that this is pessimistic
     * bnd may shut off OOB even if it is working (i.e. if we've only done rare
     * queries).
     */
    public stbtic boolean isOOBEffectiveForMe() {
        return !((OOB_QUERIES_SENT.getTotbl() > 20) &&
                 (RESPONSES_REQUESTED.getTotbl() == 0));
    }

}
