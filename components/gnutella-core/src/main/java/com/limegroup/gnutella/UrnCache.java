package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.DataUtils;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.Set;

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
     * File where urns (currently SHA1 urns) for files are stored.
     */
    private static final File URN_CACHE_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "fileurns.cache");

    /**
     * Last good version of above.
     */
    private static final File URN_CACHE_BACKUP_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "fileurns.bak");

    /**
     * UrnCache instance variable.  LOCKING: obtain UrnCache.class.
     */
    private static UrnCache instance = null;

    /**
     * UrnCache container.  LOCKING: obtain this.  Although URN_MAP is static,
     * UrnCache is a singleton, so obtaining UrnCache's monitor is sufficient--
     * and slightly more convenient.
     */
    private static final Map /* UrnSetKey -> HashSet */ URN_MAP =
		createMap();

    /**
	 * Returns the <tt>UrnCache</tt> instance.
	 *
	 * @return the <tt>UrnCache</tt> instance
     */
    public static synchronized UrnCache instance() {
		if (instance == null) {
			instance = new UrnCache();
		}
        return instance;
    }

    /**
     * Create and initialize urn cache.
     */
    private UrnCache() {
		removeOldEntries(URN_MAP);
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
    public synchronized Set getUrns(File file) {
        // don't trust failed mod times
        if (file.lastModified() == 0L) {
			return DataUtils.EMPTY_SET;
		} 
		UrnSetKey key = new UrnSetKey(file);

        // one or more "urn:" names for this file 
		Set cachedUrns = (Set)URN_MAP.get(key);
		if(cachedUrns == null) {
			return DataUtils.EMPTY_SET;
		}

		return Collections.unmodifiableSet(cachedUrns);
    }
    
    /**
     * Removes any URNs that associated with a specified file.
     */
    public synchronized void removeUrns(File f) {
        UrnSetKey k = new UrnSetKey(f);
        URN_MAP.remove(k);
    }

    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to URN_MAP.
	 *
	 * @param fileDesc the <tt>FileDesc</tt> instance containing URNs to store
     */
    public synchronized void addUrns(File file, Set urns) {
		UrnSetKey key = new UrnSetKey(file);
        URN_MAP.put(key, Collections.unmodifiableSet(urns));
    }
        
    /**
     * Loads values from cache file, if available.  If the cache file is
     * not readable, tries the backup.
     */
    private static Map createMap() {
        Map result;
        result = readMap(URN_CACHE_FILE);
        if(result == null)
            result = readMap(URN_CACHE_BACKUP_FILE);
        if(result == null)
            result = new HashMap();
        return result;
    }
    
    /**
     * Loads values from cache file, if available.
     */
    private static Map readMap(File file) {
        Map result;
        ObjectInputStream ois = null;
		try {
            ois = new ObjectInputStream(new FileInputStream(file));
			result = (Map)ois.readObject();
        } catch (IOException e) {
            result = null;
        } catch (ClassCastException e) {
            result = null;
        } catch (ClassNotFoundException e) {
            result = null;
        } catch(ArrayStoreException e) {
            result = null;
        } catch(IndexOutOfBoundsException e) {
            result = null;
        } catch(NegativeArraySizeException e) {
            result = null;
        } catch(IllegalStateException e) {
            result = null;
        } catch(SecurityException e) {
            result = null;
        } finally {
            if(ois != null) {
                try {
                    ois.close();
                } catch(IOException e) {
                    // all we can do is try to close it
                }
            }
        }
        return result;
	}

	/**
	 * Removes any stale entries from the map so that they will automatically
	 * be replaced.
	 *
	 * @param map the <tt>Map</tt> to check
	 */
	private static void removeOldEntries(Map map) {
        // discard outdated info
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if(next instanceof UrnSetKey) {
                UrnSetKey key = (UrnSetKey)next;
    
                if(key == null) continue;
    
                // check to see if file still exists unmodified
                File f = new File(key._path);
                if (!f.exists() || f.lastModified() != key._modTime) {
                    iter.remove();
                }
            } else {
                iter.remove();
            }
        }
    }
    
    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        //It's not ideal to hold a lock while writing to disk, but I doubt think
        //it's a problem in practice.
        URN_CACHE_FILE.renameTo(URN_CACHE_BACKUP_FILE);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(URN_CACHE_FILE));
            oos.writeObject(URN_MAP);
        } catch (Exception e) {
            ErrorService.error(e);
        } finally {
            if(oos != null) {
                try {
                    oos.close();
                } catch(IOException ignored) {}
            }
        }
    }

	/**
	 * Private class for the key for the set of URNs for files.
	 */
	private static class UrnSetKey implements Serializable {
		
		static final long serialVersionUID = -7183232365833531645L;

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
		 * Constructs a new <tt>UrnSetKey</tt> instance from the specified
		 * <tt>File</tt> instance.
		 *
		 * @param file the <tt>File</tt> instance to use in constructing the
		 *  key
		 */
		UrnSetKey(File file) {
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
			if(!(o instanceof UrnSetKey)) return false;
			UrnSetKey key = (UrnSetKey)o;

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
}





