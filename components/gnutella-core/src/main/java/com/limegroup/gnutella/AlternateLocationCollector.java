package com.limegroup.gnutella;

/**
 * Interface for classes that serve as collection points for alternate sources
 * for files.
 */
public interface AlternateLocationCollector {

	/**
	 * Adds an <tt>AlternateLocation</tt> instance to the collection of 
	 * <tt>AlternateLocation</tt>s.
	 */
	public void addAlternateLocation(AlternateLocation al);

	/**
	 * Adds the specified collection of <tt>AlternateLocation</tt>s to
	 * this collection.
	 *
	 * @param alc the <tt>AlternateLocationCollection</tt> instance to
	 *  add this alternate location to the collection 
	 */
	public void addAlternateLocationCollection(AlternateLocationCollection alc);

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
	public int numberOfAlternateLocations();
}
