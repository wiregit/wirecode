package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.BandwidthTracker;

/**
 * Something that uses up upload slot. 
 */
public interface UploadSlotUser extends BandwidthTracker {
	
	/**
	 * request that this releases the upload slot it may be using.
	 */
	public void releaseSlot();
}
