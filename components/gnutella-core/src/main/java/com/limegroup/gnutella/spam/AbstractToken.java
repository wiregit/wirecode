package com.limegroup.gnutella.spam;

/**
 * An abstract Token class, we are using this for the common age() & getAge() classes 
 */
public abstract class AbstractToken implements Token {
	byte _age;

	AbstractToken() {
		_age = 0;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void age() {
		if (_age < 127)
			_age++;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public int getAge() {
		// store bad ratings longer than good ratings since our filter relies
		// mostly on bad ratings. However do not return 0 no matter how bad the
		// rating is
		return (int) ((0 | _age) * 100 * (0.1 + Math.pow(1 - getRating(), 0.1)));
	}
}
