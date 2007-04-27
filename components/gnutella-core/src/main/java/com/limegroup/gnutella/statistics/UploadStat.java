package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;

/**
 * This class contains a type-safe enumeration of statistics for uploads.
 */
public class UploadStat extends AdvancedStatistic {

	/**
	 * Make the constructor private so that only this class can construct
	 * an <tt>UploadStat</tt> instances.
	 */
	private UploadStat() {}
	
	/**
	 * Statistic for attempted uploads.  This is incremented once per
	 * connection, not once per chunk.
	 */
	public static final Statistic ATTEMPTED =
	    new UploadStat();
	
	/**
	 * Statistic for the number of BANNED replies that were sent
	 */
	public static final Statistic BANNED =
		new UploadStat();

	/**
	 * Statistic for completed uploads.  This is incremented once per
	 * connection, not once per chunk.
	 */
	public static final Statistic COMPLETED =
	    new UploadStat();
	    
    /**
     * Statstics for completed file transfers.  This is incremented once per
     * connection, not once per chunk.
     */
    public static final Statistic COMPLETED_FILE =
        new UploadStat();
	    
    /**
     * Statistic for interrupted uploads.  This is incremented once per
     * connection, not once per chunk.
     */
    public static final Statistic INTERRUPTED =
        new UploadStat();
    
//    /**
//     * Statistic for GET requests that are accumulated AFTER the initial
//     * incoming HTTP connection.
//     */ 
//    public static final Statistic SUBSEQUENT_GET =
//        new UploadStat();
//       
//    /**
//     * Statistic for HEAD requests that are accumulated AFTER the initial
//     * incoming HTTP connection.
//     */ 
//    public static final Statistic SUBSEQUENT_HEAD =
//        new UploadStat();
//
//    /**
//     * Statistic for unknown requests that are accumulated AFTER the initial
//     * incoming HTTP connection.
//     */ 
//    public static final Statistic SUBSEQUENT_UNKNOWN =
//        new UploadStat();
        
    /**
     * Statistic for uploads whose status is FILE_NOT_FOUND.
     */ 
    public static final Statistic FILE_NOT_FOUND =
        new UploadStat();

    /**
     * Statistic for uploads whose status is LIMIT_REACHED.
     */ 
    public static final Statistic LIMIT_REACHED =
        new UploadStat();
        
    /**
	 * Statistic for uploads whose status is LIMIT_REACHED
	 * and who did not read our Retry-After header.
	 */ 
	public static final Statistic LIMIT_REACHED_GREEDY =
		new UploadStat();
        
    /**
     * Statistic for uploads whose status is UNAVAILABLE_RANGE.
     */
    public static final Statistic UNAVAILABLE_RANGE =
        new UploadStat();

    /**
     * Statistic for uploads whose status is FREELOADER
     */ 
    public static final Statistic FREELOADER =
        new UploadStat();
        
    /**
     * Statistic for uploads who send THEX trees
     */ 
    public static final Statistic THEX =
        new UploadStat();
        
    /**
     * Statistic for uploads whose status is queued.  This is incremented
     * after every request in which we queue the uploader.
     */ 
    public static final Statistic QUEUED =
        new UploadStat();
        
    /**
     * Statistic for uploads that will actually upload a portion of the file.
     * This is incremented every chunk of the upload.
     */
    public static final Statistic UPLOADING =
        new UploadStat();

    /**
     * Statistic for a GET request after we push a GIV.
     */ 
    public static final Statistic PUSHED_GET =
        new UploadStat();
        
    /**
     * Statistic for a HEAD request after we push a GIV.
     */
    public static final Statistic PUSHED_HEAD =
        new UploadStat();

    /**
     * Statistic for an unknown request after we push a GIV.
     */
    public static final Statistic PUSHED_UNKNOWN =
        new UploadStat();
        
    /**
     * Statistic for failed push requests.  This is incremented
     * every time we send a GIV and do not receive a response or
     * if we were unable to connect to send the GIV.
     */
    public static final Statistic PUSH_FAILED =
        new UploadStat();
        
    /**
     * Statistic for a succesful FW-FW upload connection.
     */
    public static final Statistic FW_FW_SUCCESS =
        new UploadStat();
        
    /**
     * Statistic for a failed FW-FW upload connection.
     */
    public static final Statistic FW_FW_FAILURE =
        new UploadStat();

    /**
     * Statistic for the number malformed requests we receive.
     */
    public static final Statistic MALFORMED_REQUEST =
        new UploadStat();
        
    /**
     * Statistic for the number of browse host requests we receive.
     */
    public static final Statistic BROWSE_HOST =
        new UploadStat();
        
    /**
     * Statistic for the number of push proxy requests we receive.
     */
    public static final Statistic PUSH_PROXY =
        new UploadStat();
        
    /**
     * Statistic for the number of push proxy requests we successfully process.
     */
    public static final Statistic PUSH_PROXY_REQ_SUCCESS =
        new UploadStat();
        
    /**
     * Statistic for the number of push proxy requests that are malformed.
     */
    public static final Statistic PUSH_PROXY_REQ_BAD =
        new UploadStat();
        
    /**
     * Statistic for the number of push proxy requests that failed (leaf was
     * gone).
     */
    public static final Statistic PUSH_PROXY_REQ_FAILED =
        new UploadStat();
        
    /**
     * Statistic for the number of update file requests we receive.
     */
    public static final Statistic UPDATE_FILE =
        new UploadStat();
        
    /**
     * Statistic for the number of traditional get requests we receive,
     * such as /get/#/filename 
     */
    public static final Statistic TRADITIONAL_GET =
        new UploadStat();
       
    /**
     * Statistic for the number of URN get requests we receive whose URN
     * we do not have in our library.
     */ 
    public static final Statistic UNKNOWN_URN_GET =
        new UploadStat();
        
    /**
     * Statistic for the number of URN get requests we receive where we
     * do have this URN in our library.
     */
    public static final Statistic URN_GET =
        new UploadStat();
       
    /**
     * Statistic for the number of uploads we have killed because they stalled.
     */ 
    public static final Statistic STALLED =
        new UploadStat();
}
