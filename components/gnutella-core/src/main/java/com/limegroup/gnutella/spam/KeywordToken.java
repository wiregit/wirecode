package com.limegroup.gnutella.spam;

/**
 * A token holding a simple keyword
 */
public class KeywordToken extends AbstractToken {
	private static final long serialVersionUID = 3257850995487748662L;

	/**
	 * must be positive
	 * 
	 * This is a heuristic value to prevent an token from becoming bad
	 * after only a small number of bad evaluations.
	 */
	private static final byte INITAL_GOOD = 20;

	/**
	 * must be positive
	 * 
	 * This value determines how dynamic the filter is. A low MAX value will
	 * allow this Token to get a bad rating after occourring only a few times in
	 * spam, a high value will make it very improbable that the filter will
	 * change its mind about a certain token without user-intervention
	 */
	private static final int MAX = 100;

	private final String keyword;

	private byte _good, _bad;
    
	KeywordToken(String keyword) {
        this.keyword = keyword;
		// give every keyword initial credit
		_good = INITAL_GOOD; 
		_bad = 0;
	}

    @Override
    public final int hashCode() {
        return keyword.hashCode();
    }
    
    @Override
    public final boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof KeywordToken))
            return false;
        return keyword.equals(((KeywordToken)o).keyword);
    }
    
	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
		return (float)Math.pow(1.f * _bad / (_good + _bad + 1), 2);
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(Rating rating) {
		switch (rating) {
		case USER_MARKED_GOOD:
			_bad = 0;
			break;
		case USER_MARKED_SPAM:
			_bad = (byte) Math.min(_bad + 10, MAX);
			break;
		case CLEARED:
			_bad = 0;
			_good = INITAL_GOOD;
			break;
		default:
			throw new IllegalArgumentException("unknown type of rating");
		}

		if (_good >= MAX || _bad >= MAX) {
			_good = (byte) (_good * 9 / 10);
			_bad = (byte) (_bad * 9 / 10);
		}
	}

	@Override
    public String toString() {
		return keyword + " " + _good + " " + _bad;
	}
}
