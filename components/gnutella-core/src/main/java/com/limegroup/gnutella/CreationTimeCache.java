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
 * In order to be speedy, this class maintains two data structures - one for
 * fast URN to creation time lookup, another for fast 'youngest' file lookup.
 * <br>
 * IMPLEMENTATION NOTES:
 * The two data structures do not reflect each other's internal representation
 * - specifically, the URN->Time lookup may have more URNs than the
 * Time->URNSet lookup.  This is a consequence of partial file sharing.  It is
 * the case that the URNs in the sets of the Time->URNSet lookup are a subset
 * of the URNs in the URN->Time lookup.  For more details, see addTime and
 * commitTime.
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
     * CreationTimeCache container.  LOCKING: obtain this.
     * URN -> Creation Time (Long)
     */
    private final Map URN_TO_TIME_MAP;

    /**
     * Alternate container.  LOCKING: obtain this.
     * Creation Time (Long) -> Set of URNs
     */
    private final SortedMap TIME_TO_URNSET_MAP;

    static {
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
     * You should never really call this - use instance - not private for
     * testing.
     */
    private CreationTimeCache() {
        URN_TO_TIME_MAP = createMap();
        // use a custom comparator to sort the map in descending order....
        TIME_TO_URNSET_MAP = new TreeMap(Comparators.inverseLongComparator());
        pruneTimes(false);
        constructURNMap();
	}
    
    /**
     * Get the Creation Time of the file.
	 * @param urn <tt>URN<tt> to look up Creation Time for
	 * @return A Long that represents the creation time of the urn.  Null if
     * there is no association.
     */
    public synchronized Long getCreationTime(URN urn) {
		return (Long) URN_TO_TIME_MAP.get(urn);
    }
    
    /**
     * Removes the CreationTime that is associated with the specified URN.
     */
    public synchronized void removeTime(URN urn) {
        removeURNFromURNSet(urn, (Long) URN_TO_TIME_MAP.remove(urn));
    }


    /**
     * Clears away any URNs for files that do not exist anymore.
     * @param shouldClearURNSetMap true if you want to clear TIME_TO_URNSET_MAP
     * too
     */
    private synchronized void pruneTimes(boolean shouldClearURNSetMap) {
        Iterator iter = URN_TO_TIME_MAP.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry currEntry = (Map.Entry) iter.next();
            URN currURN = (URN) currEntry.getKey();
            Long cTime = (Long) currEntry.getValue();
            
            // check to see if file still exists
            // NOTE: technically a URN can map to multiple FDs, but I only want
            // to know about one.  getFileDescForUrn prefers FDs over iFDs.
            FileDesc fd = fileManager.getFileDescForUrn(currURN);
            if ((fd == null) || (fd.getFile() == null) || 
                !fd.getFile().exists()) {
                iter.remove();
                if (shouldClearURNSetMap) removeURNFromURNSet(currURN, cTime);
            }
        }
    }

    
    /**
     * Clears away any URNs for files that do not exist anymore.
     */
    public synchronized void pruneTimes() {
        pruneTimes(true);
    }


    /**
     * Add a CreationTime for the specified <tt>URN</tt> instance.  Can be 
     * called for any type of file (complete or partial).  Partial files
     * should be committed upon completion via commitTime.
	 *
	 * @param urn the <tt>URN</tt> instance containing Time to store
     * @param time The creation time of the urn.
     * @throws IllegalArgumentException If urn is null or time is invalid.
     */
    public synchronized void addTime(URN urn, long time) 
        throws IllegalArgumentException {
        if (urn == null) throw new IllegalArgumentException("Null URN.");
        if (time <= 0) throw new IllegalArgumentException("Bad Time = " +
                                                          time);
        Long cTime = new Long(time);

        // populate urn to time
        URN_TO_TIME_MAP.put(urn, cTime);
    }

    /**
     * Commits the CreationTime for the specified <tt>URN</tt> instance.  Should
     * be called for complete files that are shared.  addTime() for the input
     * URN should have been called first (otherwise you'll get a
     * IllegalArgumentException)
	 *
	 * @param urn the <tt>URN</tt> instance containing Time to store
     * @throws IllegalArgumentException If urn is null or the urn was never
     * added in addTime();
     */
    public synchronized void commitTime(URN urn) 
        throws IllegalArgumentException {
        if (urn == null) throw new IllegalArgumentException("Null URN.");
        Long cTime = (Long) URN_TO_TIME_MAP.get(urn);
        if  (cTime == null) 
            throw new IllegalArgumentException("Never added URN via addTime()");

        // populate time to set of urns
        Set urnSet = (Set) TIME_TO_URNSET_MAP.get(cTime);
        if (urnSet == null) {
            urnSet = new HashSet();
            TIME_TO_URNSET_MAP.put(cTime, urnSet);
        }
        urnSet.add(urn);
    }

    

    /**
     * Returns an iterator of URNs, from 'youngest' to 'oldest'.
     * @param max the maximum number of URNs you want returned.  if you
     * want all, give Integer.MAX_VALUE.
     * @return a List ordered by younger URNs.
     */
    public synchronized List getFiles(final int max) 
        throws IllegalArgumentException {
        if (max < 1) throw new IllegalArgumentException("bad max = " + max);
        List urnList = new ArrayList();
        Iterator iter = TIME_TO_URNSET_MAP.entrySet().iterator();

        // we bank on the fact that the TIME_TO_URNSET_MAP iterator returns the 
        // entries in descending order....
        while (iter.hasNext() && (urnList.size() < max)) {
            Map.Entry currEntry = (Map.Entry) iter.next();
            Set urns = (Set) currEntry.getValue();

            // only put as many as desired
            Iterator innerIter = urns.iterator();
            while ((urnList.size() < max) && innerIter.hasNext())
                urnList.add(innerIter.next());
        }

        return urnList;
    }


    /** Returns all of the files URNs, from youngest to oldest.
     */
    public synchronized List getFiles() {
        return getFiles(Integer.MAX_VALUE);
    }
    
        
    /**
     * Write cache so that we only have to calculate them once.
     */
    public synchronized void persistCache() {
        //It's not ideal to hold a lock while writing to disk, but I doubt think
        //it's a problem in practice.
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(CTIME_CACHE_FILE));
            oos.writeObject(URN_TO_TIME_MAP);
        }
        catch (FileNotFoundException fnfe) {
            Assert.that(false, "Couldn't Write find cache file!!");
        }
        catch (IOException e) {
            Assert.that(false, "Couldn't Write to Disk!!");
        }
        finally {
            try {
                if (oos != null)
                    oos.close();
            }
            catch (IOException ignored) {}
        }
    }

    /** Evicts the urn from the TIME_TO_URNSET_MAP.
     *  @param if refTime is non-null, will try to eject from set referred to
     *  by refTime.  otherwise will do an iterative search.
     */
    private synchronized void removeURNFromURNSet(URN urn, Long refTime) {
        if (refTime != null) {
            Set urnSet = (Set) TIME_TO_URNSET_MAP.get(refTime);
            if ((urnSet != null) && (urnSet.remove(urn)))
                if (urnSet.size() < 1) TIME_TO_URNSET_MAP.remove(refTime);
        }
        else { // search everything
            Iterator iter = TIME_TO_URNSET_MAP.entrySet().iterator();
            // find the urn in the map:
            // 1) get rid of it
            // 2) get rid of the empty set if it exists
            while (iter.hasNext()) {
                Map.Entry currEntry = (Map.Entry) iter.next();
                Set urnSet = (Set) currEntry.getValue();
                if (urnSet.contains(urn)) {
                    urnSet.remove(urn); // 1)
                    if (urnSet.size() < 1) iter.remove(); // 2)
                    break;
                }
            }
        }
    }
    

    /**
     * Constructs the TIME_TO_URNSET_MAP, which is based off the entries in the
     * URN_TO_TIME_MAP.
     * IMPORTANT NOTE: currently this method is not synchronized, and does not
     * need to be since it is only called from the constructor (which auto-
     * magically disallows concurrent acess to the instance.  If this method
     * is ever made public, called from multiple entrypoints, etc.,
     * synchronization may be needed.
     */
    private void constructURNMap() {
        Set entries = URN_TO_TIME_MAP.entrySet();
        Iterator iter = entries.iterator();
        while (iter.hasNext()) {
            // for each entry, get the creation time and the urn....
            Map.Entry currEntry = (Map.Entry) iter.next();
            Long cTime = (Long) currEntry.getValue();
            URN urn = (URN) currEntry.getKey();

            // don't ever add IFDs
            if (fileManager.getFileDescForUrn(urn) instanceof 
                IncompleteFileDesc) continue;

            // put the urn in a set of urns that have that creation time....
            Set urnSet = (Set) TIME_TO_URNSET_MAP.get(cTime);
            if (urnSet == null) {
                urnSet = new HashSet();
                // populate the reverse mapping
                TIME_TO_URNSET_MAP.put(cTime, urnSet);
            }
            urnSet.add(urn);

        }
    }


    /**
     * Loads values from cache file, if available
     */
    private Map createMap() {
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
}
