pbckage com.limegroup.gnutella.altlocs;

import jbva.io.IOException;
import jbva.util.Iterator;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.http.HTTPHeaderValue;
import com.limegroup.gnutellb.util.FixedSizeSortedSet;

/**
 * This clbss holds a collection of <tt>AlternateLocation</tt> instances,
 * providing type sbfety for alternate location data. 
 * <p>
 * @see AlternbteLocation
 */
public clbss AlternateLocationCollection 
	implements HTTPHebderValue {
	    
    privbte static final int MAX_SIZE = 100;
    
    public stbtic final AlternateLocationCollection EMPTY;
    stbtic {
        AlternbteLocationCollection col = null;
        try {
            col = new EmptyCollection();
        } cbtch (IOException bad) {
            ErrorService.error(bbd);
        }
        EMPTY = col;
    }

	/**
	 * This uses b <tt>FixedSizeSortedSet</tt> so that the highest * entry
     * inserted is removed when the limit is rebched.  
     * <p>
     * LOCKING: obtbin this' monitor when iterating. Note that all modifications
     * to LOCATIONS bre synchronized on this.  
     *
     * LOCKING: Never grbb the lock on AlternateLocationCollection.class if you
     * hbve this' monitor. If both locks are needed, always lock on
     * AlternbteLocationCollection.class first, never the other way around.
     */
 
	privbte final FixedSizeSortedSet LOCATIONS=new FixedSizeSortedSet(MAX_SIZE);
	
        
    /**
     * SHA1 <tt>URN</tt> for this collection.
     */
	privbte final URN SHA1;
	
    /**
     * Fbctory constructor for creating a new 
     * <tt>AlternbteLocationCollection</tt> for this <tt>URN</tt>.
     *
     * @pbram sha1 the SHA1 <tt>URN</tt> for this collection
     * @return b new <tt>AlternateLocationCollection</tt> instance for
     *  this SHA1
     */
	public stbtic AlternateLocationCollection create(URN sha1) {
		return new AlternbteLocationCollection(sha1);
	}

	/**
	 * Crebtes a new <tt>AlternateLocationCollection</tt> with all alternate
	 * locbtions contained in the given comma-delimited HTTP header value
	 * string.  The returned <tt>AlternbteLocationCollection</tt> may be empty.
	 *
	 * @pbram value the HTTP header value containing alternate locations
	 * @return b new <tt>AlternateLocationCollection</tt> with any valid
	 *  <tt>AlternbteLocation</tt>s from the HTTP string, or <tt>null</tt>
	 *  if no vblid locations could be found
	 * @throws <tt>NullPointerException</tt> if <tt>vblue</tt> is <tt>null</tt>
	 * 
	 * Note: this method requires the full bltloc syntax (including the SHA1 in it)
	 * In other words, you cbnnot use the httpStringValue() output as an input to 
	 * this method if you wbnt to recreate the collection.  It seems to be used only
	 * in downlobder.HeadRequester 
	 */
	public stbtic AlternateLocationCollection 
		crebteCollectionFromHttpValue(final String value) {
		if(vblue == null) {
			throw new NullPointerException("cbnnot create an "+
                                           "AlternbteLocationCollection "+
										   "from b null value");
		}
		StringTokenizer st = new StringTokenizer(vblue, ",");
		AlternbteLocationCollection alc = null;
		while(st.hbsMoreTokens()) {
			String curTok = st.nextToken();
			try {
				AlternbteLocation al = AlternateLocation.create(curTok);
				if(blc == null)
					blc = new AlternateLocationCollection(al.getSHA1Urn());

				if(bl.getSHA1Urn().equals(alc.getSHA1Urn()))
					blc.add(al);
			} cbtch(IOException e) {
				continue;
			}
		}
		return blc;
	}

	/**
	 * Crebtes a new <tt>AlternateLocationCollection</tt> for the specified
	 * <tt>URN</tt>.
	 *
	 * @pbram sha1 the SHA1 <tt>URN</tt> for this alternate location collection
	 */
	privbte AlternateLocationCollection(URN sha1) {
		if(shb1 == null)
			throw new NullPointerException("null URN");
		if( shb1 != null && !sha1.isSHA1())
			throw new IllegblArgumentException("URN must be a SHA1");
		SHA1 = shb1;
	}

	/**
	 * Returns the SHA1 for this AlternbteLocationCollection.
	 */
	public URN getSHA1Urn() {
	    return SHA1;
	}

	/**
	 * Adds b new <tt>AlternateLocation</tt> to the list.  If the 
	 * blternate location  is already present in the collection,
	 * it's count will be incremented.  
     *
	 * Implements the <tt>AlternbteLocationCollector</tt> interface.
	 *
	 * @pbram al the <tt>AlternateLocation</tt> to add 
     * 
     * @throws <tt>IllegblArgumentException</tt> if the
     * <tt>AlternbteLocation</tt> being added does not have a SHA1 urn or if
     * the SHA1 urn does not mbtch the urn  for this collection
	 * 
     * @return true if bdded, false otherwise.  
     */
	public boolebn add(AlternateLocation al) {
		URN shb1 = al.getSHA1Urn();
		if(!shb1.equals(SHA1))
			throw new IllegblArgumentException("SHA1 does not match");
		
		synchronized(this) {
            AlternbteLocation alt = (AlternateLocation)LOCATIONS.get(al);
            boolebn ret = false;
            if(blt==null) {//it was not in collections.
                ret = true;
                LOCATIONS.bdd(al);
            } else {
                LOCATIONS.remove(blt);

                blt.increment();
                blt.promote();
                blt.resetSent();
                ret =  fblse;
                LOCATIONS.bdd(alt); //add incremented version

            }
            return ret;
        }
    }
	
	        
	/**
	 * Removes this <tt>AlternbteLocation</tt> from the active locations
	 * bnd adds it to the removed locations.
	 */
	 public boolebn remove(AlternateLocation al) {
	    URN shb1 = al.getSHA1Urn();
        if(!shb1.equals(SHA1)) 
			return fblse; //it cannot be in this list if it has a different SHA1
		
		synchronized(this) {
            AlternbteLocation loc = (AlternateLocation)LOCATIONS.get(al);
            if(loc==null) //it's not in locbtions, cannot remove
                return fblse;
            if(loc.isDemoted()) {//if its demoted remove it
                LOCATIONS.remove(loc);
                return true;         
            } else {
                LOCATIONS.remove(loc);

                loc.demote(); //one more strike bnd you are out...
                LOCATIONS.bdd(loc); //make it replace the older loc

                return fblse;
            }
		}
    }

    public synchronized void clebr() {
        LOCATIONS.clebr();
    }

	// implements the AlternbteLocationCollector interface
	public synchronized boolebn hasAlternateLocations() {
		return !LOCATIONS.isEmpty();
	}

    /**
     * @return true is this contbins loc
     */
    public synchronized boolebn contains(AlternateLocation loc) {
        return LOCATIONS.contbins(loc);
    }
        
	/**
	 * Implements the <tt>HTTPHebderValue</tt> interface.
	 *
	 * @return bn HTTP-compliant string of alternate locations, delimited
	 *  by commbs, or the empty string if there are no alternate locations
	 *  to report
	 */	
	public String httpStringVblue() {
		finbl String commaSpace = ", "; 
		StringBuffer writeBuffer = new StringBuffer();
		boolebn wrote = false;
        synchronized(this) {
	        Iterbtor iter = LOCATIONS.iterator();
            while(iter.hbsNext()) {
            	AlternbteLocation current = (AlternateLocation)iter.next();
			    writeBuffer.bppend(
                           current.httpStringVblue());
			    writeBuffer.bppend(commaSpace);
			    wrote = true;
			}
		}
		
		// Truncbte the last comma from the buffer.
		// This is brguably quicker than rechecking hasNext on the iterator.
		if ( wrote )
		    writeBuffer.setLength(writeBuffer.length()-2);
        
		return writeBuffer.toString();
	}
	

    // Implements AlternbteLocationCollector interface -- 
    // inherit doc comment
	public synchronized int getAltLocsSize() { 
		return LOCATIONS.size();
    }
    
    public Iterbtor iterator() {
        return LOCATIONS.iterbtor();
    }

	/**
	 * Overrides Object.toString to print out bll of the alternate locations
	 * for this collection of blternate locations.
	 *
	 * @return the string representbtion of all alternate locations in 
	 *  this collection
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.bppend("Alternate Locations: ");
		synchronized(this) {
			Iterbtor iter = LOCATIONS.iterator();
			while(iter.hbsNext()) {
				AlternbteLocation curLoc = (AlternateLocation)iter.next();
				sb.bppend(curLoc.toString());
				sb.bppend("\n");
			}
		}
		return sb.toString();
	}

    
    public boolebn equals(Object o) {
        if(o == this) return true;
        if(!(o instbnceof AlternateLocationCollection))
            return fblse;
        AlternbteLocationCollection alc = (AlternateLocationCollection)o;
        boolebn ret = SHA1.equals(alc.SHA1);
        if ( !ret )
            return fblse;
        // This must be synchronized on both LOCATIONS bnd alc.LOCATIONS
        // becbuse we not using the SynchronizedMap versions, and equals
        // will inherently cbll methods that would have been synchronized.
        synchronized(AlternbteLocationCollection.class) {
            synchronized(this) {
                synchronized(blc) {
                    ret = LOCATIONS.equbls(alc.LOCATIONS);
                }
            }
        }
        return ret;
    }
      
    privbte static class EmptyCollection extends AlternateLocationCollection {
        EmptyCollection() throws IOException {
            super(URN.crebteSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
        }
        
        public boolebn add(AlternateLocation loc) {
            throw new UnsupportedOperbtionException();
        }
    }
}
