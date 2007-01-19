package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class contains a type-safe enumeration of statistics for 
 * errors in received messages.
 */
public class ReceivedErrorStat extends AdvancedStatistic {

	/**
	 * Make the constructor private so that only this class can construct
	 * an <tt>ReceivedErrorStat</tt> instances.
	 */
	private ReceivedErrorStat() {}
	
	/**
	 * Specialized class for accumulating all errors.
	 */
	private static class ErrorStat extends ReceivedErrorStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_RECEIVED_ERRORS.incrementStat();
		}
	}
	
	/**
	 * Class for accumulating query errors.
	 */
	private static class QueryErrorStat extends ErrorStat {
		public void incrementStat() {
			super.incrementStat();
	        ALL_QUERY_ERRORS.incrementStat();
	    }
	}
	
	/**
	 * Class for accumulating query reply errors.
	 */
	private static class QueryReplyErrorStat extends ErrorStat {
		public void incrementStat() {
			super.incrementStat();
	        ALL_QUERY_REPLY_ERRORS.incrementStat();
	    }
	}
	
	/**
	 * Class for accumulating push errors.
	 */
	private static class PushErrorStat extends ErrorStat {
		public void incrementStat() {
			super.incrementStat();
	        ALL_PUSH_ERRORS.incrementStat();
	    }
	}
	
	/**
	 * Class for accumulating ping reply errors.
	 */
	private static class PingReplyErrorStat extends ErrorStat {
		public void incrementStat() {
			super.incrementStat();
	        ALL_PING_REPLY_ERRORS.incrementStat();
	    }
	}
	
	/**
	 * Class for accumulating vendor message errors.
	 */
	private static class VendorErrorStat extends ErrorStat {
		public void incrementStat() {
			super.incrementStat();
            ALL_VENDOR_ERRORS.incrementStat();
        }
    }
	
	/**
	 * Statistic for all received errors.
	 */
    public static final Statistic ALL_RECEIVED_ERRORS =
        new ReceivedErrorStat();
        
	/**
	 * Statistic for all received query errors.
	 */
    public static final Statistic ALL_QUERY_ERRORS =
        new ReceivedErrorStat();

	/**
	 * Statistic for all received query reply errors.
	 */
    public static final Statistic ALL_QUERY_REPLY_ERRORS =
        new ReceivedErrorStat();

	/**
	 * Statistic for all received push errors.
	 */
    public static final Statistic ALL_PUSH_ERRORS =
        new ReceivedErrorStat();
        
    /**
	 * Statistic for all received ping reply errors.
	 */
    public static final Statistic ALL_PING_REPLY_ERRORS =
        new ReceivedErrorStat();
        
    /**
	 * Statistic for all received vendor errors.
	 */
    public static final Statistic ALL_VENDOR_ERRORS =
        new ReceivedErrorStat();
        
    /**
     * Statistic for failure due to connection closing.
     */
    public static final Statistic CONNECTION_CLOSED =
        new ErrorStat();
        
    /**
     * Statistic for an invalid payload length.
     */
    public static final Statistic INVALID_LENGTH =
        new ErrorStat();
        
    /**
     * Statistic for an invalid hops.
     */
    public static final Statistic INVALID_HOPS =
        new ErrorStat();
        
    /**
     * Statistic for an invalid TTL.
     */
    public static final Statistic INVALID_TTL =
        new ErrorStat();
        
    /**
     * Statistic for hops exceeding soft max.
     */
    public static final Statistic HOPS_EXCEED_SOFT_MAX =
        new ErrorStat();
        
    /**
     * Statistic for hops + ttl exceeding hard max.
     */
    public static final Statistic HOPS_AND_TTL_OVER_HARD_MAX =
        new ErrorStat();
        
    /**
     * Statistic for an invalid function code.
     */
    public static final Statistic INVALID_CODE =
        new ErrorStat();

	/**
	 * Statistic for failure due to URNs.
	 */
	public static final Statistic QUERY_URN =
	    new QueryErrorStat();
	    
	/**
	 * Statistic for failure due to query length too large.
	 */
	public static final Statistic QUERY_TOO_LARGE =
	    new QueryErrorStat();
	    
    /**
     * Statistic for failure due to the XML query length.
     */
    public static final Statistic QUERY_XML_TOO_LARGE =
        new QueryErrorStat();
    
    /**
     * Statistic for failure due to empty query.
     */
    public static final Statistic QUERY_EMPTY =
        new QueryErrorStat();
    
    /**
     * Statistic for failure due to illegal characters.
     */
    public static final Statistic QUERY_ILLEGAL_CHARS =
        new QueryErrorStat();
    
    /**
     * Statistic for failure due to invalid port in QueryReply.
     */
    public static final Statistic REPLY_INVALID_PORT =
        new QueryReplyErrorStat();
        
    /**
     * Statistic for failure due to invalid address in QueryReply.
     */
    public static final Statistic REPLY_INVALID_ADDRESS =
        new QueryReplyErrorStat();
    
    /**
     * Statistic for failure due to invalid speed in QueryReply.
     */
    public static final Statistic REPLY_INVALID_SPEED =
        new QueryReplyErrorStat();
    
    /**
     * Statistic for failure due to invalid port in a push.
     */
    public static final Statistic PUSH_INVALID_PORT =
        new PushErrorStat();
    
    /**
     * Statistic for failure due to invalid address in a push.
     */
    public static final Statistic PUSH_INVALID_ADDRESS =
        new PushErrorStat();
    
    /**
     * Statistic for failure due to invalid payload in a push.
     */
    public static final Statistic PUSH_INVALID_PAYLOAD =
        new PushErrorStat();
        
    /**
     * Statistic for failure due to an invalid ping reply payload.
     */
    public static final Statistic PING_REPLY_INVALID_PAYLOAD =
        new PingReplyErrorStat();
        
    /**
     * Statistic for failure due to an invalid ping reply port.
     */
    public static final Statistic PING_REPLY_INVALID_PORT =
        new PingReplyErrorStat();
        
    /**
     * Statistic for failure due to an invalid ping reply address.
     */
    public static final Statistic PING_REPLY_INVALID_ADDRESS =
        new PingReplyErrorStat();
        
    /**
     * Statistic for failure due to an invalid ping reply ggep block.
     */
    public static final Statistic PING_REPLY_INVALID_GGEP =
        new PingReplyErrorStat();
        
    /**
     * Statistic for failure due to an invalid ping reply vendor length.
     */
    public static final Statistic PING_REPLY_INVALID_VENDOR =
        new PingReplyErrorStat();
        
    /**
     * Statistic for failure due to an invalid vendor id.
     */
    public static final Statistic VENDOR_INVALID_ID =
        new VendorErrorStat();
        
    /**
     * Statistic for failure due to an invalid selector.
     */
    public static final Statistic VENDOR_INVALID_SELECTOR =
        new VendorErrorStat();
        
    /**
     * Statistic for failure due to an invalid version.
     */
    public static final Statistic VENDOR_INVALID_VERSION =
        new VendorErrorStat();
        
    /**
     * Statistic for failure due an invalid payload.
     */
    public static final Statistic VENDOR_INVALID_PAYLOAD =
        new VendorErrorStat();
        
    /**
     * Statistic for failure due an unrecognized vendor message.
     */
    public static final Statistic VENDOR_UNRECOGNIZED =
        new VendorErrorStat();
}
