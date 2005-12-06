package com.limegroup.gnutella.archive;

public final class IdentifierUnavailableException extends Exception {
	
	public static final String REPOSITORY_VERSION = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/IdentifierUnavailableException.java,v 1.2 2005-12-06 17:39:32 zlatinb Exp $";

	
	private static final long serialVersionUID = 1558093218544066639L;
	
	private final String _identifier;
	
	public IdentifierUnavailableException( String message,
			String identifier ) {
		super( "message" );
		_identifier = identifier;
	}
	
	public String getIdentifier() {
		return _identifier;
	}
}
