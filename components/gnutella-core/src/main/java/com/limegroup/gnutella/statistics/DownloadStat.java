package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class contains a type-safe enumeration of statistics for downloads.
 */
public class DownloadStat extends AdvancedStatistic {

	/**
	 * Make the constructor private so that only this class can construct
	 * an <tt>DownloadStat</tt> instances.
	 */
	private DownloadStat() {}
	
	/**
	 * Statistic for direct connection attempts. (This cannot be automatically
	 * increment by the below connection stats because they are performed
	 * multiple times per host.)
	 */
	public static final Statistic CONNECTION_ATTEMPTS = new DownloadStat();
	
	/**
	 * Statistic for direct connection attempts that succeeded.
	 */
	public static final Statistic CONNECT_DIRECT_SUCCESS = new DownloadStat();
    
    /**
     * Statistic for direct connection attempts that failed.
     */ 
    public static final Statistic CONNECT_DIRECT_FAILURES = new DownloadStat();
       
    /**
     * Statistic for pushed connection attempts that succeeded.
     */ 
    public static final Statistic CONNECT_PUSH_SUCCESS = new DownloadStat();

    /**
     * Statistic for pushed connection attempts that failed because
     * we were interrupted for some reason.
     */ 
    public static final Statistic PUSH_FAILURE_INTERRUPTED = new DownloadStat();

    /**
     * Statistic for pushed connection attempts that failed because
     * we didn't receive a GIV from the pushee after a certain time.
     */ 
    public static final Statistic PUSH_FAILURE_NO_RESPONSE = new DownloadStat();
        
    /**
     * Statistics for pushed connection attempts that failed because
     * the socket connection was lost between the time we connected
     * and the time we attempted to retrieve the output stream.
     */
    public static final Statistic PUSH_FAILURE_LOST = new DownloadStat();
        
    /**
     * Statistic for FW-FW downloads that connected.
     */
    public static final Statistic FW_FW_SUCCESS = new DownloadStat();
        
    /**
     * Statistic for FW-FW downloads that failed to connect.
     */
    public static final Statistic FW_FW_FAILURE =
        new DownloadStat();
        
    /**
     * Statistic for attempting to steal from a grey area of another downloader
     * when no such grey area existed.
     */ 
    public static final Statistic NSE_EXCEPTION = new DownloadStat();
        
    /**
     * Statistic for the number of busy download responses.
     */
    public static final Statistic TAL_EXCEPTION = new DownloadStat();

    /**
     * Statistic for the number of range not available download responses.
     */
    public static final Statistic RNA_EXCEPTION = new DownloadStat();

    /**
     * Statistic for the number of file not found download responses.
     */ 
    public static final Statistic FNF_EXCEPTION = new DownloadStat();
        
    /**
     * Statistic for the number of not sharing download responses.
     */
    public static final Statistic NS_EXCEPTION = new DownloadStat();

    /**
     * Statistic for the number of queued download responses.
     */
    public static final Statistic Q_EXCEPTION = new DownloadStat();
        
    /**
     * Statistic for the number of ProblemReadingHeader exceptions
     * while downloading.
     */
    public static final Statistic PRH_EXCEPTION = new DownloadStat();
        
    /**
     * Statistic for the number of Unknown Codes from download responses.
     */
    public static final Statistic UNKNOWN_CODE_EXCEPTION = new DownloadStat();
        
    /**
     * Statistic for the number of IOExceptions while downloading.
     */
    public static final Statistic IO_EXCEPTION = new DownloadStat();

    /**
     * Statistic for the number of NoSuchRangeExceptions while downloading.
     */
    public static final Statistic NSR_EXCEPTION = new DownloadStat();

    /**
	 * Statistic for the number of ContentURNMismatchExceptions from download 
	 * responses.
	 */
	public static final Statistic CONTENT_URN_MISMATCH_EXCEPTION = new DownloadStat();


    /**
     * Statistic for the number of 'ok' responses while downloading.
     */
    public static final Statistic RESPONSE_OK = new DownloadStat();
        
    /**
     * Statistic for the number of alternate locations that we have succesfully
     * read from the network which we will possibly use for this download.
     */
    public static final Statistic ALTERNATE_COLLECTED = new DownloadStat();
        
    /**
     * Statistic for the number of alternate locations that did not work.
     */
    public static final Statistic ALTERNATE_NOT_ADDED = new DownloadStat();
        
    /**
     * Statistic for the number of Alternate Locations that we got off the 
     * network which actually worked
     */
    public static final Statistic ALTERNATE_WORKED = new DownloadStat();
    
    /**
     * Statistic for the number of firewalled alternate locations that we have 
     * succesfully read from the network which we will possibly use for this download.
     */
    public static final Statistic PUSH_ALTERNATE_COLLECTED = new DownloadStat();
    
    /**
     * Statistic for the number of firewalled alternate locations that did not work.
     */
    public static final Statistic PUSH_ALTERNATE_NOT_ADDED = new DownloadStat();
    
    /**
     * Statistic for the number of firewalled Alternate Locations that we got off the 
     * network which actually worked
     */
    public static final Statistic PUSH_ALTERNATE_WORKED = new DownloadStat();
    
    /**
     * Statistic for the number of successfully downloaded HTTP/1.1 chunks.
     */
    public static final Statistic SUCCESSFUL_HTTP11 = new DownloadStat();
       
    /**
     * Statistic for the number of successfully download HTTP/1.0 transfers.
     */ 
    public static final Statistic SUCCESSFUL_HTTP10 = new DownloadStat();
       
    /**
     * Statistic for the number of failed HTTP1.1 chunk downloads.
     */ 
    public static final Statistic FAILED_HTTP11 = new DownloadStat();
       
    /**
     * Statistic for the number of failed HTTP1.0 transfers.
     */ 
    public static final Statistic FAILED_HTTP10 = new DownloadStat();
        
    /**
     * Statistic for the number of once failed sources that are now working.
     */
    public static final Statistic RETRIED_SUCCESS = new DownloadStat();
}
