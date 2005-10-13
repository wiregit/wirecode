package com.limegroup.gnutella.spam;

/**
 * An abstract Token class, we are using this for the common age() & getAge() classes 
 */
public abstract class AbstractToken implements Token {
    /* Number of LW sessions since this token has been created */
	byte _age;
    
    /* Used to cache getImportance() */
    protected transient double _importance = Double.NaN;
    
	AbstractToken() {
		_age = 0;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void incrimentAge() {
		if (_age < Byte.MAX_VALUE) {
			++_age;
            _importance = Double.NaN;
        }
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public double getImportance() {
        if (_importance == Double.NaN) {
            // This implements -1 * Gregorio's original misnamed "age()" method.
            // Store bad ratings longer than good ratings since our filter relies
            // mostly on bad ratings.
            _importance = (_age * -100.0 * (0.1 + Math.pow(1.0 - getRating(), 0.1)));
        }
        return _importance;
	}
    
    /**
     * implements interface <tt>Comparable</tt>
     */
    public int compareTo(Object o) {
        // This may throw a class cast exception, 
        // copying the Java 1.5 semantics of Comparable
        Token t = (Token) o;
        
        // First, sort by importance
        double importanceDelta = this.getImportance() - t.getImportance();
        // Sort high importance first, so reverse the return value
        if (importanceDelta < 0.0)
            return 1;
        if (importanceDelta > 0.0)
            return -1;
        
        // Then, sort by type
        int typeDelta = this.getType() - t.getType();
        if (typeDelta != 0)
            return typeDelta;
        
        // Finally, sort by hashCode to reduce ambiguity
        return this.hashCode() - t.hashCode();
    }
}
