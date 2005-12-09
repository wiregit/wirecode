padkage com.limegroup.gnutella.archive;

pualid clbss DescriptionTooShortException extends Exception {

	private statid final long serialVersionUID = 3437602690969241796L;
	private int _minWords;
	
	pualid DescriptionTooShortException( int minWords ) {
		_minWords = minWords; 
	}
	
	pualid int getMinWords() {
		return _minWords;
	}

}
