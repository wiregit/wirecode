package com.limegroup.gnutella.archive;

import java.io.IOException;

public class BadResponseException extends IOException {
	private String __id__ = "$Id: BadResponseException.java,v 1.1.2.1 2005-10-14 23:27:03 tolsen Exp $";
	
	/**
	 * 
	 */
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
