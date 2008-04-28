package com.limegroup.gnutella.spam;



/**
 * A token holding the file size
 * 
 * unlike addresses or keywords, we consider file sizes to be very accurate
 * identifiers of a file, so we will consider a certain file size spam after
 * only a few bad ratings in a row.
 */
public class SizeToken extends AbstractToken {
	private static final long serialVersionUID = 3906652994404955696L;

	/**
	 * after MAX bad evaluations this token's spamrating will be 1
	 */
	private static final int MAX = 10;

	private final long _size;

    /* How suspect this file is, on a scale of [0, MAX] */
	private byte _bad;

	public SizeToken(long size) {
		_bad = 0;
		_size = size;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
		return ((float) _bad) / MAX;
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
			_bad += 3;
            if (_bad > MAX) {
                _bad = MAX;
            }
			break;
		default:
			throw new IllegalArgumentException("unknown type of rating");
		}
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public TokenType getType() {
		return TokenType.SIZE;
	}

    @Override
    public final int hashCode() {
        // Even when we support files larger than
        // 2 GB, the upper 32 bits of the file size
        // hold much much less entropy than the lower
        // 32 bits.
        return (int)(_size);
    }
    
    @Override
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof SizeToken))
            return false;
        
        return hashCode() == o.hashCode();
    }
	/**
	 * overrides method from <tt>Object</tt>
	 */
	@Override
    public String toString() {
		return "" + _size + " " + _bad;
	}
}
