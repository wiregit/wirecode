package com.limegroup.gnutella.archive;

public class DescriptionTooShortException extends Exception {

	private static final long serialVersionUID = 3437602690969241796L;
	private int _minWords;
    private String _description;
	
	public DescriptionTooShortException( String description, int minWords ) {
		_minWords = minWords; 
        _description = description;
	}
	
	public int getMinWords() {
		return _minWords;
	}

    public String getDescription() {
        return _description;
    }
    
}
