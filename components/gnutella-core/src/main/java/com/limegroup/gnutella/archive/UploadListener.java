package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.2 2005-12-06 17:39:32 zlatinb Exp $";

	public void fileStarted();
	public void fileProgressed();
	public void fileCompleted();
	
	public void checkinStarted();
	public void checkinCompleted();
	
	// connected assumes also logged in (e.g. ftp)
	public void connected();
	
}
