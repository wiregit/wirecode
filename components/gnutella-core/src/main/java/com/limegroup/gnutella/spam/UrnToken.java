package com.limegroup.gnutella.spam;

import com.limegroup.gnutella.URN;

/**
 * similar to the SizeToken but a much more accurate file identifier. If the
 * user marked an Urn as spam once, it should always return a very high spam
 * rating close to 1 afterwards
 */
public class UrnToken extends AbstractToken {
	private static final long serialVersionUID = 3546925779406631222L;

	/**
	 * after MAX bad evaluations this token's spamrating will be 1
	 */
	private static final int MAX = 4;

	private final URN _urn;

	private byte _bad;

	UrnToken(URN urn) {
		_urn = urn;
		_bad = 0;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
		return 1f / MAX * _bad;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(Rating rating) {
		_age = 0;
		switch (rating) {
		case PROGRAM_MARKED_GOOD:
			if (_bad > 0)
				_bad--;
			break;
		case PROGRAM_MARKED_SPAM:
			if (_bad < MAX)
				_bad++;
			break;
		case CLEARED:
		case USER_MARKED_GOOD:
			_bad = 0;
			break;
		case USER_MARKED_SPAM:
			_bad = MAX;
			break;
		default:
			throw new IllegalArgumentException("unknown type of rating");
		}
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public TokenType getType() {
		return TokenType.URN;
	}
    
    @Override
    public final int hashCode() {
        return _urn.hashCode();
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof UrnToken))
            return false;
        
        if (hashCode() != o.hashCode()) {
            return false;
        }
        
        return _urn.equals(((UrnToken)o)._urn);
    }

	/**
	 * overrides method from <tt>Object</tt>
	 */
	@Override
    public String toString() {
		return _urn.toString() + " " + _bad;
	}
}
