package com.limegroup.gnutella.altlocs;

import com.limegroup.gnutella.http.*; 
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.net.*;
import java.util.StringTokenizer;
import java.io.*;
import com.limegroup.gnutella.*;

/**
 * This class holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type safety for alternate location data. 
 * <p>
 * @see AlternateLocation
 */
public final class AlternateLocationCollection 
	implements HTTPHeaderValue, AlternateLocationCollector {
	    
    private static final int MAX_SIZE = 100;

	/**
	 * <tt>Map</tt> of <tt>AlternateLocation</tt> instances that map to
	 * <tt>AlternateLocation</tt> instances.  
	 * This uses a <tt>FixedSizeForgetfulHashMap</tt> so that the oldest
	 * entry inserted is removed when the limit is reached.
     * LOCKING: obtain this' monitor when iterating -- otherwise 
	 *          it's synchronized on its own
     *
     * There must be a seperate _locations variable to do equals comparisons
     * on.  SynchronizedMap.equals(SynchronizedMap) won't work, because
     * the synchronized map does not extend Fixedsize.., and Fixedsize..
     * uses private variables for the equals comparison.
	 */
    //Sumeet:TODO2: 
    //TODO1: Make sure the synchronization of this class works.
	private final FixedSizeSortedSet LOCATIONS=new FixedSizeSortedSet(MAX_SIZE);
        
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
	public static AlternateLocationCollection create(URN sha1) {
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
				AlternateLocation al = AlternateLocation.create(curTok);
				if(alc == null)
					alc = new AlternateLocationCollection(al.getSHA1Urn());

				if(al.getSHA1Urn().equals(alc.getSHA1Urn()))
					alc.add(al);
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
		if(sha1 == null)
			throw new NullPointerException("null URN");
		if( sha1 != null && !sha1.isSHA1())
			throw new IllegalArgumentException("URN must be a SHA1");
		SHA1 = sha1;
	}

	/**
	 * Returns the SHA1 for this AlternateLocationCollection.
	 */
	public URN getSHA1Urn() {
	    return SHA1;
	}

	/**
	 * Adds a new <tt>AlternateLocation</tt> to the list.  If the 
	 * alternate location  is already present in the collection,
	 * it's count will be incremented.  
     *
	 * Implements the <tt>AlternateLocationCollector</tt> interface.
	 *
	 * @param al the <tt>AlternateLocation</tt> to add 
     * 
     * @throws <tt>IllegalArgumentException</tt> if the
     * <tt>AlternateLocation</tt> being added does not have a SHA1 urn or if
     * the SHA1 urn does not match the urn  for this collection
	 * 
     * @return true if added, false otherwise.  
     */
	public boolean add(AlternateLocation al) {
		URN sha1 = al.getSHA1Urn();
		if(!sha1.equals(SHA1))
			throw new IllegalArgumentException("SHA1 does not match");
		
		synchronized(this) {
            AlternateLocation alt = null;
            if(LOCATIONS.contains(al))
                alt = (AlternateLocation)(LOCATIONS.tailSet(al).first());
            if(alt==null) {//it was not in collections.
                LOCATIONS.add(al);//no need to increment
                return true;
            }
            else {
                alt.increment();
                LOCATIONS.add(alt); //put it back
                return false;
            }
        }
    }
	
	        
	/**
	 * Removes this <tt>AlternateLocation</tt> from the active locations
	 * and adds it to the removed locations.
	 */
	 public boolean remove(AlternateLocation al) {
	    URN sha1 = al.getSHA1Urn();
        if(!sha1.equals(SHA1)) 
			return false; //it cannot be in this list if it has a different SHA1
		
		synchronized(this) {
            AlternateLocation loc = null;
            if(LOCATIONS.contains(al))
                loc = (AlternateLocation)(LOCATIONS.tailSet(al).first());
            if(loc==null) //it's not in locations, cannot remove
                return false;
            if(loc.getCount() == 0) //we did remove it...don't add it back
                return true;            
            else {
                loc.decrement();
                LOCATIONS.add(loc); //put it back
                return false;
            }
		}
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
	public int addAll(AlternateLocationCollection alc) {
        if(alc == null) 
            throw new NullPointerException("ALC is null");

		if(!alc.getSHA1Urn().equals(SHA1)) 
			throw new IllegalArgumentException("SHA1 does not match");

		//Sumeet:TODO1: potential deadlock. Let a and b are 2
		//AlternateLocationCollection, a.addAll(b) and b.addAll(a) are called on
		//two threads at the same time, we could have a deadlock
		int added = 0;
		synchronized(alc) {
			Iterator iter = alc.LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curLoc = (AlternateLocation)iter.next();
				if( add(curLoc) )
				    added++;
			}
		}
		return added;
	}

    public synchronized void clear() {
        LOCATIONS.clear();
    }

	// implements the AlternateLocationCollector interface
	public synchronized boolean hasAlternateLocations() {
		return !LOCATIONS.isEmpty();
	}

    /**
     * @return true is this contains loc
     */
    public synchronized boolean contains(AlternateLocation loc) {
        return LOCATIONS.contains(loc);
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
		final String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolean wrote = false;
        //Sumeet:TODO2: improve randomization of altlocs
        //Sumeet:TODO2: send all for n-alts
        synchronized(this) {
	        Iterator iter = LOCATIONS.iterator();            
            // then write out the next 10.
            for(int i = 0; i < 10 && iter.hasNext(); i++) {
			    writeBuffer.append((
                           (HTTPHeaderValue)iter.next()).httpStringValue());
			    writeBuffer.append(commaSpace);
			    wrote = true;
			}
		}
		
		// Truncate the last comma from the buffer.
		// This is arguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);
		    
		return writeBuffer.toString();
	}
	

    // Implements AlternateLocationCollector interface -- 
    // inherit doc comment
	public int getNumberOfAlternateLocations() { 
		return LOCATIONS.size();
    }
    
    public Iterator iterator() {
        //Sumeet:TODO1: synchronization needs checking
        return LOCATIONS.iterator();
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
		synchronized(this) {
			Iterator iter = LOCATIONS.iterator();
			while(iter.hasNext()) {
				AlternateLocation curLoc = (AlternateLocation)iter.next();
				sb.append(curLoc.toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

    
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof AlternateLocationCollection))
            return false;
        AlternateLocationCollection alc = (AlternateLocationCollection)o;
        boolean ret = SHA1.equals(alc.SHA1);
        if ( !ret )
            return false;
        // This must be synchronized on both LOCATIONS and alc.LOCATIONS
        // because we not using the SynchronizedMap versions, and equals
        // will inherently call methods that would have been synchronized.
        synchronized(AlternateLocationCollection.class) {
                ret = LOCATIONS.equals(alc.LOCATIONS);
        }
        return ret;
    }
}
