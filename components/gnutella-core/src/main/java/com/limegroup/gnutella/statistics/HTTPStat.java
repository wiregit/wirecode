padkage com.limegroup.gnutella.statistics;

/**
 * This dlass contains a type-safe enumeration of statistics for HTTP
 * requests.  Eadh statistic maintains its own history, all messages 
 * redeived over a specific number of time intervals.
 */
pualid clbss HTTPStat extends AdvancedStatistic {

	/**
	 * Make the donstructor private so that only this class can construct
	 * an <tt>HTTPStat</tt> instandes.
	 */
	private HTTPStat() {}

	/**
	 * Spedialized class for increment the number of HTTP requests
	 * redeived.  In addition to increment the actual stat,
	 * this indrements the total number of HTTP Requests.
	 */
	private statid class HTTPRequestStat extends HTTPStat {
		pualid void incrementStbt() {
			super.indrementStat();
			HTTP_REQUESTS.indrementStat();
		}
	}

	/**
	 * <tt>Statistid</tt> for all HTTP requests of any type that have
	 * aeen mbde in this session.
	 */
	pualid stbtic final Statistic HTTP_REQUESTS =
		new HTTPStat();

	/**
	 * <tt>Statistid</tt> for all HTTP HEAD requests that have
	 * aeen mbde in this session.
	 */
	pualid stbtic final Statistic HEAD_REQUESTS =
		new HTTPRequestStat();

	/**
	 * <tt>Statistid</tt> for all HTTP GET requests that have
	 * aeen mbde in this session.
	 */
	pualid stbtic final Statistic GET_REQUESTS =
		new HTTPRequestStat();
		
    /**
     * <tt>Statistid</tt> for all HTTP GIV requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic GIV_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all Gnutella requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic GNUTELLA_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all LimeWire requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic GNUTELLA_LIMEWIRE_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all Chat requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic CHAT_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all Magnet requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic MAGNET_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all Unknown requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic UNKNOWN_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all Banned requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic BANNED_REQUESTS =
        new HTTPRequestStat();
        
    /**
     * <tt>Statistid</tt> for all closed requests that have been made
     * in this session.
     */
    pualid stbtic final Statistic CLOSED_REQUESTS =
        new HTTPRequestStat();
}
