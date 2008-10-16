package com.limegroup.gnutella.spam;

/**
 * A token representing a keyword from a file name or query string
 */
public class KeywordToken extends Token {
	private static final long serialVersionUID = 3257850995487748662L;
    
    /**
     * We should be very careful about treating keywords as indicators of
     * spam, since spammers often echo the query string in the file name.
     */
    private static final float KEYWORD_WEIGHT = 0.01f;
    
	protected final String keyword;
    
	KeywordToken(String keyword) {
        this.keyword = keyword;
	}
    
    @Override
	protected float getWeight() {
        return KEYWORD_WEIGHT;
    }
    
    @Override
    public int hashCode() {
        return keyword.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof KeywordToken))
            return false;
        return keyword.equals(((KeywordToken)o).keyword);
    }
    
	@Override
    public String toString() {
		return keyword + " " + rating;
	}
}
