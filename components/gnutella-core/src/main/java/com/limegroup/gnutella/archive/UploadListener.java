package com.limegroup.gnutella.archive;

public interface UploadListener {

	public void fileStarted();
	public void fileProgressed();
	public void fileCompleted();
	
	public void checkinStarted();
	public void checkinCompleted();
	
	// connected assumes also logged in (e.g. ftp)
	public void connected();
	
}
