package com.limegroup.gnutella;

import com.sun.java.util.collections.*;

/**
 * This class holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type safety for alternate location data.  <p>
 *
 * This class is thread-safe.
 */
public final class AlternateLocationCollection 
	implements HTTPHeaderValue, AlternateLocationCollector {

	/**
	 * <tt>Map</tt> of <tt>AlternateLocation</tt> instances that map to
	 * <tt>AlternateLocation</tt> instances.
	 */
	private Map _alternateLocations;

	/**
	 * Adds the specified <tt>AlternateLocation</tt> instance to the list
	 * of alternate locations for this this file.
	 *
	 * @param al the <tt>AlternateLocation</tt> instance to add
	 */
	public void addAlternateLocation(AlternateLocation al) {
		createMap();

		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_alternateLocations.put(al, al);
	}

	/**
	 * Add the specified <tt>AlternateLocationCollection</tt> to this
	 * <tt>AlternateLocationCollection</tt>.
	 *
	 * @param alc the <tt>AlternateLocationCollection</tt> instance 
	 *  containing the collection of alternate locations to add
	 */
	public void addAlternateLocationCollection(AlternateLocationCollection alc) {
		createMap();
		_alternateLocations.putAll(alc.getMap());
	}

	public boolean hasAlternateLocations() {
		if(_alternateLocations == null) return true;
		return _alternateLocations.isEmpty();
	}

	/**
	 * Accessor for the internal <tt>Map</tt> of alternate locations,
	 * accessible only from this class.
	 *
	 * @return the <tt>Map</tt> of <tt>AlternateLocation</tt> instances
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
			    Collections.synchronizedMap(
				    new TreeMap(AlternateLocation.createComparator()));
		}
	}

	/**
	 * Adds the specified <tt>AlternateLocation</tt> instance to the list
	 * of "temporary" alternate locations.  These will not be stored to
	 * the official alternate locations list until a call to 
	 * commitTemporaryAlternateLocations is made.  This is to avoid,
	 * for example, sending back the same alternate location headers
	 * back to an uploader as they sent in with their upload request,
	 * as they clearly already know about those locations.
	 *
	 * @param al the <tt>AlternateLocation</tt> instance to add to the 
	 *  temporary list.
	 */
	/*
	public synchronized void addTemporaryAlternateLocation(AlternateLocation al) {
		if(_temporaryAlternateLocations == null) {
			// we use a TreeMap to both filter duplicates and provide
			// ordering based on the timestamp
			_temporaryAlternateLocations = 
			    new TreeMap(AlternateLocation.createComparator());
		}

		// note that alternate locations without a timestamp are placed
		// at the end of the map because they have the oldest possible
		// date according to the date class, namely:
		// January 1, 1970, 00:00:00 GMT.
		_temporaryAlternateLocations.put(al, al);
	}
	*/

	/**
	 * Moves all temporary alternate locations to the "official" list of
	 * alternate locations for this file that will be reported in
	 * HTTP headers.
	 */
	/*
	public synchronized void commitTemporaryAlternateLocations() {
		// if there are no temporary locations to commit, just return
		if(_temporaryAlternateLocations == null) {
			return;
		}
		if(_alternateLocations == null) {
			_alternateLocations =
			    new TreeMap(AlternateLocation.createComparator());
		}
		_alternateLocations.putAll(_temporaryAlternateLocations);

		// clear out all the temporary locations.  we could set this to
		// null, but there's a good chance that if a file was asigned
		// temporary alternate locations once, then it will be again
		_temporaryAlternateLocations.clear();
	}
	*/

	public String httpStringValue() {
		// if there are no alternate locations, simply return
		if(_alternateLocations == null) return null;

		Iterator iter = _alternateLocations.values().iterator();	   
		StringBuffer writeBuffer = new StringBuffer();
		writeBuffer.append(HTTPConstants.ALTERNATE_LOCATION_HEADER+" ");
		while(iter.hasNext()) {
			writeBuffer.append(((AlternateLocation)iter.next()).toString());
			if(iter.hasNext()) {
				writeBuffer.append(", ");
			}
		}
		return writeBuffer.toString();
	}
}
