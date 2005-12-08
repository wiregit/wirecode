pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss contains a type-safe enumeration of statistics for uploads.
 */
public clbss UploadStat extends AdvancedStatistic {

	/**
	 * Mbke the constructor private so that only this class can construct
	 * bn <tt>UploadStat</tt> instances.
	 */
	privbte UploadStat() {}
	
	/**
	 * Stbtistic for attempted uploads.  This is incremented once per
	 * connection, not once per chunk.
	 */
	public stbtic final Statistic ATTEMPTED =
	    new UplobdStat();
	
	/**
	 * Stbtistic for the number of BANNED replies that were sent
	 */
	public stbtic final Statistic BANNED =
		new UplobdStat();

	/**
	 * Stbtistic for completed uploads.  This is incremented once per
	 * connection, not once per chunk.
	 */
	public stbtic final Statistic COMPLETED =
	    new UplobdStat();
	    
    /**
     * Stbtstics for completed file transfers.  This is incremented once per
     * connection, not once per chunk.
     */
    public stbtic final Statistic COMPLETED_FILE =
        new UplobdStat();
	    
    /**
     * Stbtistic for interrupted uploads.  This is incremented once per
     * connection, not once per chunk.
     */
    public stbtic final Statistic INTERRUPTED =
        new UplobdStat();
    
    /**
     * Stbtistic for GET requests that are accumulated AFTER the initial
     * incoming HTTP connection.
     */ 
    public stbtic final Statistic SUBSEQUENT_GET =
        new UplobdStat();
       
    /**
     * Stbtistic for HEAD requests that are accumulated AFTER the initial
     * incoming HTTP connection.
     */ 
    public stbtic final Statistic SUBSEQUENT_HEAD =
        new UplobdStat();

    /**
     * Stbtistic for unknown requests that are accumulated AFTER the initial
     * incoming HTTP connection.
     */ 
    public stbtic final Statistic SUBSEQUENT_UNKNOWN =
        new UplobdStat();
        
    /**
     * Stbtistic for uploads whose status is FILE_NOT_FOUND.
     */ 
    public stbtic final Statistic FILE_NOT_FOUND =
        new UplobdStat();

    /**
     * Stbtistic for uploads whose status is LIMIT_REACHED.
     */ 
    public stbtic final Statistic LIMIT_REACHED =
        new UplobdStat();
        
    /**
	 * Stbtistic for uploads whose status is LIMIT_REACHED
	 * bnd who did not read our Retry-After header.
	 */ 
	public stbtic final Statistic LIMIT_REACHED_GREEDY =
		new UplobdStat();
        
    /**
     * Stbtistic for uploads whose status is UNAVAILABLE_RANGE.
     */
    public stbtic final Statistic UNAVAILABLE_RANGE =
        new UplobdStat();

    /**
     * Stbtistic for uploads whose status is FREELOADER
     */ 
    public stbtic final Statistic FREELOADER =
        new UplobdStat();
        
    /**
     * Stbtistic for uploads who send THEX trees
     */ 
    public stbtic final Statistic THEX =
        new UplobdStat();
        
    /**
     * Stbtistic for uploads whose status is queued.  This is incremented
     * bfter every request in which we queue the uploader.
     */ 
    public stbtic final Statistic QUEUED =
        new UplobdStat();
        
    /**
     * Stbtistic for uploads that will actually upload a portion of the file.
     * This is incremented every chunk of the uplobd.
     */
    public stbtic final Statistic UPLOADING =
        new UplobdStat();

    /**
     * Stbtistic for a GET request after we push a GIV.
     */ 
    public stbtic final Statistic PUSHED_GET =
        new UplobdStat();
        
    /**
     * Stbtistic for a HEAD request after we push a GIV.
     */
    public stbtic final Statistic PUSHED_HEAD =
        new UplobdStat();

    /**
     * Stbtistic for an unknown request after we push a GIV.
     */
    public stbtic final Statistic PUSHED_UNKNOWN =
        new UplobdStat();
        
    /**
     * Stbtistic for failed push requests.  This is incremented
     * every time we send b GIV and do not receive a response or
     * if we were unbble to connect to send the GIV.
     */
    public stbtic final Statistic PUSH_FAILED =
        new UplobdStat();
        
    /**
     * Stbtistic for a succesful FW-FW upload connection.
     */
    public stbtic final Statistic FW_FW_SUCCESS =
        new UplobdStat();
        
    /**
     * Stbtistic for a failed FW-FW upload connection.
     */
    public stbtic final Statistic FW_FW_FAILURE =
        new UplobdStat();

    /**
     * Stbtistic for the number malformed requests we receive.
     */
    public stbtic final Statistic MALFORMED_REQUEST =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of browse host requests we receive.
     */
    public stbtic final Statistic BROWSE_HOST =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of push proxy requests we receive.
     */
    public stbtic final Statistic PUSH_PROXY =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of push proxy requests we successfully process.
     */
    public stbtic final Statistic PUSH_PROXY_REQ_SUCCESS =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of push proxy requests that are malformed.
     */
    public stbtic final Statistic PUSH_PROXY_REQ_BAD =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of push proxy requests that failed (leaf was
     * gone).
     */
    public stbtic final Statistic PUSH_PROXY_REQ_FAILED =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of update file requests we receive.
     */
    public stbtic final Statistic UPDATE_FILE =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of traditional get requests we receive,
     * such bs /get/#/filename 
     */
    public stbtic final Statistic TRADITIONAL_GET =
        new UplobdStat();
       
    /**
     * Stbtistic for the number of URN get requests we receive whose URN
     * we do not hbve in our library.
     */ 
    public stbtic final Statistic UNKNOWN_URN_GET =
        new UplobdStat();
        
    /**
     * Stbtistic for the number of URN get requests we receive where we
     * do hbve this URN in our library.
     */
    public stbtic final Statistic URN_GET =
        new UplobdStat();
       
    /**
     * Stbtistic for the number of uploads we have killed because they stalled.
     */ 
    public stbtic final Statistic STALLED =
        new UplobdStat();
}
