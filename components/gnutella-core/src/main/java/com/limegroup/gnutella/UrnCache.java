/**
 * This class contains a systemwide URN cache that persists file URNs (hashes)
 * across sessions.
 *
 * Modified by Gordon Mohr (2002/02/19): Added URN storage, calculation, caching
 * Repackaged by Greg Bildson (2002/02/19): Moved to dedicated class.
 *
 * @see FileDesc
 * @see URN
 */

package com.limegroup.gnutella;

import com.bitzi.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.security.*;
import java.util.Enumeration;

public final class UrnCache {
    
    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";

    /**
     * UrnCache instance variable
     */
    private static UrnCache instance = null;

    /**
     * UrnCache container
     */
    private Map /* URNSetKey -> HashSet */ theUrnCache;

    /**
	 * Returns the <tt>UrnCache</tt> instance.
	 *
	 * @return the <tt>UrnCache</tt> instance
     */
    public static UrnCache instance() {
		if (instance == null) {
			instance = new UrnCache();
		}
        return instance;
    }

    /**
     *  Create and initialize urn cache
     */
    private UrnCache() {
        initCache();
	}
    
    /**
     * Find any URNs remembered from a previous session for the specified
	 * <tt>File</tt> instance.  The returned <tt>Set</tt> is
	 * guaranteed to be non-null, but it may be empty.
	 *
	 * @param file the <tt>File</tt> instance to look up URNs for
	 * @return a new <tt>Set</tt> containing any cached URNs for the
	 *  speficied <tt>File</tt> instance, guaranteed to be non-null, but
	 *  possibly empty
     */
    public Set getUrns(File file) {
        // don't trust failed mod times
        if (file.lastModified() == 0L) {
			return Collections.EMPTY_SET;
		} 
		URNSetKey key = new URNSetKey(file);

        // one or more "urn:" names for this file 
        
		Set cachedUrns = (Set)theUrnCache.get(key);
		if(cachedUrns == null) {
			return Collections.EMPTY_SET;
		}

		return cachedUrns;
    }



    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to theUrnCache.
	 *
	 * @param fileDesc the <tt>FileDesc</tt> instance containing URNs to store
     */
    public void addUrns(File file, Set urns) {
		URNSetKey key = new URNSetKey(file);
        theUrnCache.put(key, urns);
    }
    
    //
    // UrnCache Management
    //
    
    /**
     * load values from cache file, if available
     */
    private void initCache() {
        try {
            ObjectInputStream ois = 
			    new ObjectInputStream(new FileInputStream(URN_CACHE_FILE));
            theUrnCache = (Hashtable)ois.readObject();
            ois.close();
        } catch (Exception e) {
            // lack of cache is non-fatal
        } 
        if (theUrnCache == null) {
            theUrnCache = new Hashtable();
            return;
        }
        // discard outdated info
        Iterator iter = theUrnCache.keySet().iterator();
        while (iter.hasNext()) {
			URNSetKey key = (URNSetKey)iter.next();

            // check to see if file still exists unmodified
            File f = new File(key.PATH);
            if (!f.exists() || f.lastModified() != key.MOD_TIME) {
                iter.remove();
            }
        }
    }
    
    /**
     * write cache to disk to save recalc time later
     */
    public void persistCache() {
        try {
            ObjectOutputStream oos = 
			    new ObjectOutputStream(new FileOutputStream(URN_CACHE_FILE));
            oos.writeObject(theUrnCache);
            oos.close();
        } catch (Exception e) {
            // no great loss
        }
    }

	/**
	 * Private class for the key for the set of URNs for files.
	 */
	private static class URNSetKey {
		
		/**
		 * Constant for the file modification time.
		 */
		private final long MOD_TIME;

		/**
		 * Constant for the file path.
		 */
		private final String PATH;

		/**
		 * Constant cached hash code, since this class is used exclusively
		 * as a hash key.
		 */
		private final int HASH_CODE;

		/**
		 * Constructs a new <tt>URNSetKey</tt> instance from the specified
		 * <tt>File</tt> instance.
		 *
		 * @param file the <tt>File</tt> instance to use in constructing the
		 *  key
		 */
		private URNSetKey(File file) {
			MOD_TIME = file.lastModified();
			PATH = file.getAbsolutePath();
			HASH_CODE = calculateHashCode();
		}

		/**
		 * Helper method to calculate the hash code.
		 *
		 * @return the hash code for this instance
		 */
		private int calculateHashCode() {
			int result = 17;
			result = result*37 + (int)(MOD_TIME ^(MOD_TIME >>> 32));
			result = result*37 + PATH.hashCode();
			return result;
		}

		/**
		 * Overrides Object.equals so that keys with equal paths and modification
		 * times will be considered equal.
		 *
		 * @param o the <tt>Object</tt> instance to compare for equality
		 * @return <tt>true</tt> if the specified object is the same instance
		 *  as this object, or if it has the same modification time and the same
		 *  path, otherwise returns <tt>false</tt>
		 */
		public boolean equals(Object o) {
			if(this == o) return true;
			if(!(o instanceof URNSetKey)) return false;
			URNSetKey key = (URNSetKey)o;

			// note that the path is guaranteed to be non-null
			return ((MOD_TIME == key.MOD_TIME) &&
					(PATH.equals(key.PATH)));
		}

		/**
		 * Overrides Object.hashCode to meet the specification of Object.equals
		 * and to make this class function properly as a hash key.
		 *
		 * @return the hash code for this instance
		 */
		public int hashCode() {
			return HASH_CODE;
		}
	}
}





