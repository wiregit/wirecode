package com.limegroup.gnutella;

import com.bitzi.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.security.*;
import java.util.Enumeration;

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
public final class UrnCache {
    
    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final File URN_CACHE_FILE = new File("fileurns.cache");

    /**
     * UrnCache instance variable
     */
    private static UrnCache instance = null;

    /**
     * UrnCache container
     */
    private final Map /* URNSetKey -> HashSet */ URN_MAP;

	/**
	 * Constant for an empty, unmodifiable <tt>Set</tt>.  This is necessary
	 * because Collections.EMPTY_SET is not serializable in the collections 
	 * 1.1 implementation.
	 */
	private static final Set EMPTY_SET = 
		Collections.unmodifiableSet(new HashSet());

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
        URN_MAP = createMap();//initCache();
	}
    
    /**
     * Find any URNs remembered from a previous session for the specified
	 * <tt>File</tt> instance.  The returned <tt>Set</tt> is
	 * guaranteed to be non-null, but it may be empty.
	 *
	 * @param file the <tt>File</tt> instance to look up URNs for
	 * @return a new <tt>Set</tt> containing any cached URNs for the
	 *  speficied <tt>File</tt> instance, guaranteed to be non-null and 
	 *  unmodifiable, but possibly empty
     */
    public Set getUrns(File file) {
        // don't trust failed mod times
        if (file.lastModified() == 0L) {
			return EMPTY_SET;
		} 
		URNSetKey key = new URNSetKey(file);

        // one or more "urn:" names for this file 
		Set cachedUrns = (Set)URN_MAP.get(key);
		if(cachedUrns == null) {
			return EMPTY_SET;
		}

		return Collections.unmodifiableSet(cachedUrns);
    }



    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to URN_MAP.
	 *
	 * @param fileDesc the <tt>FileDesc</tt> instance containing URNs to store
     */
    public void addUrns(File file, Set urns) {
		URNSetKey key = new URNSetKey(file);
        URN_MAP.put(key, Collections.unmodifiableSet(urns));
    }
    
    //
    // UrnCache Management
    //
    
    /**
     * Loads values from cache file, if available
     */
    private static Map createMap() {//void initCache() {
		if(!URN_CACHE_FILE.isFile()) {
			return new Hashtable();
		}
		Map urnMap = null;
		try {
            ObjectInputStream ois = 
			    new ObjectInputStream(new FileInputStream(URN_CACHE_FILE));
			urnMap = (Hashtable)ois.readObject();
		} catch(FileNotFoundException e) {
			// this should never happen, given our check above
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		}
        // discard outdated info
        Iterator iter = urnMap.keySet().iterator();
        while (iter.hasNext()) {
			URNSetKey key = (URNSetKey)iter.next();

            // check to see if file still exists unmodified
            File f = new File(key._path);
            if (!f.exists() || f.lastModified() != key._modTime) {
                iter.remove();
            }
        }
		return urnMap;
    }
    
    /**
     * Write cache to disk to save recalc time later
     */
    public void persistCache() {
        try {
            ObjectOutputStream oos = 
			    new ObjectOutputStream(new FileOutputStream(URN_CACHE_FILE));
            oos.writeObject(URN_MAP);
            oos.close();
        } catch (Exception e) {
			e.printStackTrace();
            // no great loss
        }
    }

	/**
	 * Private class for the key for the set of URNs for files.
	 */
	private static class URNSetKey implements Serializable {
		
		/**
		 * Constant for the file modification time.
		 * @serial
		 */
		transient long _modTime;

		/**
		 * Constant for the file path.
		 * @serial
		 */
		transient String _path;

		/**
		 * Constant cached hash code, since this class is used exclusively
		 * as a hash key.
		 * @serial
		 */
		transient int _hashCode;

		/**
		 * Constructs a new <tt>URNSetKey</tt> instance from the specified
		 * <tt>File</tt> instance.
		 *
		 * @param file the <tt>File</tt> instance to use in constructing the
		 *  key
		 */
		URNSetKey(File file) {
			_modTime = file.lastModified();
			_path = file.getAbsolutePath();
			_hashCode = calculateHashCode();
		}

		/**
		 * Helper method to calculate the hash code.
		 *
		 * @return the hash code for this instance
		 */
		int calculateHashCode() {
			int result = 17;
			result = result*37 + (int)(_modTime ^(_modTime >>> 32));
			result = result*37 + _path.hashCode();
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
			return ((_modTime == key._modTime) &&
					(_path.equals(key._path)));
		}

		/**
		 * Overrides Object.hashCode to meet the specification of Object.equals
		 * and to make this class functions properly as a hash key.
		 *
		 * @return the hash code for this instance
		 */
		public int hashCode() {
			return _hashCode;
		}

		/**
		 * Serializes this instance.
		 *
		 * @serialData the modification time followed by the file path
		 */
		private void writeObject(ObjectOutputStream s) 
			throws IOException {
			s.defaultWriteObject();
			s.writeLong(_modTime);
			s.writeObject(_path);
		}

		/**
		 * Deserializes this instance, restoring all invariants.
		 */
		private void readObject(ObjectInputStream s) 
			throws IOException, ClassNotFoundException {
			s.defaultReadObject();
			_modTime = s.readLong();
			_path = (String)s.readObject();
			_hashCode = calculateHashCode();
		}
	}


	/*
	private static void main(String[] args) {
		UrnCache cache = UrnCache.instance();
		File dir = new File("S:\\Gnutella\\installers\\LimeWire210\\winNoVM");
		File[] files = dir.listFiles();
		URN[] urns = new URN[files.length];
		Set[] sets = new Set[files.length];
		for(int i=0; i<files.length; i++) {
			try {
				urns[i] = URNFactory.createSHA1Urn(files[i]);
			} catch(IOException e) {
			}
			sets[i] = new HashSet();
			sets[i].add(urns[i]);
			cache.addUrns(files[i], sets[i]);
		}
		System.out.println("map size before out: "+cache.URN_MAP.size()); 
		cache.persistCache();
		//cache.URN_MAP = null;
		//cache.URN_MAP = UrnCache.createMap();
		cache.URN_MAP.clear();
		cache.URN_MAP.putAll(UrnCache.createMap());
		System.out.println("map size after out:  "+cache.URN_MAP.size()); 
		System.out.println("maps equal: "+cache.URN_MAP.equals(UrnCache.createMap())); 
		for(int i=0; i<files.length; i++) {
			Set set = cache.getUrns(files[i]);
			System.out.println("sets equal: "+set.equals(sets[i])); 
			//urns[i] = URNFactory.createSHA1Urn(files[i]);
			//sets[i] = new HashSet(files[i], urns[i]);
			//cache.addUrns(files[i], sets[i]);
		}		
	}
	*/
	
}





