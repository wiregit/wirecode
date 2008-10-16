package com.limegroup.gnutella.spam;

/**
 * A token representing a name/value pair from XML metadata
 */
public class XMLKeywordToken extends KeywordToken {
	private static final long serialVersionUID = 3617573808026760503L;
    
    /**
     * XML name/value pairs are more specific than keywords, so we require
     * fewer updates before considering them spam.
     */
    private static final float XML_WEIGHT = 0.1f;

	XMLKeywordToken(String name, String value) {
		super(name + ":" + value);
	}
    
    @Override
    protected float getWeight() {
        return XML_WEIGHT;
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof XMLKeywordToken))
            return false;
        return keyword.equals(((XMLKeywordToken)o).keyword);
    }
}
