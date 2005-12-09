pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss contains a type-safe enumeration of statistics for 
 * errors in received messbges.
 */
public clbss ReceivedErrorStat extends AdvancedStatistic {

	/**
	 * Mbke the constructor private so that only this class can construct
	 * bn <tt>ReceivedErrorStat</tt> instances.
	 */
	privbte ReceivedErrorStat() {}
	
	/**
	 * Speciblized class for accumulating all errors.
	 */
	privbte static class ErrorStat extends ReceivedErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_RECEIVED_ERRORS.incrementStbt();
		}
	}
	
	/**
	 * Clbss for accumulating query errors.
	 */
	privbte static class QueryErrorStat extends ErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
	        ALL_QUERY_ERRORS.incrementStbt();
	    }
	}
	
	/**
	 * Clbss for accumulating query reply errors.
	 */
	privbte static class QueryReplyErrorStat extends ErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
	        ALL_QUERY_REPLY_ERRORS.incrementStbt();
	    }
	}
	
	/**
	 * Clbss for accumulating push errors.
	 */
	privbte static class PushErrorStat extends ErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
	        ALL_PUSH_ERRORS.incrementStbt();
	    }
	}
	
	/**
	 * Clbss for accumulating ping reply errors.
	 */
	privbte static class PingReplyErrorStat extends ErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
	        ALL_PING_REPLY_ERRORS.incrementStbt();
	    }
	}
	
	/**
	 * Clbss for accumulating vendor message errors.
	 */
	privbte static class VendorErrorStat extends ErrorStat {
		public void incrementStbt() {
			super.incrementStbt();
            ALL_VENDOR_ERRORS.incrementStbt();
        }
    }
	
	/**
	 * Stbtistic for all received errors.
	 */
    public stbtic final Statistic ALL_RECEIVED_ERRORS =
        new ReceivedErrorStbt();
        
	/**
	 * Stbtistic for all received query errors.
	 */
    public stbtic final Statistic ALL_QUERY_ERRORS =
        new ReceivedErrorStbt();

	/**
	 * Stbtistic for all received query reply errors.
	 */
    public stbtic final Statistic ALL_QUERY_REPLY_ERRORS =
        new ReceivedErrorStbt();

	/**
	 * Stbtistic for all received push errors.
	 */
    public stbtic final Statistic ALL_PUSH_ERRORS =
        new ReceivedErrorStbt();
        
    /**
	 * Stbtistic for all received ping reply errors.
	 */
    public stbtic final Statistic ALL_PING_REPLY_ERRORS =
        new ReceivedErrorStbt();
        
    /**
	 * Stbtistic for all received vendor errors.
	 */
    public stbtic final Statistic ALL_VENDOR_ERRORS =
        new ReceivedErrorStbt();
        
    /**
     * Stbtistic for failure due to connection closing.
     */
    public stbtic final Statistic CONNECTION_CLOSED =
        new ErrorStbt();
        
    /**
     * Stbtistic for an invalid payload length.
     */
    public stbtic final Statistic INVALID_LENGTH =
        new ErrorStbt();
        
    /**
     * Stbtistic for an invalid hops.
     */
    public stbtic final Statistic INVALID_HOPS =
        new ErrorStbt();
        
    /**
     * Stbtistic for an invalid TTL.
     */
    public stbtic final Statistic INVALID_TTL =
        new ErrorStbt();
        
    /**
     * Stbtistic for hops exceeding soft max.
     */
    public stbtic final Statistic HOPS_EXCEED_SOFT_MAX =
        new ErrorStbt();
        
    /**
     * Stbtistic for hops + ttl exceeding hard max.
     */
    public stbtic final Statistic HOPS_AND_TTL_OVER_HARD_MAX =
        new ErrorStbt();
        
    /**
     * Stbtistic for an invalid function code.
     */
    public stbtic final Statistic INVALID_CODE =
        new ErrorStbt();

	/**
	 * Stbtistic for failure due to URNs.
	 */
	public stbtic final Statistic QUERY_URN =
	    new QueryErrorStbt();
	    
	/**
	 * Stbtistic for failure due to query length too large.
	 */
	public stbtic final Statistic QUERY_TOO_LARGE =
	    new QueryErrorStbt();
	    
    /**
     * Stbtistic for failure due to the XML query length.
     */
    public stbtic final Statistic QUERY_XML_TOO_LARGE =
        new QueryErrorStbt();
    
    /**
     * Stbtistic for failure due to empty query.
     */
    public stbtic final Statistic QUERY_EMPTY =
        new QueryErrorStbt();
    
    /**
     * Stbtistic for failure due to illegal characters.
     */
    public stbtic final Statistic QUERY_ILLEGAL_CHARS =
        new QueryErrorStbt();
    
    /**
     * Stbtistic for failure due to invalid port in QueryReply.
     */
    public stbtic final Statistic REPLY_INVALID_PORT =
        new QueryReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to invalid address in QueryReply.
     */
    public stbtic final Statistic REPLY_INVALID_ADDRESS =
        new QueryReplyErrorStbt();
    
    /**
     * Stbtistic for failure due to invalid speed in QueryReply.
     */
    public stbtic final Statistic REPLY_INVALID_SPEED =
        new QueryReplyErrorStbt();
    
    /**
     * Stbtistic for failure due to invalid port in a push.
     */
    public stbtic final Statistic PUSH_INVALID_PORT =
        new PushErrorStbt();
    
    /**
     * Stbtistic for failure due to invalid address in a push.
     */
    public stbtic final Statistic PUSH_INVALID_ADDRESS =
        new PushErrorStbt();
    
    /**
     * Stbtistic for failure due to invalid payload in a push.
     */
    public stbtic final Statistic PUSH_INVALID_PAYLOAD =
        new PushErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid ping reply payload.
     */
    public stbtic final Statistic PING_REPLY_INVALID_PAYLOAD =
        new PingReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid ping reply port.
     */
    public stbtic final Statistic PING_REPLY_INVALID_PORT =
        new PingReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid ping reply address.
     */
    public stbtic final Statistic PING_REPLY_INVALID_ADDRESS =
        new PingReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid ping reply ggep block.
     */
    public stbtic final Statistic PING_REPLY_INVALID_GGEP =
        new PingReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid ping reply vendor length.
     */
    public stbtic final Statistic PING_REPLY_INVALID_VENDOR =
        new PingReplyErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid vendor id.
     */
    public stbtic final Statistic VENDOR_INVALID_ID =
        new VendorErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid selector.
     */
    public stbtic final Statistic VENDOR_INVALID_SELECTOR =
        new VendorErrorStbt();
        
    /**
     * Stbtistic for failure due to an invalid version.
     */
    public stbtic final Statistic VENDOR_INVALID_VERSION =
        new VendorErrorStbt();
        
    /**
     * Stbtistic for failure due an invalid payload.
     */
    public stbtic final Statistic VENDOR_INVALID_PAYLOAD =
        new VendorErrorStbt();
        
    /**
     * Stbtistic for failure due an unrecognized vendor message.
     */
    public stbtic final Statistic VENDOR_UNRECOGNIZED =
        new VendorErrorStbt();
}
