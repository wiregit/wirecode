package com.limegroup.gnutella;

import com.limegroup.gnutella.http.HTTPRequestMethod;
import java.io.IOException;
import java.net.*;

/**
 * This interface outlines the basic functionality for a class that 
 * performs uploads.
 */
public interface Uploader extends BandwidthTracker {

	public static final int CONNECTING       = 0;
	public static final int FREELOADER       = 1;
	public static final int LIMIT_REACHED    = 2;
	public static final int UPLOADING        = 3;
	public static final int COMPLETE         = 4;
	public static final int INTERRUPTED      = 5;
	public static final int PUSH_FAILED      = 6;
	public static final int FILE_NOT_FOUND   = 7;
    public static final int BROWSE_HOST      = 8;
    public static final int QUEUED           = 9;
    public static final int UPDATE_FILE      = 10;
    public static final int MALFORMED_REQUEST = 11;
    public static final int PUSH_PROXY       = 12;

    /**
	 * Stops this upload.  If the download is already 
	 * stopped, it does nothing.
	 */ 
	public void stop();
    
	/**
	 * returns the name of the file being uploaded.
	 */
	public String getFileName();
	
	/**
	 * returns the length of the file being uploaded.
	 */ 
	public int getFileSize();
	
	/**
	 * returns the length of the requested size for uploading
	 */ 
	public int getAmountRequested();	

	/**
	 * Returns the <tt>FileDesc</tt> for this uploader -- the file that
	 * is being uploaded.
	 *
	 * @return the <tt>FileDesc</tt> for this uploader -- the file that
	 *  is being uploaded, which can be <tt>null</tt> in cases such as when
	 *  the file can't be found
	 */
	public FileDesc getFileDesc();

	/**
	 * returns the index of the file being uploaded.
	 */ 
	public int getIndex();

	/**
	 * returns the amount that of data that has been uploaded.
	 * this method was previously called "amountRead", but the
	 * name was changed to make more sense.
	 */ 
	public int amountUploaded();
	
	/**
	 * Returns the amount of data that this uploader and all previous
	 * uploaders exchanging this file have uploaded.
	 */
	public int getTotalAmountUploaded();

	/**
	 * returns the string representation of the IP Address
	 * of the host being uploaded to.
	 */
	public String getHost();

    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP
     */
    public int getState();

	/**
	 * Sets the state of this uploader.
	 */
	public void setState(int state);

	public void writeResponse();

	/**
	 * returns true if chat for the host is on, false if it is not.
	 */
	public boolean isChatEnabled();
	
	/**
	 * returns true if browse host is enabled, false if it is not.
	 */
	public boolean isBrowseHostEnabled();
	
	/**
	 * return the port of the gnutella-client host (not the HTTP port)
	 */
	public int getGnutellaPort();
	
	/** 
	 * return the userAgent
	 */
	public String getUserAgent();
	
	/** 
	 * return whether or not the headers have been parsed
	 */
	public boolean isHeaderParsed();

    public boolean supportsQueueing();
    
    /**
     * returns the current request method.
     */
    public HTTPRequestMethod getMethod();
    
    /**
     * Returns the current queue position if queued.
     */
    public int getQueuePosition();
    
    /**
     * Returns whether or not the uploader is in an inactive state.
     */
    public boolean isInactive();

}

