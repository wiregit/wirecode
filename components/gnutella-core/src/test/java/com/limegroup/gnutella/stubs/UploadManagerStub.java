
package com.limegroup.gnutella.stubs;

import org.limewire.concurrent.Providers;

import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.uploader.UploadSlotManagerImpl;

/**
 * stub for easier testing.  Feel free to override more methods/getters
 */
public class UploadManagerStub extends HTTPUploadManager {
	
	public UploadManagerStub() {
		super(new UploadSlotManagerImpl(), ProviderHacks.getHttpRequestHandlerFactory(), Providers.of(ProviderHacks.getContentManager()));
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
