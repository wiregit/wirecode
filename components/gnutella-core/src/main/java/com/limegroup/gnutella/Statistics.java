package com.limegroup.gnutella;

/**
 * Maintains various session statistics, like uptime.  Implements the Singleton
 * pattern.  Statistics are initialized the when the class is loaded; call
 * Statistics.instance() to guarantee initialization.  
 */
public class Statistics {
    private static Statistics _instance=new Statistics();

	/**
	 * Variable for the total number of pings received over TCP.
	 */
	private static int _totalTCPPingRequests = 0;

	/**
	 * Variable for the total number of pongs received over TCP.
	 */
	private static int _totalTCPPingReplies = 0;

	/**
	 * Variable for the total number of queries received over TCP.
	 */
	private static int _totalTCPQueryRequests = 0;

	/**
	 * Variable for the total number of replies received over TCP.
	 */
	private static int _totalTCPQueryReplies = 0;

	/**
	 * Variable for the total number of push requests received over TCP.
	 */
	private static int _totalTCPPushRequests = 0;

	/**
	 * Variable for the total number of route table messages received 
	 * over TCP.
	 */
	private static int _totalTCPRouteTableMessages = 0;

	/**
	 * Variable for the current number of pings received over TCP.
	 */
	private static int _currentTCPPingRequests = 0;

	/**
	 * Variable for the current number of pongs received over TCP.
	 */
	private static int _currentTCPPingReplies = 0;

	/**
	 * Variable for the current number of queries received over TCP.
	 */
	private static int _currentTCPQueryRequests = 0;

	/**
	 * Variable for the current number of replies received over TCP.
	 */
	private static int _currentTCPQueryReplies = 0;

	/**
	 * Variable for the current number of push requests received over TCP.
	 */
	private static int _currentTCPPushRequests = 0;

	/**
	 * Variable for the current number of route table messages received 
	 * over TCP.
	 */
	private static int _currentTCPRouteTableMessages = 0;

	/**
	 * Variable for the current number of messages filtered over TCP.
	 */
	private static int _currentFilteredTCPMessages = 0;

	/**
	 * Variable for the total number of messages received over TCP
	 * that have been filtered.
	 */
	private static int _totalFilteredTCPMessages = 0;

	/**
	 * Variable for the total number of TCP messages received.
	 */
	private static long _totalTCPMessages = 0;

	/**
	 * Variable for the total number of pings received over UDP.
	 */
	private static int _totalUDPPingRequests = 0;

	/**
	 * Variable for the total number of pongs received over UDP.
	 */
	private static int _totalUDPPingReplies = 0;

	/**
	 * Variable for the total number of queries received over UDP.
	 */
	private static int _totalUDPQueryRequests = 0;

	/**
	 * Variable for the total number of replies received over UDP.
	 */
	private static int _totalUDPQueryReplies = 0;

	/**
	 * Variable for the total number of push requests received over UDP.
	 */
	private static int _totalUDPPushRequests = 0;

	/**
	 * Variable for the total number of route table messages received 
	 * over UDP.
	 */
	private static int _totalUDPRouteTableMessages = 0;

	/**
	 * Variable for the total number of messages received over UDP
	 * that have been filtered.
	 */
	private static int _totalFilteredUDPMessages = 0;

	/**
	 * Variable for the current number of pings received over UDP.
	 */
	private static int _currentUDPPingRequests = 0;

	/**
	 * Variable for the current number of pongs received over UDP.
	 */
	private static int _currentUDPPingReplies = 0;

	/**
	 * Variable for the current number of queries received over UDP.
	 */
	private static int _currentUDPQueryRequests = 0;

	/**
	 * Variable for the current number of replies received over UDP.
	 */
	private static int _currentUDPQueryReplies = 0;

	/**
	 * Variable for the current number of push requests received over UDP.
	 */
	private static int _currentUDPPushRequests = 0;

	/**
	 * Variable for the current number of route table messages received 
	 * over UDP.
	 */
	private static int _currentUDPRouteTableMessages = 0;

	/**
	 * Variable for the current number of messages filtered over UDP.
	 */
	private static int _currentFilteredUDPMessages = 0;

	/**
	 * Variable for the total number of UDP messages received.
	 */
	private static long _totalUDPMessages = 0;
    
	/**
	 * Variable for the total number of messages received.
	 */
	private static long _totalMessages = 0;

	/**
	 * Variable for the total number of routing errors.
	 */
	private static int _totalRouteErrors = 0;

