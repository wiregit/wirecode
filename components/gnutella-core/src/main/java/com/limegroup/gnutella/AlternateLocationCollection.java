package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/**
 * This class holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type safety for alternate location data.  <p>
 *
 * This class is thread-safe.
 *
 * @see AlternateLocation
 * @see HTTPHeaderValue
 * @see AlternateLocationCollector
 */
public final class AlternateLocationCollection 
	implements HTTPHeaderValue, AlternateLocationCollector {

	/**
	 * <tt>Map</tt> of <tt>AlternateLocation</tt> instances that map to
	 * <tt>AlternateLocation</tt> instances.
	 */
	private SortedMap _alternateLocations;

	// implements the AlternateLocationCollector interface
	public void addAlternateLocation(AlternateLocation al) {
		createMap();

		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_alternateLocations.put(al, al);

		// if the collection of alternate locations is getting too big,
		// remove the last element (the least desirable alternate location)
		// from the Map
		if(_alternateLocations.size() > 10) {
			_alternateLocations.remove(_alternateLocations.lastKey());
		}
	}

	// implements the AlternateLocationCollector interface
	public void addAlternateLocationCollection(AlternateLocationCollection alc) {
		createMap();
		_alternateLocations.putAll(alc.getMap());
	}

	// implements the AlternateLocationCollector interface
	public boolean hasAlternateLocations() {
		if(_alternateLocations == null) return false;
		return !_alternateLocations.isEmpty();
	}

	/**
	 * Accessor for the internal <tt>Map</tt> of alternate locations,
	 * accessible only from this class.
	 *
	 * @return the <tt>Map</tt> of <tt>AlternateLocation</tt> instances,
	 *  which may be <tt>null</tt>
	 */
	private Map getMap() {
		return _alternateLocations;
	}

	/**
	 * Constructs a synchronized map instance for the alternate locations
	 * if it's not already created.
	 */
	private void createMap() {
		if(_alternateLocations == null) {
			// we use a TreeMap to both filter duplicates and provide
			// ordering based on the timestamp
			_alternateLocations = 
			    Collections.synchronizedSortedMap(new TreeMap());
		}
	}

	// implements the HTTP header value interface
	public String httpStringValue() {
		// if there are no alternate locations, simply return
		if(_alternateLocations == null) return null;

		Iterator iter = _alternateLocations.values().iterator();	   
		StringBuffer writeBuffer = new StringBuffer();
		writeBuffer.append(HTTPConstants.ALTERNATE_LOCATION_HEADER+" ");
		while(iter.hasNext()) {
			writeBuffer.append(((HTTPHeaderValue)iter.next()).httpStringValue());
			if(iter.hasNext()) {
				writeBuffer.append(", ");
			}
		}
		return writeBuffer.toString();
	}

	/**
	 * Returns the number of alternate locations stored.
	 *
	 * @return the number of alternate locations stored
	 */
	public int size() {
		return _alternateLocations.size();
	}

	/**
	 * Overrides Object.equals to more accurately compare 
	 * <tt>AlternateLocationCollection</tt> instances based on the data.
	 *
	 * @return <tt>true</tt> if the specified <tt>Object</tt> is an alternate
	 *  location collection that has stored exactly the same alterate 
	 *  locations as this collection
	 */
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof AlternateLocationCollection)) return false;
		AlternateLocationCollection alc = (AlternateLocationCollection)o;
		return _alternateLocations.equals(alc.getMap());
	}

	/**
	 * Overrides Object.hashCode to meet the hashCode specification that
	 * demands that objects that are "equal" according to the equals method
	 * also have the same hash code.
	 *
	 * @return the hash code for this instance
	 */
	public int hashCode() {
		return 37*_alternateLocations.hashCode();
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
		Iterator iter = _alternateLocations.values().iterator();
		while(iter.hasNext()) {
			AlternateLocation curLoc = (AlternateLocation)iter.next();
			sb.append(curLoc.toString());
			sb.append(" ");
		}
		return sb.toString();
	}
}
