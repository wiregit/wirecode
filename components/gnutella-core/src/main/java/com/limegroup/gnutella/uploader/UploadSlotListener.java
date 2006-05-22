package com.limegroup.gnutella.uploader;

/**
 * A listener for availability of upload slots. 
 *
 */
public interface UploadSlotListener {
	/**
	 * notification that an upload slot has become
	 * available
	 */
	public void slotAvailable();
	
	/**
	 * request that this releases an upload slot if it
	 * has one (effectively killing an upload)
	 */
	public void releaseSlot();
}
