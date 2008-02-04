package com.limegroup.gnutella.spam;

/**
 * An abstract Token class, we are using this for the common age() & getAge() classes 
 */
public abstract class AbstractToken implements Token {
    /* Number of LW sessions since this token has been created */
	protected byte _age;
    
    /* Used to cache getImportance() */
    protected transient double _importance = Double.NaN;
    
	AbstractToken() {
		_age = 0;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void incrementAge() {
		if (_age < Byte.MAX_VALUE) {
            synchronized (this) {
			    ++_age;
            }
            // Mark _importance to be lazily calculated when needed
            _importance = Double.NaN;
        }
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public double getImportance() {
        // Avoid race conditions by using a local variable
        double importance = _importance;
        if (Double.isNaN(importance)) {
            // This implements -1 * Gregorio's original misnamed "age()" method.
            // Store bad ratings longer than good ratings since our filter relies
            // mostly on bad ratings.
            importance = (_age * -100.0 * (0.1 + Math.pow(1.0 - getRating(), 0.1)));
            _importance = importance;
        }
        return importance;
	}
    
    /**
     * implements interface <tt>Comparable</tt>
     */
    public int compareTo(Token t) {
        // First, sort by importance
        double importanceDelta = this.getImportance() - t.getImportance();
        // Sort low importance first
        if (importanceDelta < 0.0)
            return -1;
        if (importanceDelta > 0.0)
            return 1;
        
        // Then, sort by type
        int typeDelta = this.getType().ordinal() - t.getType().ordinal();
        if (typeDelta != 0)
            return typeDelta;
        
        // Finally, sort by hashCode to reduce ambiguity
        return this.hashCode() - t.hashCode();
    }
}
