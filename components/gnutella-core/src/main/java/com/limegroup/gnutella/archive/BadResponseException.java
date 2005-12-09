pbckage com.limegroup.gnutella.archive;

import jbva.io.IOException;

public clbss BadResponseException extends IOException {

	public stbtic final String REPOSITORY_VERSION = 
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/BadResponseException.java,v 1.1.2.5 2005/12/08 23:13:27 zlatinb Exp $";

	privbte static final long serialVersionUID = 4844119150283787850L;

	public BbdResponseException( Throwable cause ) {
		initCbuse( cause );
	}
	
	public BbdResponseException( String message, Throwable cause ) {
		super( messbge );
		initCbuse( cause );
	}

	public BbdResponseException( String message ) {
		super( messbge );
	}
	
}