    /** "PROTECTED" FOR TESTING PURPOSES ONLY! */
    protected Statistics() {}
    
    /** Returns the single Statistics instance. */
    public static Statistics instance() {
        return _instance;
    }

    
    /////////////////////////////////////////////////////////////////////

    /** The number of seconds in a day. */
    private static final int SECONDS_PER_DAY=24*60*60;
    /** Controls how much past is remembered in calculateFractionalUptime.
     *  Default: 7 days, which doesn't quite mean what you might think
     *  see calculateFractionalUptime. */
    private static final int WINDOW_MILLISECONDS=7*SECONDS_PER_DAY*1000;
    
    /** The time this was initialized. */
    private long startTime=now();
    

    /** 
     * Returns the amount of time this has been running.
     * @return the session uptime in milliseconds
     */        
    public long getUptime() {
        //TODO: clocks can go backwards from daylight savings, resulting in
        //negative values.
        return now() - startTime;
    }

    /**
     * Calculates the average number of seconds this host runs per day, i.e.,
     * calculateFractionRunning*24*60*60.
     * @return uptime in seconds/day.
     * @see calculateFractionalUptime
     */
    public int calculateDailyUptime() {
        return (int)(calculateFractionalUptime()*(float)SECONDS_PER_DAY);
    }

    /** 
     * Calculates the fraction of time this is running, a unitless quantity
     * between zero and 1.  Implemented using an exponential moving average
     * (EMA) that discounts the past.  Does not update the FRACTION_RUNNING
     * property; that should only be done once, on shutdown
     * @see calculateDailyUptime  
     */
    public float calculateFractionalUptime() { 
        //Let
        //     P = the last value returned by calculateFractionRunning stored
        //         in the SettingsMangager
        //     W = the window size in seconds.  (See note below.)
        //     t = the uptime for this session.  It is assumed that
        //         t<W; otherwise set t=W. 
        //     T = the elapsed time since the end of the previous session, i.e.,
        //         since P' was updated.  Note that t<T.  It is assumed that
        //         T<W; otherwise set T=W.
        //
        //The new fraction P' of the time this is running can be calculated as
        //a weighted average of the current session (t/T) and the past (P):
        //     P' = (T/W)*t/T + (1-T/W)*P
        //        =  t/W      + (W-T)/W*P
        //
        //W's name is misleading, because more than W seconds worth of history
        //are factored into the calculation.  More specifically, a session i
        //days ago contributes 1/W * ((W-1)/W)^i part of the average.  The
        //default value of W (7 days) means, for example, that the past 9 days
        //account for 75% of the calculation.
        SettingsManager settings=SettingsManager.instance();
        final float W=WINDOW_MILLISECONDS;
        float T=Math.min(W, now()-settings.getLastShutdownTime());
        float t=Math.min(W, getUptime());
        float P=settings.getFractionalUptime();
        
        //Occasionally clocks can go backwards, e.g., if user adjusts them or
        //from daylight savings time.  In this case, ignore the current session
        //and just return P.
        if (t<0 || T<0 || t>T)
            return P;

        return t/W + (W-T)/W*P;
    }

    /**
     * Notifies this that LimeWire is shutting down, updating permanent
     * statistics in limewire.props if necessary.  
     */
    public void shutdown() {
        //Order matters, as calculateFractionalUptime() depends on the
        //LAST_SHUTDOWN_TIME property.
        SettingsManager settings=SettingsManager.instance();
        settings.setFractionalUptime(calculateFractionalUptime());
        settings.setLastShutdownTime(now());
    }

