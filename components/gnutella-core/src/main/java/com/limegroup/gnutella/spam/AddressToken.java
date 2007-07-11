package com.limegroup.gnutella.spam;

import java.util.Arrays;

import com.limegroup.gnutella.RouterService;

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
	private static final byte INITIAL_GOOD = 20;

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
    
    private boolean ratingInitialized;

	private byte _good;

	private byte _bad;
    
    private final int _hashCode;

	public AddressToken(byte[] address, int port) {
		assert address.length == 4;
		_address = address;
		_port = (short) port;
        _hashCode = getHashCode();
        ratingInitialized = false;
	}
    
    /* Rating initialization is costly, and many Tokens are created 
     * simply to be replaced by existing tokens in the RatingTable,
     * so initialize rating lazily. */
    private synchronized void initializeRating() {
        if (ratingInitialized) {
            return;
        }
        // this initial value is an
        _good = INITIAL_GOOD;
        int logDistance = RouterService.getIpFilter().logMinDistanceTo(_address);
        
        // Constants 1600 and 3.3 chosen such that:
        // Same /24 subnet as a banned IP results in a rating of 0.07
        // Same /16 subnet as a banned IP results in a rating of 0.01
        int bad = (int) (1600 * Math.pow(1+logDistance, -3.3));
        while (bad > MAX) {
            bad /= 2;
            _good /= 2;
        }
        _bad = (byte) bad;
        
        ratingInitialized = true;
    }
    
    private int getHashCode() {
        int hashCode = TYPE;
        hashCode = (TYPE * hashCode) + _address[0];
        hashCode = (TYPE * hashCode) + _address[1];
        hashCode = (TYPE * hashCode) + _address[2];
        hashCode = (TYPE * hashCode) + _address[3];
        hashCode = (TYPE * hashCode) + _port;
        return hashCode;
    }

	/**
	 * implements interface <tt>Token</tt>
	 */
	public float getRating() {
        if (!ratingInitialized) {
            initializeRating();
        }
		return ((float) _bad )/(_good + _bad + 1);
	}

    /**
     * implements interface <tt>Token</tt>
     */
    public double getImportance() {
        // Throw out AddressTokens first, since they
        // can mostly be regenerated from the list of
        // blocked IP addresses.
        return Double.NEGATIVE_INFINITY;
    }
    
	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(int rating) {
        if (!ratingInitialized) {
            initializeRating();
        }
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
			_good = INITIAL_GOOD;
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

    public final int hashCode() {
        return _hashCode;
    }
    
    public final boolean equals(Object o) {
        if (o == null)
            return false;
        if ( ! (o instanceof AddressToken))
            return false;
        
        if (_hashCode != o.hashCode()) {
            return false;
        }
        
        return _port == ((AddressToken)o)._port 
                && Arrays.equals(_address, ((AddressToken)o)._address);
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
