package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.1.2.6 2005-11-03 22:35:28 tolsen Exp $";

	public void fileStarted( UploadEvent e );
	public void fileProgressed( UploadEvent e );
	public void fileCompleted( UploadEvent e );
	
	public void checkinStarted( UploadEvent e );
	public void checkinCompleted( UploadEvent e );
	
	// connected assumes also logged in (e.g. ftp)
	public void connected( UploadEvent e );
	
}
