package com.limegroup.gnutella.archive;

public interface FTPUploadListener extends UploadListener {
	
	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/FTPUploadListener.java,v 1.1.2.1 2005-10-26 20:02:48 tolsen Exp $";

	public void connected( UploadEvent e );
	public void loggedIn( UploadEvent e );
	public void dirChanged( UploadEvent e );
}
