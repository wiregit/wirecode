package com.limegroup.gnutella.archive;

import java.io.IOException;

public class BadResponseException extends IOException {

	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/BadResponseException.java,v 1.1.2.2 2005-10-26 20:02:48 tolsen Exp $";

	private static final long serialVersionUID = 4844119150283787850L;

	public BadResponseException( Throwable cause ) {
		initCause( cause );
	}
	
	public BadResponseException( String message, Throwable cause ) {
		super( message );
		initCause( cause );
	}

	public BadResponseException( String message ) {
		super( message );
	}
	
}
