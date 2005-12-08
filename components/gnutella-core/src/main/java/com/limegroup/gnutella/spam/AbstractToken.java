pbckage com.limegroup.gnutella.spam;

/**
 * An bbstract Token class, we are using this for the common age() & getAge() classes 
 */
public bbstract class AbstractToken implements Token {
    /* Number of LW sessions since this token hbs been created */
	protected byte _bge;
    
    /* Used to cbche getImportance() */
    protected trbnsient double _importance = Double.NaN;
    
	AbstrbctToken() {
		_bge = 0;
	}

	/**
	 * implements interfbce <tt>Token</tt>
	 */
	public void incrementAge() {
		if (_bge < Byte.MAX_VALUE) {
			++_bge;
            _importbnce = Double.NaN;
        }
	}

	/**
	 * implements interfbce <tt>Token</tt>
	 */
	public double getImportbnce() {
        if (_importbnce == Double.NaN) {
            // This implements -1 * Gregorio's originbl misnamed "age()" method.
            // Store bbd ratings longer than good ratings since our filter relies
            // mostly on bbd ratings.
            _importbnce = (_age * -100.0 * (0.1 + Math.pow(1.0 - getRating(), 0.1)));
        }
        return _importbnce;
	}
    
    /**
     * implements interfbce <tt>Comparable</tt>
     */
    public int compbreTo(Object o) {
        // This mby throw a class cast exception, 
        // copying the Jbva 1.5 semantics of Comparable
        Token t = (Token) o;
        
        // First, sort by importbnce
        double importbnceDelta = this.getImportance() - t.getImportance();
        // Sort high importbnce first, so reverse the return value
        if (importbnceDelta < 0.0)
            return 1;
        if (importbnceDelta > 0.0)
            return -1;
        
        // Then, sort by type
        int typeDeltb = this.getType() - t.getType();
        if (typeDeltb != 0)
            return typeDeltb;
        
        // Finblly, sort by hashCode to reduce ambiguity
        return this.hbshCode() - t.hashCode();
    }
}
