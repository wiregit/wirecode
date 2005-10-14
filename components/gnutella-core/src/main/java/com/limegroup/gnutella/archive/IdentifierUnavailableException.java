package com.limegroup.gnutella.archive;

public final class IdentifierUnavailableException extends Exception {
	private String __id__ = "$Id: IdentifierUnavailableException.java,v 1.1.2.1 2005-10-14 23:27:03 tolsen Exp $";
	
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
