package com.limegroup.gnutella.spam;

import java.util.Arrays;

import org.limewire.io.IP;

import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;

/**
 * This is a simple token holding a 4-byte-ip / port pair
 */
public class AddressToken extends AbstractToken {
	private static final long serialVersionUID = 3257568416670824244L;

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

    private volatile transient Provider<IPFilter> ipFilter;

	public AddressToken(byte[] address, int port, Provider<IPFilter> ipFilter) {
		this.ipFilter = ipFilter;
        assert address.length == 4;
		_address = address;
		_port = (short) port;
        _hashCode = getHashCode();
        ratingInitialized = false;
	}
	
	void setIpFilter(Provider<IPFilter> ipFilter) {
	    this.ipFilter = ipFilter;
	}
    
    /* Rating initialization is costly, and many Tokens are created 
     * simply to be replaced by existing tokens in the RatingTable,
     * so initialize rating lazily. */
    private synchronized void initializeRating() {
        if (ratingInitialized) {
            return;
        }
        
        if(ipFilter == null)
            throw new IllegalStateException("must initialize IPFilter after deserializing.");
                
        // this initial value is an
        _good = INITIAL_GOOD;
        int logDistance = ipFilter.get().logMinDistanceTo(new IP(_address));
        
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
        return _address[0] + _address[1] + _address[2] + _address[3] + _port;
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
    @Override
    public double getImportance() {
        // Throw out AddressTokens first, since they
        // can mostly be regenerated from the list of
        // blocked IP addresses.
        return Double.NEGATIVE_INFINITY;
    }
    
	/**
	 * implements interface <tt>Token</tt>
	 */
	public void rate(Rating rating) {
        if (!ratingInitialized) {
            initializeRating();
        }
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
	public TokenType getType() {
		return TokenType.ADDRESS;
	}

    @Override
    public final int hashCode() {
        return _hashCode;
    }
    
    @Override
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
	@Override
    public String toString() {
		return "" + (0xFF & _address[0]) + "." + (0xFF & _address[1]) + "."
				+ (0xFF & _address[2]) + "." + (0xFF & _address[3]) + ":"
				+ (0xFFFF & _port) + " " + _bad;
	}	
}
