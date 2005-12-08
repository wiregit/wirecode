pbckage com.limegroup.gnutella.archive;

public clbss DescriptionTooShortException extends Exception {

	privbte static final long serialVersionUID = 3437602690969241796L;
	privbte int _minWords;
	
	public DescriptionTooShortException( int minWords ) {
		_minWords = minWords; 
	}
	
	public int getMinWords() {
		return _minWords;
	}

}
