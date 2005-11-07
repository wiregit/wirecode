package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.1.2.7 2005-11-07 17:00:18 zlatinb Exp $";

	public void fileStarted( AbstractContribution e );
	public void fileProgressed( AbstractContribution e );
	public void fileCompleted( AbstractContribution e );
	
	public void checkinStarted( AbstractContribution e );
	public void checkinCompleted( AbstractContribution e );
	
	// connected assumes also logged in (e.g. ftp)
	public void connected( AbstractContribution e );
	
}
