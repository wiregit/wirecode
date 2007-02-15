
package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.DefaultUploadManager;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/**
 * stub for easier testing.  Feel free to override more methods/getters
 */
public class UploadManagerStub extends DefaultUploadManager {
	
	public UploadManagerStub() {
		super(new UploadSlotManager());
	}
	
	int _numQueuedUploads;
	boolean _isBusy;
	boolean _isQueueFull;
	int _uploadsInProgress;
	
	public void setNumQueuedUploads(int what) {
		_numQueuedUploads =what;
	}
	
	public void setUploadsInProgress(int what) {
		_uploadsInProgress = what;
	}
	
	public void setIsBusy(boolean what) {
		_isBusy=what;
	}
	
	public void setIsQueueFull(boolean what) {
		_isQueueFull=what;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#getNumQueuedUploads()
	 */
	public synchronized int getNumQueuedUploads() {
		// TODO Auto-generated method stub
		return _numQueuedUploads;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#isBusy()
	 */
	public synchronized boolean isBusy() {
		// TODO Auto-generated method stub
		return _isBusy;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#isQueueFull()
	 */
	public synchronized boolean isQueueFull() {
		// TODO Auto-generated method stub
		return _isQueueFull;
	}
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#uploadsInProgress()
	 */
	public synchronized int uploadsInProgress() {
		// TODO Auto-generated method stub
		return _uploadsInProgress;
	}
}
