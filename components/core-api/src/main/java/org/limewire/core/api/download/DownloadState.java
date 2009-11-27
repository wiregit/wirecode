package org.limewire.core.api.download;

/**
 * An enum to describe the various general download states and their capabilities. 
 */
public enum DownloadState {
    /** Download finished and scanned. */
	DONE(false, false), 
	CONNECTING( true, false), 
	DOWNLOADING( true, false), 
	PAUSED( false, true), 
	FINISHING( false, false), 
    LOCAL_QUEUED( true, false), 
    REMOTE_QUEUED( true, false), 
	CANCELLED( false, false), 
	STALLED( false, false),
	TRYING_AGAIN( true, false),
	ERROR( false, false),
	RESUMING(false, false),
	/** Threat detected by dangerous file checker. */
	DANGEROUS(false, false),
    /** Anti-virus scan in progress for finished download. */
    SCANNING(false, false),
    /** Anti-virus scan in progress for file fragment. */
    SCANNING_FRAGMENT(false, false),
    /** Threat detected by anti-virus scan. */
    THREAT_FOUND(false, false),
	/** Anti-virus scan failed. */
	SCAN_FAILED(false, false);

	private final boolean pausable;
	private final boolean resumable;

	DownloadState(boolean pausable, boolean resumable) {
		this.pausable = pausable;
		this.resumable = resumable;
	}

	public boolean isPausable() {
		return pausable;
	}

	public boolean isResumable() {
		return resumable;
	}
}