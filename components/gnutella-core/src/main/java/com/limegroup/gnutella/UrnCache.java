package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ConverterObjectInputStream;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ProcessingQueue;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * This class contains a systemwide URN cache that persists file URNs (hashes)
 * across sessions.
 *
 * Modified by Gordon Mohr (2002/02/19): Added URN storage, calculation, caching
 * Repackaged by Greg Bildson (2002/02/19): Moved to dedicated class.
 *
 * @see URN
 */
public final class UrnCache {
    
    private static final Log LOG = LogFactory.getLog(UrnCache.class);
    
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
    private static final Map /* UrnSetKey -> HashSet */ URN_MAP = createMap();
    
    /**
     * The ProcessingQueue that Files are hashed in.
     */
    private final ProcessingQueue QUEUE = new ProcessingQueue("Hasher");
    
    /**
     * The set of files that are pending hashing to the callbacks that are listening to them.
     */
    private Map /* File -> List (of UrnCallback) */ pendingHashing = new HashMap();
    
    /**
     * Whether or not data is dirty since the last time we saved.
     */
    private boolean dirty = false;

    /**
	 * Returns the <tt>UrnCache</tt> instance.
	 *
	 * @return the <tt>UrnCache</tt> instance
     */
    public static synchronized UrnCache instance() {
		if (instance == null)
			instance = new UrnCache();
        return instance;
    }

    /**
     * Create and initialize urn cache.
     */
    private UrnCache() {
		dirty = removeOldEntries(URN_MAP);
	}

    /**
     * Calculates the given File's URN and caches it.  The callback will
     * be notified of the URNs.  If they're already calculated, the callback
     * will be notified immediately.  Otherwise, it will be notified when hashing
     * completes, fails, or is interrupted.
     */
    public synchronized void calculateAndCacheUrns(File file, UrnCallback callback) {			
    	Set urns = getUrns(file);
        // TODO: If we ever create more URN types (other than SHA1)
        // we cannot just check for size == 0, we must check for
        // size == NUM_URNS_WE_WANT, and calculateUrns should only
        // calculate the URN for the specific hash we still need.
        if(!urns.isEmpty()) {
            callback.urnsCalculated(file, urns);
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug("Adding: " + file + " to be hashed.");
            List list = (List)pendingHashing.get(file);
            if(list == null) {
                list = new ArrayList(1);
                pendingHashing.put(file, list);
            }
            list.add(callback);
            QUEUE.add(new Processor(file));
        }
    }
    
    /**
     * Clears all callbacks that are owned by the given owner.
     */
    public synchronized void clearPendingHashes(Object owner) {
        if(LOG.isDebugEnabled())
            LOG.debug("Clearing all pending hashes owned by: " + owner);
        
        for(Iterator i = pendingHashing.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            File f = (File)next.getKey();
            List callbacks = (List)next.getValue();
            for(int j = callbacks.size() - 1; j >= 0; j--) {
                UrnCallback c = (UrnCallback)callbacks.get(j);
                if(c.isOwner(owner))
                    callbacks.remove(j);
            }            
            // if there's no more callbacks for this file, remove it.
            if(callbacks.isEmpty())
                i.remove();
        }
    }
    
    /**
     * Clears all callbacks for the given file that are owned by the given owner.
     */
    public synchronized void clearPendingHashesFor(File file, Object owner) {
        if(LOG.isDebugEnabled())
            LOG.debug("Clearing all pending hashes for: " + file + ", owned by: " + owner);
        List callbacks = (List)pendingHashing.get(file);
        if(callbacks != null) {
            for(int j = callbacks.size() - 1; j >= 0; j--) {
                UrnCallback c = (UrnCallback)callbacks.get(j);
                if(c.isOwner(owner))
                    callbacks.remove(j);
            }
            if(callbacks.isEmpty())
                pendingHashing.remove(file);
        }
    }   
    
