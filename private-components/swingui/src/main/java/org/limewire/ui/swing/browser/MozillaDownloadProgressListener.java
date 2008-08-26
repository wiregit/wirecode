package org.limewire.ui.swing.browser;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;

public class MozillaDownloadProgressListener implements
		nsIDownloadProgressListener {
	private final long downloadId;
	private final AtomicInteger state;
	private final AtomicLong selfProgress;
	private final AtomicLong totalProgress;
	private final AtomicLong lastUpdateTime;
	

	public MozillaDownloadProgressListener(long downloadId, short state) {
		this.downloadId = downloadId;
		this.state = new AtomicInteger(state);
		this.selfProgress = new AtomicLong();
		this.totalProgress = new AtomicLong();
		this.lastUpdateTime = new AtomicLong(System.currentTimeMillis());
		System.out.println("starting state: " + state);
	}

	@Override
	public void onDownloadStateChange(short state, nsIDownload download) {
		if (this.downloadId == download.getId()) {
			this.state.set(state);
			// this is my download
			System.out.println("state: " + this.state);
		}
	}

	@Override
	public void onProgressChange(nsIWebProgress webProgress,
			nsIRequest request, long curSelfProgress, long maxSelfProgress,
			long curTotalProgress, long maxTotalProgress, nsIDownload download) {
		
		if (this.downloadId == download.getId()) {
			// this is my download
			
			long diff = curTotalProgress - totalProgress.longValue();
			
			
			selfProgress.set(curSelfProgress);
			totalProgress.set(curTotalProgress);
			System.out.println("curSelfProgress: " + selfProgress);
			System.out.println("curTotalProgress: " + totalProgress);
		}
	}

	@Override
	public void onSecurityChange(nsIWebProgress webProgress,
			nsIRequest request, long state, nsIDownload download) {
		// don't care about this event.
	}

	@Override
	public void onStateChange(nsIWebProgress webProgress, nsIRequest request,
			long stateFlags, long status, nsIDownload download) {
		// no longer used by mozilla api
	}

	@Override
	public void setDocument(nsIDOMDocument document) {
		// no mozilla window to use
	}

	@Override
	public nsIDOMDocument getDocument() {
		// no mozilla window to use
		return null;
	}

	@Override
	public nsISupports queryInterface(String uuid) {
		return Mozilla.queryInterface(this, uuid);
	}
}