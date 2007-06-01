
package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/**
 * stub for easier testing.  Feel free to override more methods/getters
 */
public class UploadManagerStub extends UploadManager {
	
	public UploadManagerStub() {
		super(new UploadSlotManager());
	}
	
	private int _numQueuedUploads;
    private int _uploadsInProgress;
	
	public void setNumQueuedUploads(int what) {
		_numQueuedUploads =what;
	}
	
	public void setUploadsInProgress(int what) {
		_uploadsInProgress = what;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#getNumQueuedUploads()
	 */
    @Override
	public synchronized int getNumQueuedUploads() {
		// TODO Auto-generated method stub
		return _numQueuedUploads;
	}
    
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#uploadsInProgress()
	 */
    @Override
	public synchronized int uploadsInProgress() {
		// TODO Auto-generated method stub
		return _uploadsInProgress;
	}
}
