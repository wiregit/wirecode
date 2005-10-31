package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.1.2.3 2005-10-31 20:59:55 tolsen Exp $";

	public void fileStarted( UploadEvent e );
	public void fileProgressed( UploadEvent e );
	public void fileCompleted( UploadEvent e );
	
	// connected assumes also logged in (e.g. ftp)
	public void connected( UploadEvent e );

	// everything's done
	public void completed( UploadEvent e );	
}
