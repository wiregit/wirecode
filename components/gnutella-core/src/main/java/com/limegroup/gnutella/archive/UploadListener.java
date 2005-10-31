package com.limegroup.gnutella.archive;

public interface UploadListener {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/UploadListener.java,v 1.1.2.2 2005-10-31 20:51:17 tolsen Exp $";

	public void fileStarted( UploadEvent e );
	public void fileProgressed( UploadEvent e );
	public void fileCompleted( UploadEvent e );
	public void connected( UploadEvent e );
	public void loggedIn( UploadEvent e );
	public void dirChanged( UploadEvent e );	
	
}
