package com.limegroup.gnutella.altlocs;

import com.limegroup.gnutella.URN;

/**
 * Interface for classes that serve as collection points for alternate sources
 * for files.
 */
public interface AlternateLocationCollector {

	/**
	 * Adds an <tt>AlternateLocation</tt> instance to the collection of 
	 * <tt>AlternateLocation</tt>s.
	 */
	public boolean add(AlternateLocation al);

	/**
	 * Adds the specified collection of <tt>AlternateLocation</tt>s to
	 * this collection.
	 *
	 * @param alc the <tt>AlternateLocationCollection</tt> instance to
	 *  add this alternate location to the collection 
	 */
	public int addAll(AlternateLocationCollection alc);
	
	/**
	 * Removes the specified location from this collection.
	 */
	public boolean remove(AlternateLocation al);

	/**
	 * Returns whether or not this <tt>AlternateLocationCollector</tt> has
	 * any alternate locations.
	 *
	 * @return <tt>true</tt> if this <tt>AlternateLocationCollector</tt>
	 *  has 1 or more alternate locations, <tt>false</tt> otherwise
	 */
	public boolean hasAlternateLocations();
	
	/**
	 * Number of alternate locations this collector is holding
	 *
	 */
	public int getAltLocsSize();

	/**
	 * Accessor for the SHA1 <tt>URN</tt> instance for this collection.
	 *
	 * @return the SHA1 <tt>URN</tt> for this collection
	 */
	public URN getSHA1Urn();
}
