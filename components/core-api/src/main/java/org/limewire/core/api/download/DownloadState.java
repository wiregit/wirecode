package org.limewire.core.api.download;

public enum DownloadState {
	//(cancellable, pausable, resumable, searchAgainable)
	DONE(false, false, false, false), 
	CONNECTING(true, false, false, false), 
	DOWNLOADING(true, true, false, false), 
	PAUSED(true, false, true, false), 
	FINISHING(true, false, false, false), 
	QUEUED(true, false, false, false), 
	CANCELLED(false, false, false, false), 
	STALLED(true, false, false, true), 
	ERROR(false, false, false, false);

	private boolean cancellable;
	private boolean pausable;
	private boolean resumable;
	private boolean searchAgainable;

	DownloadState(boolean cancellable, boolean pausable, boolean resumable,
			boolean searchAgainable) {
		this.cancellable = cancellable;
		this.pausable = pausable;
		this.resumable = resumable;
		this.searchAgainable = searchAgainable;
	}

	public boolean isCancellable() {
		return cancellable;
	}

	public boolean isPausable() {
		return pausable;
	}

	public boolean isResumable() {
		return resumable;
	}

	public boolean isSearchAgainable() {
		return searchAgainable;
	}
	
	
}
