package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.1.2.4 2005-10-31 22:06:09 tolsen Exp $";

	public void fileStarted( UploadEvent e );
	public void fileProgressed( UploadEvent e );
	public void fileCompleted( UploadEvent e );
	
	// connected assumes also logged in (e.g. ftp)
	public void connected( UploadEvent e );
	
}
