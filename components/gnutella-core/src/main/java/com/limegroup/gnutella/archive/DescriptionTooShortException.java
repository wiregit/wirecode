package com.limegroup.gnutella.archive;

public class DescriptionTooShortException extends Exception {

	private static final long serialVersionUID = 3437602690969241796L;
	private int _minWords;
	
	public DescriptionTooShortException( int minWords ) {
		_minWords = minWords; 
	}
	
	public int getMinWords() {
		return _minWords;
	}

}
