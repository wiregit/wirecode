package com.limegroup.gnutella;

public interface Transfer {

	/**
	 * Stops this transfer.  If the transfer is already 
	 * stopped, it does nothing.
	 */ 
	public void stop();

    /**
     * Returns the state of this: one of QUEUED, CONNECTING, DOWNLOADING,
     * WAITING_FOR_RETRY, COMPLETE, ABORTED, GAVE_UP
     */
    public int getState();

	/**
     * Returns the String representation of the state.
     */
	public String getStatus();
	
	/**
	 * returns the length of the file being uploaded.
	 */ 
	public int getFileSize();
	
	/** 
     * Returns the name of the current or last file this is downloading, 
	 * or null in the rare case that this has no more files to download.
	 * (This might happen if this has been stopped.)
     */
    public String getFileName();

    /**
     * Returns the address of the downloader, or null if this is not currently
     * connected. 
     */
    public String getHost();

	/**
	 * returns the amount transfered
	 */
	public int getAmountTransfered();
}
