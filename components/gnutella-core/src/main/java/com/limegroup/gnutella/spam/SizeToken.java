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

	/**
	 * This value will be or'd to all sizes to allow this filter to work better
	 * for certain kinds of spam, were the spammer varies the file size a bit
	 * 
	 * Whether or not this is really a good idea remains to be seen....
	 */
	private static final byte FUZZ_FACTOR = 0xF;

	private static final int TYPE = TYPE_SIZE;

	private final long _size;

	private byte _bad;

	public SizeToken(long size) {
		_bad = 0;
		_size = size;
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
	public void rate(int rating) {
		_age = 0;
		switch (rating) {
		case RATING_GOOD:
			if (_bad > 0)
				_bad--;
			break;
		case RATING_SPAM:
			if (_bad < MAX)
				_bad++;
			break;
		case RATING_CLEARED:
		case RATING_USER_MARKED_GOOD:
			_bad = 0;
			break;
		case RATING_USER_MARKED_SPAM:
			_bad = (byte) Math.min(_bad + 3, MAX);
			break;
		default:
			throw new IllegalArgumentException("unknown type of rating");
		}
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public int getType() {
		return TYPE;
	}

	/**
	 * implements interface <tt>Comparable</tt>
	 */
	public int compareTo(Object o) {
		Token tok = (Token) o;

		if (TYPE != tok.getType())
			return TYPE - tok.getType();

		SizeToken other = (SizeToken) tok;

		long dif = (this._size - other._size) & ~FUZZ_FACTOR;

		if (dif > 0)
			return 1;
		else if (dif < 0)
			return -1;
		return 0;
	}

	/**
	 * overrides method from <tt>Object</tt>
	 */
	public String toString() {
		return "" + _size + " " + _bad;
	}
}
