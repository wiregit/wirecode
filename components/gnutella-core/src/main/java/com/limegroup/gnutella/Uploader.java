package com.limegroup.gnutella;

import java.io.IOException;

/**
 * This is the Uploader interface.  There is a stop
 * method, but no start.  
 * 
 */

public interface Uploader {


  //  	public static final int NOT_CONNECTED    = 0;
//  	public static final int CONNECTED        = 1;
//  	public static final int COMPLETE         = 2;
//  	public static final int LIMIT_REACHED    = 3;
//  	public static final int PUSH_FAILED      = 4;
//  	public static final int FREELOADER       = 5;
//  	public static final int UPLOADING        = 6;
//  	public static final int INTERRUPTED      = 7;

	public static final int CONNECTING       = 0;
	public static final int FREELOADER       = 1;
	public static final int LIMIT_REACHED    = 2;
	public static final int UPLOADING        = 3;
	public static final int COMPLETE         = 4;
	public static final int INTERRUPTED      = 5;
	public static final int PUSH_FAILED      = 6;
	public static final int FILE_NOT_FOUND   = 7;

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
	 * returns the string representation of the IP Address
	 * of the host being uploaded to.
	 */
	public String getHost();

    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP
     */
    public int getState();


	public void connect() throws IOException;
	public void start();
	public void setState(int state);
	public void setAmountUploaded(int amount);

}