    /**
     * Adds any URNs that can be locally calculated; may take a while to 
	 * complete on large files.  After calculation, the items are added
	 * for future remembering.
	 *
	 * @param file the <tt>File</tt> instance to calculate URNs for
	 * @return the new <tt>Set</tt> of calculated <tt>URN</tt> instances.  If 
     * the calling thread is interrupted while executing this, returns an empty
     * set.
     */
    public Set calculateUrns(File file) throws IOException, InterruptedException {
        Set set = new HashSet(1);
        set.add(URN.createSHA1Urn(file));
        return set;
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
        if (file.lastModified() == 0L)
			return Collections.EMPTY_SET;

		UrnSetKey key = new UrnSetKey(file);

        // one or more "urn:" names for this file 
		Set cachedUrns = (Set)URN_MAP.get(key);
		if(cachedUrns == null)
			return Collections.EMPTY_SET;

		return cachedUrns;
    }
    
    /**
     * Removes any URNs that associated with a specified file.
     */
    public synchronized void removeUrns(File f) {
        UrnSetKey k = new UrnSetKey(f);
        URN_MAP.remove(k);
        dirty = true;
    }

    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to URN_MAP.
	 *
	 * @param file the <tt>File</tt> instance containing URNs to store
     */
    public synchronized void addUrns(File file, Set urns) {
		UrnSetKey key = new UrnSetKey(file);
        URN_MAP.put(key, Collections.unmodifiableSet(urns));
        dirty = true;
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
            ois = new ConverterObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(file)));
			result = (Map)ois.readObject();
	    } catch(Throwable t) {
	        LOG.error("Unable to read UrnCache", t);
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
	private static boolean removeOldEntries(Map map) {
        // discard outdated info
        boolean dirty = false;
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            Object next = iter.next();
            if(next instanceof UrnSetKey) {
                UrnSetKey key = (UrnSetKey)next;
    
                if(key == null) continue;
    
                // check to see if file still exists unmodified
                File f = new File(key._path);
                if (!f.exists() || f.lastModified() != key._modTime) {
                    dirty = true;
                    iter.remove();
                }
            } else {
                dirty = true;
                iter.remove();
            }
        }
        return dirty;
    }
    
    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        if(!dirty)
            return;
        
        //It's not ideal to hold a lock while writing to disk, but I doubt think
        //it's a problem in practice.
        URN_CACHE_FILE.renameTo(URN_CACHE_BACKUP_FILE);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(URN_CACHE_FILE)));
            oos.writeObject(URN_MAP);
            oos.flush();
        } catch (IOException e) {
            ErrorService.error(e);
        } finally {
            IOUtils.close(oos);
        }
        
        dirty = false;
    }
    
    private class Processor implements Runnable {
        private final File file;
        
        Processor(File f) {
            file = f;
        }
        
        public void run() {
            Set urns;
            List callbacks;
            
            synchronized(UrnCache.this) {
                callbacks = (List)pendingHashing.remove(file);
                urns = getUrns(file); // already calculated?
            }
            
            // If there was atleast one callback listening, try and send it out
            // (which may involve calculating it).
            if(callbacks != null && !callbacks.isEmpty()) {
                // If not calculated, calculate OUTSIDE OF LOCK.
                if(urns.isEmpty()) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Hashing file: " + file);
                    try {
                        urns = calculateUrns(file);
                        addUrns(file, urns);
                    } catch(IOException ignored) {
                        LOG.warn("Unable to calculate URNs", ignored);
                    } catch(InterruptedException ignored) {
                        LOG.warn("Unable to calculate URNs", ignored);
                    }
                }
                
                // Note that because we already removed this list from the Map,
                // we don't need to synchronize while iterating over it, because
                // nothing else can modify it now.
                for(int i = 0; i < callbacks.size(); i++)
                    ((UrnCallback)callbacks.get(i)).urnsCalculated(file, urns);
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





