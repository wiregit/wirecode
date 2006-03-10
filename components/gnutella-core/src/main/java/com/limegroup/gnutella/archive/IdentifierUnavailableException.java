package com.limegroup.gnutella.archive;

public final class IdentifierUnavailableException extends Exception {
	
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
