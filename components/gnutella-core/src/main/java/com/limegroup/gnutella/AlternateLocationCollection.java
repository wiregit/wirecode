package com.limegroup.gnutella;

import com.limegroup.gnutella.http.*; 
import com.sun.java.util.collections.*;
import java.net.*;
import java.util.StringTokenizer;
import java.io.*;

/**
 * This class holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type safety for alternate location data.  <p>
 *
 * This class is thread-safe.
 *
 * @see AlternateLocation
 */
public final class AlternateLocationCollection 
	implements HTTPHeaderValue, AlternateLocationCollector {

	/**
	 * <tt>Set</tt> of <tt>AlternateLocation</tt> instances that map to
	 * <tt>AlternateLocation</tt> instances.  
     * LOCKING: obtain LOCATIONS monitor when iterating -- otherwise 
	 *          it's synchronized on its own
     * INVARIANT: _alternateLocations.get(k)==k
	 */
	private final SortedSet LOCATIONS = 
		Collections.synchronizedSortedSet(new TreeSet());
		
    /**
     * <tt>Set</tt> of <tt>AlternateLocation</tt> instances that we've
     * removed from this collection.  Attempts to add to this collection
     * will first check to see if the location has been previously removed.
     * If so, we will not re-add them.
     */
    private final Set REMOVED =
        Collections.synchronizedSet(new HashSet());

    /**
     * SHA1 <tt>URN</tt> for this collection.
     */
	private final URN SHA1;

    /**
     * Factory constructor for creating a new 
     * <tt>AlternateLocationCollection</tt> for this <tt>URN</tt>.
     *
     * @param sha1 the SHA1 <tt>URN</tt> for this collection
     * @return a new <tt>AlternateLocationCollection</tt> instance for
     *  this SHA1
     */
	public static AlternateLocationCollection createCollection(URN sha1) {
		return new AlternateLocationCollection(sha1);
	}

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> with all alternate
	 * locations contained in the given comma-delimited HTTP header value
	 * string.  The returned <tt>AlternateLocationCollection</tt> may be empty.
	 *
	 * @param value the HTTP header value containing alternate locations
	 * @return a new <tt>AlternateLocationCollection</tt> with any valid
	 *  <tt>AlternateLocation</tt>s from the HTTP string, or <tt>null</tt>
	 *  if no valid locations could be found
	 * @throws <tt>NullPointerException</tt> if <tt>value</tt> is <tt>null</tt>
	 */
	public static AlternateLocationCollection 
		createCollectionFromHttpValue(final String value) {
		if(value == null) {
			throw new NullPointerException("cannot create an "+
                                           "AlternateLocationCollection "+
										   "from a null value");
		}
		StringTokenizer st = new StringTokenizer(value, ",");
		AlternateLocationCollection alc = null;
		while(st.hasMoreTokens()) {
			String curTok = st.nextToken();
			try {
				AlternateLocation al = 
                    AlternateLocation.createAlternateLocation(curTok);
				if(alc == null) {
					alc = new AlternateLocationCollection(al.getSHA1Urn());
				}

				if(al.getSHA1Urn().equals(alc.getSHA1Urn())) {
					alc.addAlternateLocation(al);
				}
			} catch(IOException e) {
				continue;
			}
		}
		return alc;
	}

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> for the specified
	 * <tt>URN</tt>.
	 *
	 * @param sha1 the SHA1 <tt>URN</tt> for this alternate location collection
	 */
	private AlternateLocationCollection(URN sha1) {
		if(sha1 == null) {
			throw new NullPointerException("null URN");
		}
		if( sha1 != null && !sha1.isSHA1()) {
			throw new IllegalArgumentException("URN must be a SHA1");
		}
		SHA1 = sha1;
	}

    // inherit doc comment
	public URN getSHA1Urn() {
		return SHA1;
	}

	/**
	 * Adds a new <tt>AlternateLocation</tt> to the list.  If the 
	 * alternate location  is already present in the collection,
	 * it will not be added.  If an alternate location with the same
	 * url is already in the list, this keeps the one with the more recent
	 * timestamp, and discards the other. <p>
	 *
	 * Implements the <tt>AlternateLocationCollector</tt> interface.
	 *
	 * @param al the <tt>AlternateLocation</tt> to add	 
	 * @throws <tt>IllegalArgumentException</tt> if the <tt>AlternateLocation</tt>
	 *  being added does not have a SHA1 urn or if the SHA1 urn does not match
	 *  the urn for this collection
	 */
	public void addAlternateLocation(AlternateLocation al) {
		URN sha1 = al.getSHA1Urn();
		if(!sha1.equals(SHA1)) {
			throw new IllegalArgumentException("SHA1 does not match");
		}
		
		// do not add this if it was previously removed.
		if ( wasRemoved(al) ) {
		    return;
        }

		URL url = al.getUrl();
		synchronized(LOCATIONS) {
			Iterator iter = LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curAl = (AlternateLocation)iter.next();
				URL curUrl = curAl.getUrl();
				
				// make sure we don't store multiple alternate locations
				// for the same url
				if(curUrl.equals(url)) {
					// this checks the date
					int comp = curAl.compareTo(al);
					if(comp  > 0) {
						// the AlternateLocation argument is newer than the 
						// existing one with the same URL
						LOCATIONS.remove(curAl);
						break;
					} else {
						return;
					}
				}
			}
		}
		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		LOCATIONS.add(al);

		// if the collection of alternate locations is getting too big,
		// remove the last element (the least desirable alternate location)
		// from the Map
		if(LOCATIONS.size() > 100) {
			LOCATIONS.remove(LOCATIONS.last());
		}
	}
	
	/**
	 * Removes this <tt>AlternateLocation</tt> from the list.  This will
	 * iterate through the list to locate an alternate location with the same
	 * URL and remove that.
	 */
	 public boolean removeAlternateLocation(AlternateLocation al) {
	    URN sha1 = al.getSHA1Urn();
        if(!sha1.equals(SHA1)) {
			return false; // it cannot be in this list if it has a different SHA1
		}
		
		synchronized(LOCATIONS) {
			Iterator iter = LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curAl = (AlternateLocation)iter.next();
				if ( curAl.equalsURL(al) ) {
				    LOCATIONS.remove(curAl);
				    REMOVED.add(curAl);
				    return true;
                }
			}
		}
		return false;
    }
    
    /**
     * Determines if this <tt>AlternateLocation</tt> was once removed
     * from this collection.
     */
    public boolean wasRemoved(AlternateLocation al) {
        URN sha1 = al.getSHA1Urn();
        // it could never have been added (or removed) if the sh1 is different
        if(!sha1.equals(SHA1))
            return false;
            
        synchronized(REMOVED) {
            Iterator iter = REMOVED.iterator();
            while(iter.hasNext()) {
                AlternateLocation curAl = (AlternateLocation)iter.next();
                if( curAl.equalsURL(al) )
                    return true;
            }
        }
        return false;
    }
                    

	/**
     * Implements the <tt>AlternateLocationCollector</tt> interface.
     * Adds the specified <tt>AlternateLocationCollection</tt> to this 
     * collection.
     *
     * @param alc the <tt>AlternateLocationCollection</tt> to add
     * @throws <tt>NullPointerException</tt> if <tt>alc</tt> is 
     *  <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the SHA1 of the
     *  collection to add does not match the collection of <tt>this</tt>
     */
	public void 
        addAlternateLocationCollection(AlternateLocationCollection alc) {
        if(alc == null) {
            throw new NullPointerException("ALC is null");
        }
		if(!alc.getSHA1Urn().equals(SHA1)) {
			throw new IllegalArgumentException("SHA1 does not match");
		}
		Set set = alc.LOCATIONS;
		synchronized(set) { // we must synchronize iteration over the map
			Iterator iter = set.iterator();
			while(iter.hasNext()) {
				AlternateLocation curLoc = (AlternateLocation)iter.next();
				this.addAlternateLocation(curLoc);
			}
		}
	}

	// implements the AlternateLocationCollector interface
	public boolean hasAlternateLocations() {
		return !LOCATIONS.isEmpty();
	}

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> of the alternate
	 * locations that are in <tt>alc</tt> but not in <tt>this</tt>.
	 * 
	 * @return a new <tt>AlternateLocationCollection</tt> with the alternate 
	 *  locations in alc but not in this
	 */
	public AlternateLocationCollection 
		diffAlternateLocationCollection(AlternateLocationCollection alc) {
        if(alc==null) {
            throw new NullPointerException("alc is null");
        }
		AlternateLocationCollection nalc = new AlternateLocationCollection(SHA1);

		// don't need to synchronize here because the values method returns
		// a copy -- we're the only one that has it
		Iterator iter = alc.values().iterator();
		AlternateLocation value;
		Iterator iter2;
		AlternateLocation value2;
		boolean  matches;
		while (iter.hasNext()) {
			value = (AlternateLocation)iter.next();
            if ( wasRemoved(value) ) continue;

			// see above for why synchronizing here is unnecessary
            iter2 = values().iterator();
			matches = false;

			// Compare to all of this list for a match
		    while (iter2.hasNext()) {
			    value2 = (AlternateLocation) iter2.next();
				matches = value.equalsURL(value2);
				if (matches) 
				    break;
			}
			if ( !matches ) {
			    nalc.addAlternateLocation(value);
			}
		}
		return nalc;
	}

	/**
	 * Returns a <tt>Collection</tt> of <tt>AlternateLocation</tt>s that has
	 * been randomized to avoid distributed DOS attacks on servants.
	 *
	 * @return a randomized <tt>Collection</tt> of <tt>AlternateLocation</tt>s
	 */
	public Collection values() {
		List list = null;
		synchronized(LOCATIONS) {
			// Note that new ArrayList(List) internally iterates over List
			// so you need to synchronize this call.
		    list = new ArrayList(LOCATIONS);
		}
		Collections.shuffle(list);
		return list;
	}


	/**
	 * Implements the <tt>HTTPHeaderValue</tt> interface.
	 *
	 * This adds randomness to the order in which alternate locations are
	 * reported and only reports 10 locations.
	 *
	 * @return an HTTP-compliant string of alternate locations, delimited
	 *  by commas, or the empty string if there are no alternate locations
	 *  to report
	 */	
	public String httpStringValue() {
        // TODO: Could this be a performance issue??

        List list = null;
        synchronized(LOCATIONS) {
			// Note that new ArrayList(List) internally iterates over List
			// so you need to synchronize this call.
            list = new LinkedList(LOCATIONS);
        }
        list = list.subList(0, list.size() >= 10 ? 10 : list.size());

		// we have our own copy, so we don't need to synchronize
		Iterator iter = list.iterator();
		final String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		while(iter.hasNext()) {
			writeBuffer.append(((HTTPHeaderValue)iter.next()).httpStringValue());
			if(iter.hasNext()) {
				writeBuffer.append(commaSpace);
			}
		}
		return writeBuffer.toString();
	}
	

    // Implements AlternateLocationCollector interface -- 
    // inherit doc comment
	public int numberOfAlternateLocations() { 
		return LOCATIONS.size();
    }

	/**
	 * Overrides Object.toString to print out all of the alternate locations
	 * for this collection of alternate locations.
	 *
	 * @return the string representation of all alternate locations in 
	 *  this collection
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Alternate Locations: ");
		synchronized(LOCATIONS) {
			Iterator iter = LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curLoc = (AlternateLocation)iter.next();
				sb.append(curLoc.toString());
				sb.append(" ");
			}
		}
		return sb.toString();
	}

    
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof AlternateLocationCollection))
            return false;
        AlternateLocationCollection alc = (AlternateLocationCollection)o;
        return SHA1.equals(alc.SHA1) &&
            LOCATIONS.equals(alc.LOCATIONS);
    }
}












