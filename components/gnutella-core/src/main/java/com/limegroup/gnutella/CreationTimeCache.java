package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.ErrorService;

/**
 * This class contains a systemwide File creation time cache that persists these
 * times across sessions.  Very similar to UrnCache but less complex.
 *
 * This class is needed because we don't want to consult
 * File.lastModifiedTime() all the time.  We want to preserve creation times
 * across the Gnutella network.
 *
 * In order to be speedy, this class maintains two data structures - one for
 * fast URN to creation time lookup, another for fast 'youngest' file lookup.
 */
public final class CreationTimeCache {
    
    /**
     * File where creation times for files are stored.
     */
    private static final File CTIME_CACHE_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "createtimes.cache");

    /**
     * CreationTimeCache instance variable.  
     * LOCKING: obtain CreationTimeCache.class.
     */
    private static CreationTimeCache instance;

    /** Handy access to FileManager.
     */
    private static FileManager fileManager;

    /**
     * CreationTimeCache container.  LOCKING: obtain this.  Although 
     * CREATE_TIME_MAP is static, CreationTimeCache is a singleton, so
     * obtaining CreationTimeCache's monitor is sufficient -- and slightly
     * more convenient.
     */
    private static final Map /* URN -> Creation Time (Long) */ TIME_MAP;

    /**
     * Alternate container.  LOCKING: obtain this.
     * Creation Time (Long) -> Set of URNs
     */
    private static final SortedMap URN_MAP;

    static {
        TIME_MAP = createMap();
        // use a custom comparator to sort the map in descending order....
        URN_MAP = new TreeMap(new MyComparator());
        fileManager = RouterService.getFileManager();
        instance = new CreationTimeCache();
    }

    /**
	 * Returns the <tt>CreationTimeCache</tt> instance.
	 *
	 * @return the <tt>CreationTimeCache</tt> instance
     */
    public static synchronized CreationTimeCache instance() {
        return instance;
    }

    /**
     * Create and initialize urn cache.
     */
    private CreationTimeCache() {
		removeOldEntries(TIME_MAP);
        constructURNMap();
	}
    
    /**
     * Get the Creation Time of the file.
	 * @param urn <tt>URN<tt> to look up Creation Time for
	 * @return A Long that represents the creation time of the urn.  Null if
     * there is no association.
     */
    public synchronized Long getCreationTime(URN urn) {
		return (Long) TIME_MAP.get(urn);
    }
    
    /**
     * Removes the CreationTime that is associated with the specified URN.
     */
    public synchronized void removeTime(URN urn) {
        TIME_MAP.remove(urn);
        // don't worry about URN_MAP, it'll be pruned next time on startup
    }

    /**
     * Add a CreationTime for the specified <tt>URN</tt> instance.
	 *
	 * @param urn the <tt>URN</tt> instance containing Time to store
     * @param time The creation time of the urn.
     * @throws IllegalArgumentException If urn is null or time is invalid.
     */
    public synchronized void addTime(URN urn, long time) 
        throws IllegalArgumentException {
        if (urn == null) throw new IllegalArgumentException("Null URN.");
        if (time <= 0) throw new IllegalArgumentException("Bad Time.");
        Long cTime = new Long(time);

        // populate urn to time
        TIME_MAP.put(urn, cTime);

        // populate time to set of urns
        Set urnSet = (Set) URN_MAP.get(cTime);
        if (urnSet == null) {
            urnSet = new HashSet();
            URN_MAP.put(cTime, urnSet);
        }
        urnSet.add(urn);
    }


    /**
     * Returns an iterator of URNs, from 'youngest' to 'oldest'.
     * @return the iterator returns younger URNs first.
     */
    public synchronized Iterator getFiles() {
        List urnList = new ArrayList();
        Iterator iter = URN_MAP.entrySet().iterator();

        // we bank on the fact that the URN_MAP iterator returns the entries
        // in descending order....
        while (iter.hasNext()) {
            Map.Entry currEntry = (Map.Entry) iter.next();
            Set urns = (Set) currEntry.getValue();
            urnList.addAll(urns);
        }

        return urnList.listIterator();
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
			URN key = (URN) iter.next();
            if(key == null) continue;

            // check to see if file still exists
            FileDesc fd = fileManager.getFileDescForUrn(key);
            if ((fd == null) || (fd.getFile() == null) || 
                !fd.getFile().exists())
                iter.remove();
        }
    }

    /**
     * Constructs the URN_MAP, which is based off the entries in the TIME_MAP.
     */
    private static void constructURNMap() {
        Set entries = TIME_MAP.entrySet();
        Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            // for each entry, get the creation time and the urn....
            Map.Entry currEntry = (Map.Entry) iter.next();
            Long cTime = (Long) currEntry.getValue();
            URN urn = (URN) currEntry.getKey();

            // put the urn in a set of urns that have that creation time....
            Set urnSet = (Set) URN_MAP.get(cTime);
            if (urnSet == null)
                urnSet = new HashSet();
            urnSet.add(urn);

            // and populate the reverse mapping
            URN_MAP.put(cTime, urnSet);
        }
    }


    /**
     * Loads values from cache file, if available
     */
    private static Map createMap() {
        ObjectInputStream ois = null;
		try {
            ois = new ObjectInputStream(new FileInputStream(CTIME_CACHE_FILE));
			return (Map)ois.readObject();
		} catch(FileNotFoundException e) {
			return new HashMap();
		} catch(IOException e) {
            return new HashMap();
        } catch(ClassNotFoundException e) {
            return new HashMap();
        } catch(ClassCastException e) {
            return new HashMap();
        } catch(IndexOutOfBoundsException ioobe) {
            return new HashMap();
        } catch(SecurityException se) {
            return new HashMap();
        } finally {
            if(ois != null) {
                try {
                    ois.close();
                } catch(IOException e) {
                    // all we can do is try to close it
                }
            }
        }
	}

    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        //It's not ideal to hold a lock while writing to disk, but I doubt think
        //it's a problem in practice.
        try {
            ObjectOutputStream oos = 
			    new ObjectOutputStream(new FileOutputStream(CTIME_CACHE_FILE));
            oos.writeObject(TIME_MAP);
            oos.close();
        } catch (Exception e) {
            ErrorService.error(e);
        }
    }

    
    private static final class MyComparator implements Comparator {

        // switches the usual meaning of compare....        
        public int compare(Object o1, Object o2) {
            if ((o1 instanceof Long) && (o2 instanceof Long))
                return 0 - ((Long)o1).compareTo(((Long)o2));
            ErrorService.error(new Exception("Should only compare longs!!" +
                                             "  o1.class = " + o1.getClass() +
                                             ", o2.class = " + o2.getClass()));
            return 0;
        }

        public boolean equals(Object o) {
            return (o instanceof MyComparator);
        }

    }
}