    /** The current system time, in milliseconds.  Exists as a hook for testing
     *  purposes only. */
    protected long now() {
        return System.currentTimeMillis();
    }


//  	/**
//  	 * Adds a new TCP ping request to the message statistics.
//  	 */
//  	public void addTCPPingRequest() {
//  		_totalTCPPingRequests++;
//  		_currentTCPPingRequests++;
//  		addTCPMessage();
//  	}

//  	/**
//  	 * Adds a new TCP ping reply to the message statistics.
//  	 */
//  	public void addTCPPingReply() {
//  		_totalTCPPingReplies++;
//  		_currentTCPPingReplies++;
//  		addTCPMessage();
//  	}

//  	/**
//  	 * Adds a new TCP query request to the message statistics.
//  	 */
//  	public void addTCPQueryRequest() {
//  		_totalTCPQueryRequests++;
//  		_currentTCPQueryRequests++;
//  		addTCPMessage();
//  	}

//  	/**
//  	 * Adds a new TCP query reply to the message statistics.
//  	 */
//  	public void addTCPQueryReply() {
//  		_totalTCPQueryReplies++;
//  		_currentTCPQueryReplies++;
//  		addTCPMessage();
//  	}


//  	/**
//  	 * Adds a new TCP push request to the message statistics.
//  	 */
//  	public void addTCPPushRequest() {
//  		_totalTCPPushRequests++;
//  		_currentTCPPushRequests++;
//  		addTCPMessage();
//  	}
	
//  	/**
//  	 * Adds a new TCP route table message to the message statistics.
//  	 */
//  	public void addTCPRouteTableMessage() {
//  		_totalTCPRouteTableMessages++;
//  		_currentTCPRouteTableMessages++;
//  		addTCPMessage();
//  	}

//  	/**
//  	 * Adds a filtered TCP message to the message statistics.
//  	 */
//  	public void addFilteredTCPMessage() {
//  		_totalFilteredTCPMessages++;
//  		_currentFilteredTCPMessages++;
//  		addTCPMessage();
//  	}

//  	/**
//  	 * Adds a TCP message to the data.
//  	 */
//  	private void addTCPMessage() {
//  		_totalTCPMessages++;
//  		_totalMessages++;
//  	}

//  	/**
//  	 * Adds a new UDP ping request to the message statistics.
//  	 */
//  	public void addUDPPingRequest() {
//  		_totalUDPPingRequests++;
//  		_currentUDPPingRequests++;
//  		addUDPMessage();
//  	}

//  	/**
//  	 * Adds a new UDP ping reply to the message statistics.
//  	 */
//  	public void addUDPPingReply() {
//  		_totalUDPPingReplies++;
//  		_currentUDPPingReplies++;
//  		addUDPMessage();
//  	}

//  	/**
//  	 * Adds a new UDP query request to the message statistics.
//  	 */
//  	public void addUDPQueryRequest() {
//  		_totalUDPQueryRequests++;
//  		_currentUDPQueryRequests++;
//  		addUDPMessage();
//  	}

//  	/**
//  	 * Adds a new UDP query reply to the message statistics.
//  	 */
//  	public void addUDPQueryReply() {
//  		_totalUDPQueryReplies++;
//  		_currentUDPQueryReplies++;
//  		addUDPMessage();
//  	}


//  	/**
//  	 * Adds a new UDP push request to the message statistics.
//  	 */
//  	public void addUDPPushRequest() {
//  		_totalUDPPushRequests++;
//  		_currentUDPPushRequests++;
//  		addUDPMessage();
//  	}
	
//  	/**
//  	 * Adds a new UDP route table message to the message statistics.
//  	 */
//  	public void addUDPRouteTableMessage() {
//  		_totalUDPRouteTableMessages++;
//  		_currentUDPRouteTableMessages++;
//  		addUDPMessage();
//  	}

//  	/**
//  	 * Adds a filtered UDP message to the message statistics.
//  	 */
//  	public void addFilteredUDPMessage() {
//  		_totalFilteredUDPMessages++;
//  		_currentFilteredUDPMessages++;
//  		addUDPMessage();
//  	}

//  	/**
//  	 * Adds a UDP message to the data.
//  	 */
//  	private void addUDPMessage() {
//  		_totalUDPMessages++;
//  		_totalMessages++;
//  	}

//  	/**
//  	 * Adds a routing error to the number of routing errors.a
//  	 */
//  	public void addRouteError() {
//  		_totalRouteErrors++;
//  	}

//  	/**
//  	 * Accessor for the total number of dropped messages.
//  	 *
//  	 * @return the total number of dropped messages
//  	 */
//  	public int getTotalDroppedMessages() {
//  		return (_totalRouteErrors + _totalFilteredUDPMessages +
//  				_totalFilteredTCPMessages);		
//  	}

//  	/**
//  	 * Accessor for the total number of TCP ping requests.
//  	 *
//  	 * @return the total number of TCP ping requests
//  	 */
//  	public int getTotalTCPPingRequests() {
//  		return _totalTCPPingRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of TCP ping replies.
//  	 *
//  	 * @return the total number of TCP ping replies
//  	 */
//  	public int getTotalTCPPingReplies() {
//  		return _totalTCPPingReplies;
//  	}

//  	/**
//  	 * Accessor for the total number of TCP query requests.
//  	 *
//  	 * @return the total number of TCP query requests
//  	 */
//  	public int getTotalTCPQueryRequests() {
//  		return _totalTCPQueryRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of TCP query replies.
//  	 *
//  	 * @return the total number of TCP query replies
//  	 */
//  	public int getTotalTCPQueryReplies() {
//  		return _totalTCPQueryReplies;
//  	}

//  	/**
//  	 * Accessor for the total number of TCP push requests.
//  	 *
//  	 * @return the total number of TCP push requests
//  	 */
//  	public int getTotalTCPPushRequests() {
//  		return _totalTCPPushRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of TCP route table messages.
//  	 *
//  	 * @return the total number of TCP route table messages
//  	 */
//  	public int getTotalTCPRouteTableMessages() {
//  		return _totalTCPRouteTableMessages;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP ping requests.
//  	 *
//  	 * @return the current number of TCP ping requests
//  	 */
//  	public int getCurrentTCPPingRequests() {
//  		return _currentTCPPingRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP ping replies.
//  	 *
//  	 * @return the current number of TCP ping replies
//  	 */
//  	public int getCurrentTCPPingReplies() {
//  		return _currentTCPPingReplies;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP query requests.
//  	 *
//  	 * @return the current number of TCP query requests
//  	 */
//  	public int getCurrentTCPQueryRequests() {
//  		return _currentTCPQueryRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP query replies.
//  	 *
//  	 * @return the current number of TCP query replies
//  	 */
//  	public int getCurrentTCPQueryReplies() {
//  		return _currentTCPQueryReplies;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP push requests.
//  	 *
//  	 * @return the current number of TCP push requests
//  	 */
//  	public int getCurrentTCPPushRequests() {
//  		return _currentTCPPushRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of TCP route table messages.
//  	 *
//  	 * @return the current number of TCP route table messages
//  	 */
//  	public int getCurrentTCPRouteTableMessages() {
//  		return _currentTCPRouteTableMessages;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP ping requests.
//  	 *
//  	 * @return the total number of UDP ping requests
//  	 */
//  	public int getTotalUDPPingRequests() {
//  		return _totalUDPPingRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP ping replies.
//  	 *
//  	 * @return the total number of UDP ping replies
//  	 */
//  	public int getTotalUDPPingReplies() {
//  		return _totalUDPPingReplies;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP query requests.
//  	 *
//  	 * @return the total number of UDP query requests
//  	 */
//  	public int getTotalUDPQueryRequests() {
//  		return _totalUDPQueryRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP query replies.
//  	 *
//  	 * @return the total number of UDP query replies
//  	 */
//  	public int getTotalUDPQueryReplies() {
//  		return _totalUDPQueryReplies;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP push requests.
//  	 *
//  	 * @return the total number of UDP push requests
//  	 */
//  	public int getTotalUDPPushRequests() {
//  		return _totalUDPPushRequests;
//  	}

//  	/**
//  	 * Accessor for the total number of UDP route table messages.
//  	 *
//  	 * @return the total number of UDP route table messages
//  	 */
//  	public int getTotalUDPRouteTableMessages() {
//  		return _totalUDPRouteTableMessages;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP ping requests.
//  	 *
//  	 * @return the current number of UDP ping requests
//  	 */
//  	public int getCurrentUDPPingRequests() {
//  		return _currentUDPPingRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP ping replies.
//  	 *
//  	 * @return the current number of UDP ping replies
//  	 */
//  	public int getCurrentUDPPingReplies() {
//  		return _currentUDPPingReplies;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP query requests.
//  	 *
//  	 * @return the current number of UDP query requests
//  	 */
//  	public int getCurrentUDPQueryRequests() {
//  		return _currentUDPQueryRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP query replies.
//  	 *
//  	 * @return the current number of UDP query replies
//  	 */
//  	public int getCurrentUDPQueryReplies() {
//  		return _currentUDPQueryReplies;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP push requests.
//  	 *
//  	 * @return the current number of UDP push requests
//  	 */
//  	public int getCurrentUDPPushRequests() {
//  		return _currentUDPPushRequests;
//  	}

//  	/**
//  	 * Accessor for the current number of UDP route table messages.
//  	 *
//  	 * @return the current number of UDP route table messages
//  	 */
//  	public int getCurrentUDPRouteTableMessages() {
//  		return _currentUDPRouteTableMessages;
//  	}
}
