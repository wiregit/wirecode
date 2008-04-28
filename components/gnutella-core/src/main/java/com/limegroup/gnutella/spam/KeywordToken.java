package com.limegroup.gnutella.spam;

import java.util.Arrays;

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

	private final byte[] _keyword;

	private byte _good;

	private byte _bad;
    
    private final int _hashCode;

	KeywordToken(byte[] keyword) {
		// give every keyword initial credit
		_good = INITAL_GOOD; 
		_bad = 0;
		_keyword = keyword;
		int h = 0;
		for (int i = 0; i < _keyword.length; i++) 
		    h = 31*h + _keyword[i];

        _hashCode = h;
	}

    @Override
    public final int hashCode() {
        return _hashCode;
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (! (o instanceof KeywordToken))
            return false;
        
        if (_hashCode != o.hashCode()) {
            return false;
        }
        
        return Arrays.equals(_keyword, ((KeywordToken)o)._keyword);
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
		_age = 0;
		switch (rating) {
		case PROGRAM_MARKED_GOOD:
			_good++;
			break;
		case PROGRAM_MARKED_SPAM:
			_bad++;
			break;
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

	/**
	 * implements interface <tt>Token</tt>
	 */
	public TokenType getType() {
		return TokenType.KEYWORD;
	}

	/**
	 * overrides method from <tt>Object</tt>
	 */
	@Override
    public String toString() {
		return new String(_keyword) + " " + _good + " " + _bad;
	}
}
