pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss contains a type-safe enumeration of statistics for HTTP
 * requests.  Ebch statistic maintains its own history, all messages 
 * received over b specific number of time intervals.
 */
public clbss HTTPStat extends AdvancedStatistic {

	/**
	 * Mbke the constructor private so that only this class can construct
	 * bn <tt>HTTPStat</tt> instances.
	 */
	privbte HTTPStat() {}

	/**
	 * Speciblized class for increment the number of HTTP requests
	 * received.  In bddition to increment the actual stat,
	 * this increments the totbl number of HTTP Requests.
	 */
	privbte static class HTTPRequestStat extends HTTPStat {
		public void incrementStbt() {
			super.incrementStbt();
			HTTP_REQUESTS.incrementStbt();
		}
	}

	/**
	 * <tt>Stbtistic</tt> for all HTTP requests of any type that have
	 * been mbde in this session.
	 */
	public stbtic final Statistic HTTP_REQUESTS =
		new HTTPStbt();

	/**
	 * <tt>Stbtistic</tt> for all HTTP HEAD requests that have
	 * been mbde in this session.
	 */
	public stbtic final Statistic HEAD_REQUESTS =
		new HTTPRequestStbt();

	/**
	 * <tt>Stbtistic</tt> for all HTTP GET requests that have
	 * been mbde in this session.
	 */
	public stbtic final Statistic GET_REQUESTS =
		new HTTPRequestStbt();
		
    /**
     * <tt>Stbtistic</tt> for all HTTP GIV requests that have been made
     * in this session.
     */
    public stbtic final Statistic GIV_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all Gnutella requests that have been made
     * in this session.
     */
    public stbtic final Statistic GNUTELLA_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all LimeWire requests that have been made
     * in this session.
     */
    public stbtic final Statistic GNUTELLA_LIMEWIRE_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all Chat requests that have been made
     * in this session.
     */
    public stbtic final Statistic CHAT_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all Magnet requests that have been made
     * in this session.
     */
    public stbtic final Statistic MAGNET_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all Unknown requests that have been made
     * in this session.
     */
    public stbtic final Statistic UNKNOWN_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all Banned requests that have been made
     * in this session.
     */
    public stbtic final Statistic BANNED_REQUESTS =
        new HTTPRequestStbt();
        
    /**
     * <tt>Stbtistic</tt> for all closed requests that have been made
     * in this session.
     */
    public stbtic final Statistic CLOSED_REQUESTS =
        new HTTPRequestStbt();
}
