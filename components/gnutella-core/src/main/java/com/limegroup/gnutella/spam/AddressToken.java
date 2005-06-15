package com.limegroup.gnutella.spam;

import com.limegroup.gnutella.Assert;

/**
 * This is a simple token holding a 4-byte-ip / port pair
 */
public class AddressToken extends AbstractToken {
	private static final long serialVersionUID = 3257568416670824244L;

	private static final int TYPE = TYPE_ADDRESS;

	/**
	 * must be positive
	 * 
	 * This is a heuristic value to prevent an IP address from becoming bad
	 * after only a small number of bad ratings.
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
	private static final int MAX = 50;

	private final byte[] _address;

	private final short _port;

	private byte _good;

	private byte _bad;

	public AddressToken(byte[] address, int port) {
		Assert.that(address.length == 4);
		// this initial value is an
		_good = INITAL_GOOD;
		_bad = 0;
		_address = address;
		_port = (short) port;
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
		return 1.f * _bad / (_good + _bad + 1);
	}

	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(int rating) {
		_age = 0;
		switch (rating) {
		case RATING_GOOD:
			_good++;
			break;
		case RATING_SPAM:
			_bad++;
			break;
		case RATING_USER_MARKED_GOOD:
			_bad = 0;
			break;
		case RATING_USER_MARKED_SPAM:
			_bad = (byte) Math.min(_bad + 10, MAX);
			break;
		case RATING_CLEARED:
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

		AddressToken other = (AddressToken) tok;

		for (int i = 0; i < _address.length; i++) {
			int dif = this._address[i] - other._address[i];
			if (dif != 0)
				return dif;
		}

		return this._port - other._port;
	}

	/**
	 * overrides method from <tt>Object</tt>
	 */
	public String toString() {
		return "" + (0xFF & _address[0]) + "." + (0xFF & _address[1]) + "."
				+ (0xFF & _address[2]) + "." + (0xFF & _address[3]) + ":"
				+ (0xFFFF & _port) + " " + _bad;
	}
}
