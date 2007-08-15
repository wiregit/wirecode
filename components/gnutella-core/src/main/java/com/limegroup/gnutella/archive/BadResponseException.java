package com.limegroup.gnutella.archive;

import java.io.IOException;

public class BadResponseException extends IOException {
    
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
