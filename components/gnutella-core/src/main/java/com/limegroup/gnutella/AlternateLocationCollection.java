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
	 * <tt>Map</tt> of <tt>AlternateLocation</tt> instances that map to
	 * <tt>AlternateLocation</tt> instances.  
     * LOCKING: obtain this' monitor
     * INVARIANT: _alternateLocations.get(k)==k
     * TODO: shouldn't this just be s a sorted set?
	 */
	private SortedMap _alternateLocations;

	/**
	 * Creates a new <tt>AlternateLocationCollection</tt> with all alternate
	 * locations contained in the given comma-delimited HTTP header value
	 * string.  The returned <tt>AlternateLocationCollection</tt> may be empty.
	 *
	 * @param value the HTTP header value containing alternate locations
	 * @return a new <tt>AlternateLocationCollection</tt> with any valid
	 *  <tt>AlternateLocation</tt>s from the HTTP string
	 * @throws <tt>NullPointerException</tt> if <tt>value</tt> is <tt>null</tt>
	 */
	public static AlternateLocationCollection 
		createCollectionFromHttpValue(final String value) {
		if(value == null) {
			throw new NullPointerException("cannot create an AlternateLocationCollection "+
										   "from a null value");
		}
		StringTokenizer st = new StringTokenizer(value, ",");
		AlternateLocationCollection alc = new AlternateLocationCollection();
		while(st.hasMoreTokens()) {
			String curTok = st.nextToken();
			try {
				AlternateLocation al = AlternateLocation.createAlternateLocation(curTok);
				alc.addAlternateLocation(al);
			} catch(IOException e) {
				continue;
			}
		}
		return alc;
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
	 */
	public synchronized void addAlternateLocation(AlternateLocation al) {
		createMap();

		URL url = al.getUrl();
		Set keySet = _alternateLocations.keySet();
		Iterator iter = keySet.iterator();
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
					_alternateLocations.remove(curAl);
					break;
				} else {
					return;
				}
			}
		}
		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_alternateLocations.put(al, al);

		// if the collection of alternate locations is getting too big,
		// remove the last element (the least desirable alternate location)
		// from the Map
		if(_alternateLocations.size() > 100) {
			_alternateLocations.remove(_alternateLocations.lastKey());
		}
	}

	// implements the AlternateLocationCollector interface
	public synchronized void 
		addAlternateLocationCollection(AlternateLocationCollection alc) {
		Map map = alc._alternateLocations;
		if(map == null) return;
		Collection values = map.values();
		Iterator iter = values.iterator();
		while(iter.hasNext()) {
			AlternateLocation curLoc = (AlternateLocation)iter.next();
			this.addAlternateLocation(curLoc);
		}
	}

	// implements the AlternateLocationCollector interface
	public synchronized boolean hasAlternateLocations() {
		if(_alternateLocations == null) return false;
		return !_alternateLocations.isEmpty();
	}

	/**
	 * Return the new Alternate Locations in alc but not in this
	 */
	public synchronized AlternateLocationCollection 
		diffAlternateLocationCollection(AlternateLocationCollection alc) {
		AlternateLocationCollection nalc = new AlternateLocationCollection();
		Iterator iter = alc.values().iterator();
		AlternateLocation value;
		Iterator iter2;
		AlternateLocation value2;
		boolean  matches;
		while (iter.hasNext()) {
			value = (AlternateLocation) iter.next();
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
	public synchronized Collection values() {
		if(_alternateLocations == null) {
			return Collections.EMPTY_LIST;
		}
		List list = new ArrayList(_alternateLocations.values());
		Collections.shuffle(list);
		return list;
	}

	/**
	 * Constructs a synchronized map instance for the alternate locations
	 * if it's not already created.
	 */
	private synchronized void createMap() {
		if(_alternateLocations == null) {
			// we use a TreeMap to both filter duplicates and provide
			// ordering based on the timestamp
			_alternateLocations = 
			    Collections.synchronizedSortedMap(new TreeMap());
		}
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
	public synchronized String httpStringValue() {
		// if there are no alternate locations, simply return the empty
		// string
		if(_alternateLocations == null) return "";

        // TODO: Could this be a performance issue??
		List list = new LinkedList(_alternateLocations.values());
		Collections.shuffle(list);
        list = list.subList(0, list.size() >= 10 ? 10 : list.size());
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

	/**
	 * Returns the number of alternate locations stored.
	 *
	 * @return the number of alternate locations stored
	 */
	public synchronized int size() {
		if(_alternateLocations == null) {
			return 0;
		}
		return _alternateLocations.size();
	}
	
	/**
	 *  Implements AlternateLocationCollector interface
	 */
	public int numberOfAlternateLocations() { return size(); }

	/**
	 * Overrides Object.toString to print out all of the alternate locations
	 * for this collection of alternate locations.
	 *
	 * @return the string representation of all alternate locations in 
	 *  this collection
	 */
	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Alternate Locations: ");
		if(_alternateLocations == null) {
			sb.append("empty");
		}
		else {
			synchronized(_alternateLocations) {
				Iterator iter = _alternateLocations.values().iterator();
				while(iter.hasNext()) {
					AlternateLocation curLoc = (AlternateLocation)iter.next();
					sb.append(curLoc.toString());
					sb.append(" ");
				}
			}
		}
		return sb.toString();
	}
}
