package com.limegroup.gnutella;

/**
 * This interface outlines the functionality that any class wanting to track
 * bandwidth must implement.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public interface BandwidthTracker {
	
	/**
	 * Returns the number of new bytes passed since the last time this
	 * method was called.
	 *
	 * @return the number of bytes passed since the last time this method 
	 *         was called
	 */
	int getNewBytesTransferred();
}


