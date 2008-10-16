package com.limegroup.gnutella.spam;

import com.google.inject.Provider;
import com.limegroup.gnutella.filters.IPFilter;
import org.limewire.io.IP;

/**
 * A simple token holding an IP address
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

	private final String address;

    private boolean ratingInitialized;

	private byte _good;

	private byte _bad;
    
    private static volatile transient Provider<IPFilter> ipFilter;

    static void setIpFilter(Provider<IPFilter> filter) {
        ipFilter = filter;
    }
    
	public AddressToken(String address) {
		this.address = address;
        ratingInitialized = false;
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
                
        _good = INITIAL_GOOD;
        int logDistance = ipFilter.get().logMinDistanceTo(new IP(address));
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
	public void rate(Rating rating) {
        if (!ratingInitialized) {
            initializeRating();
        }
		switch (rating) {
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

    @Override
    public final int hashCode() {
        return address.hashCode();
    }
    
    @Override
    public final boolean equals(Object o) {
        if(o == null)
            return false;
        if(!(o instanceof AddressToken))
            return false;
        return address.equals(((AddressToken)o).address);
    }

	@Override
    public String toString() {
		return address + " " + _good + " " + _bad;
	}
}
