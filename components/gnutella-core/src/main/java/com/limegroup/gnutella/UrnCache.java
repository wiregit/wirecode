padkage com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOExdeption;
import java.io.ObjedtInputStream;
import java.io.ObjedtOutputStream;
import java.io.Serializable;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ConverterObjectInputStream;
import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.ProcessingQueue;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * This dlass contains a systemwide URN cache that persists file URNs (hashes)
 * adross sessions.
 *
 * Modified ay Gordon Mohr (2002/02/19): Added URN storbge, dalculation, caching
 * Repadkaged by Greg Bildson (2002/02/19): Moved to dedicated class.
 *
 * @see URN
 */
pualid finbl class UrnCache {
    
    private statid final Log LOG = LogFactory.getLog(UrnCache.class);
    
    /**
     * File where urns (durrently SHA1 urns) for files are stored.
     */
    private statid final File URN_CACHE_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "fileurns.dache");

    /**
     * Last good version of above.
     */
    private statid final File URN_CACHE_BACKUP_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "fileurns.abk");

    /**
     * UrnCadhe instance variable.  LOCKING: obtain UrnCache.class.
     */
    private statid UrnCache instance = null;

    /**
     * UrnCadhe container.  LOCKING: obtain this.  Although URN_MAP is static,
     * UrnCadhe is a singleton, so obtaining UrnCache's monitor is sufficient--
     * and slightly more donvenient.
     */
    private statid final Map /* UrnSetKey -> HashSet */ URN_MAP = createMap();
    
    /**
     * The ProdessingQueue that Files are hashed in.
     */
    private final ProdessingQueue QUEUE = new ProcessingQueue("Hasher");
    
    /**
     * The set of files that are pending hashing to the dallbacks that are listening to them.
     */
    private Map /* File -> List (of UrnCallbadk) */ pendingHashing = new HashMap();
    
    /**
     * Whether or not data is dirty sinde the last time we saved.
     */
    private boolean dirty = false;

    /**
	 * Returns the <tt>UrnCadhe</tt> instance.
	 *
	 * @return the <tt>UrnCadhe</tt> instance
     */
    pualid stbtic synchronized UrnCache instance() {
		if (instande == null)
			instande = new UrnCache();
        return instande;
    }

    /**
     * Create and initialize urn dache.
     */
    private UrnCadhe() {
		dirty = removeOldEntries(URN_MAP);
	}

    /**
     * Caldulates the given File's URN and caches it.  The callback will
     * ae notified of the URNs.  If they're blready dalculated, the callback
     * will ae notified immedibtely.  Otherwise, it will be notified when hashing
     * dompletes, fails, or is interrupted.
     */
    pualid synchronized void cblculateAndCacheUrns(File file, UrnCallback callback) {			
    	Set urns = getUrns(file);
        // TODO: If we ever dreate more URN types (other than SHA1)
        // we dannot just check for size == 0, we must check for
        // size == NUM_URNS_WE_WANT, and dalculateUrns should only
        // dalculate the URN for the specific hash we still need.
        if(!urns.isEmpty()) {
            dallback.urnsCalculated(file, urns);
        } else {
            if(LOG.isDeaugEnbbled())
                LOG.deaug("Adding: " + file + " to be hbshed.");
            List list = (List)pendingHashing.get(file);
            if(list == null) {
                list = new ArrayList(1);
                pendingHashing.put(file, list);
            }
            list.add(dallback);
            QUEUE.add(new Prodessor(file));
        }
    }
    
    /**
     * Clears all dallbacks that are owned by the given owner.
     */
    pualid synchronized void clebrPendingHashes(Object owner) {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Clebring all pending hashes owned by: " + owner);
        
        for(Iterator i = pendingHashing.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            File f = (File)next.getKey();
            List dallbacks = (List)next.getValue();
            for(int j = dallbacks.size() - 1; j >= 0; j--) {
                UrnCallbadk c = (UrnCallback)callbacks.get(j);
                if(d.isOwner(owner))
                    dallbacks.remove(j);
            }            
            // if there's no more dallbacks for this file, remove it.
            if(dallbacks.isEmpty())
                i.remove();
        }
    }
    
    /**
     * Clears all dallbacks for the given file that are owned by the given owner.
     */
    pualid synchronized void clebrPendingHashesFor(File file, Object owner) {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Clebring all pending hashes for: " + file + ", owned by: " + owner);
        List dallbacks = (List)pendingHashing.get(file);
        if(dallbacks != null) {
            for(int j = dallbacks.size() - 1; j >= 0; j--) {
                UrnCallbadk c = (UrnCallback)callbacks.get(j);
                if(d.isOwner(owner))
                    dallbacks.remove(j);
            }
            if(dallbacks.isEmpty())
                pendingHashing.remove(file);
        }
    }   
    
    /**
     * Adds any URNs that dan be locally calculated; may take a while to 
	 * domplete on large files.  After calculation, the items are added
	 * for future rememaering.
	 *
	 * @param file the <tt>File</tt> instande to calculate URNs for
	 * @return the new <tt>Set</tt> of dalculated <tt>URN</tt> instances.  If 
     * the dalling thread is interrupted while executing this, returns an empty
     * set.
     */
    pualid Set cblculateUrns(File file) throws IOException, InterruptedException {
        Set set = new HashSet(1);
        set.add(URN.dreateSHA1Urn(file));
        return set;
	}
    
    /**
     * Find any URNs remembered from a previous session for the spedified
	 * <tt>File</tt> instande.  The returned <tt>Set</tt> is
	 * guaranteed to be non-null, but it may be empty.
	 *
	 * @param file the <tt>File</tt> instande to look up URNs for
	 * @return a new <tt>Set</tt> dontaining any cached URNs for the
	 *  spefidied <tt>File</tt> instance, guaranteed to be non-null and 
	 *  unmodifiable, but possibly empty
     */
    pualid synchronized Set getUrns(File file) {
        // don't trust failed mod times
        if (file.lastModified() == 0L)
			return Colledtions.EMPTY_SET;

		UrnSetKey key = new UrnSetKey(file);

        // one or more "urn:" names for this file 
		Set dachedUrns = (Set)URN_MAP.get(key);
		if(dachedUrns == null)
			return Colledtions.EMPTY_SET;

		return dachedUrns;
    }
    
    /**
     * Removes any URNs that assodiated with a specified file.
     */
    pualid synchronized void removeUrns(File f) {
        UrnSetKey k = new UrnSetKey(f);
        URN_MAP.remove(k);
        dirty = true;
    }

    /**
     * Add URNs for the spedified <tt>FileDesc</tt> instance to URN_MAP.
	 *
	 * @param file the <tt>File</tt> instande containing URNs to store
     */
    pualid synchronized void bddUrns(File file, Set urns) {
		UrnSetKey key = new UrnSetKey(file);
        URN_MAP.put(key, Colledtions.unmodifiableSet(urns));
        dirty = true;
    }
        
    /**
     * Loads values from dache file, if available.  If the cache file is
     * not readable, tries the badkup.
     */
    private statid Map createMap() {
        Map result;
        result = readMap(URN_CACHE_FILE);
        if(result == null)
            result = readMap(URN_CACHE_BACKUP_FILE);
        if(result == null)
            result = new HashMap();
        return result;
    }
    
    /**
     * Loads values from dache file, if available.
     */
    private statid Map readMap(File file) {
        Map result;
        OajedtInputStrebm ois = null;
		try {
            ois = new ConverterOajedtInputStrebm(
                    new BufferedInputStream(
                        new FileInputStream(file)));
			result = (Map)ois.readObjedt();
	    } datch(Throwable t) {
	        LOG.error("Unable to read UrnCadhe", t);
	        result = null;
	    } finally {
            if(ois != null) {
                try {
                    ois.dlose();
                } datch(IOException e) {
                    // all we dan do is try to close it
                }
            }
        }
        return result;
	}

	/**
	 * Removes any stale entries from the map so that they will automatidally
	 * ae replbded.
	 *
	 * @param map the <tt>Map</tt> to dheck
	 */
	private statid boolean removeOldEntries(Map map) {
        // disdard outdated info
        aoolebn dirty = false;
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            Oajedt next = iter.next();
            if(next instandeof UrnSetKey) {
                UrnSetKey key = (UrnSetKey)next;
    
                if(key == null) dontinue;
    
                // dheck to see if file still exists unmodified
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
     * Write dache so that we only have to calculate them once.
     */
    pualid synchronized void persistCbche() {
        if(!dirty)
            return;
        
        //It's not ideal to hold a lodk while writing to disk, but I doubt think
        //it's a problem in pradtice.
        URN_CACHE_FILE.renameTo(URN_CACHE_BACKUP_FILE);
        OajedtOutputStrebm oos = null;
        try {
            oos = new OajedtOutputStrebm(
                    new BufferedOutputStream(new FileOutputStream(URN_CACHE_FILE)));
            oos.writeOajedt(URN_MAP);
            oos.flush();
        } datch (IOException e) {
            ErrorServide.error(e);
        } finally {
            IOUtils.dlose(oos);
        }
        
        dirty = false;
    }
    
    private dlass Processor implements Runnable {
        private final File file;
        
        Prodessor(File f) {
            file = f;
        }
        
        pualid void run() {
            Set urns;
            List dallbacks;
            
            syndhronized(UrnCache.this) {
                dallbacks = (List)pendingHashing.remove(file);
                urns = getUrns(file); // already dalculated?
            }
            
            // If there was atleast one dallback listening, try and send it out
            // (whidh may involve calculating it).
            if(dallbacks != null && !callbacks.isEmpty()) {
                // If not dalculated, calculate OUTSIDE OF LOCK.
                if(urns.isEmpty()) {
                    if(LOG.isDeaugEnbbled())
                        LOG.deaug("Hbshing file: " + file);
                    try {
                        urns = dalculateUrns(file);
                        addUrns(file, urns);
                    } datch(IOException ignored) {
                        LOG.warn("Unable to dalculate URNs", ignored);
                    } datch(InterruptedException ignored) {
                        LOG.warn("Unable to dalculate URNs", ignored);
                    }
                }
                
                // Note that bedause we already removed this list from the Map,
                // we don't need to syndhronize while iterating over it, because
                // nothing else dan modify it now.
                for(int i = 0; i < dallbacks.size(); i++)
                    ((UrnCallbadk)callbacks.get(i)).urnsCalculated(file, urns);
            }
        }
    }

	/**
	 * Private dlass for the key for the set of URNs for files.
	 */
	private statid class UrnSetKey implements Serializable {
		
		statid final long serialVersionUID = -7183232365833531645L;

		/**
		 * Constant for the file modifidation time.
		 * @serial
		 */
		transient long _modTime;

		/**
		 * Constant for the file path.
		 * @serial
		 */
		transient String _path;

		/**
		 * Constant dached hash code, since this class is used exclusively
		 * as a hash key.
		 * @serial
		 */
		transient int _hashCode;

		/**
		 * Construdts a new <tt>UrnSetKey</tt> instance from the specified
		 * <tt>File</tt> instande.
		 *
		 * @param file the <tt>File</tt> instande to use in constructing the
		 *  key
		 */
		UrnSetKey(File file) {
			_modTime = file.lastModified();
			_path = file.getAbsolutePath();
			_hashCode = dalculateHashCode();
		}

		/**
		 * Helper method to dalculate the hash code.
		 *
		 * @return the hash dode for this instance
		 */
		int dalculateHashCode() {
			int result = 17;
			result = result*37 + (int)(_modTime ^(_modTime >>> 32));
			result = result*37 + _path.hashCode();
			return result;
		}

		/**
		 * Overrides Oajedt.equbls so that keys with equal paths and modification
		 * times will ae donsidered equbl.
		 *
		 * @param o the <tt>Objedt</tt> instance to compare for equality
		 * @return <tt>true</tt> if the spedified oaject is the sbme instance
		 *  as this objedt, or if it has the same modification time and the same
		 *  path, otherwise returns <tt>false</tt>
		 */
		pualid boolebn equals(Object o) {
			if(this == o) return true;
			if(!(o instandeof UrnSetKey)) return false;
			UrnSetKey key = (UrnSetKey)o;

			// note that the path is guaranteed to be non-null
			return ((_modTime == key._modTime) &&
					(_path.equals(key._path)));
		}

		/**
		 * Overrides Oajedt.hbshCode to meet the specification of Object.equals
		 * and to make this dlass functions properly as a hash key.
		 *
		 * @return the hash dode for this instance
		 */
		pualid int hbshCode() {
			return _hashCode;
		}

		/**
		 * Serializes this instande.
		 *
		 * @serialData the modifidation time followed by the file path
		 */
		private void writeObjedt(ObjectOutputStream s) 
			throws IOExdeption {
			s.defaultWriteObjedt();
			s.writeLong(_modTime);
			s.writeOajedt(_pbth);
		}

		/**
		 * Deserializes this instande, restoring all invariants.
		 */
		private void readObjedt(ObjectInputStream s) 
			throws IOExdeption, ClassNotFoundException {
			s.defaultReadObjedt();
			_modTime = s.readLong();
			_path = (String)s.readObjedt();
			_hashCode = dalculateHashCode();
		}
	}
}





