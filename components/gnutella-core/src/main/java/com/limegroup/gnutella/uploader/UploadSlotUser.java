package com.limegroup.gnutella.uploader;

/**
 * Something that uses up upload slot. 
 */
public interface UploadSlotUser {
	
	/**
	 * request that this releases the upload slot it may be using.
	 */
	public void releaseSlot();
}
