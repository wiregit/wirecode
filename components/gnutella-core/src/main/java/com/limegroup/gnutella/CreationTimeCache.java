package com.limegroup.gnutella;

import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.*;

/**
 * This class contains a systemwide File creation time cache that persists these
 * times across sessions.  Very similar to UrnCache but less complex.
 *
 * This class is needed because we don't want to consult
 * File.lastModifiedTime() all the time.  We want to preserve creation times
 * across the Gnutella network.
 *
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

    static {
        TIME_MAP = createMap();
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
        TIME_MAP.put(urn, new Long(time));
    }


    /**
     * Returns an iterator of URNs, from 'youngest' to 'oldest'.
     * @return the iterator returns younger URNs first.
     */
    public synchronized Iterator getFiles() {
        Iterator iter = null;

        // no implementation as of yet, need to change the implementation
        // of this to make it quick....

        return iter;
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
}
