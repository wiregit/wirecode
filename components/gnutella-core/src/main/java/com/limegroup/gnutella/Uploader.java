package com.limegroup.gnutella;

import java.io.IOException;

/**
 * This is the Uploader interface.  There is a stop
 * method, but no start.  
 * 
 */

public interface Uploader extends Transfer {

	public static final int CONNECTING       = 0;
	public static final int FREELOADER       = 1;
	public static final int LIMIT_REACHED    = 2;
	public static final int UPLOADING        = 3;
	public static final int COMPLETE         = 4;
	public static final int INTERRUPTED      = 5;
	public static final int PUSH_FAILED      = 6;
	public static final int FILE_NOT_FOUND   = 7;

	/**
	 * returns the index of the file being uploaded.
	 */ 
	public int getIndex();

	public void connect() throws IOException;
	public void start();
	public void setState(int state);
	public void setAmountTransfered(int amount);

}

