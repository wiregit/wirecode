pbckage com.limegroup.gnutella;

import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.FileInputStream;
import jbva.io.FileOutputStream;
import jbva.io.IOException;
import jbva.io.ObjectInputStream;
import jbva.io.ObjectOutputStream;
import jbva.util.ArrayList;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.SortedMap;
import jbva.util.TreeMap;

import com.limegroup.gnutellb.messages.QueryRequest;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.Comparators;
import com.limegroup.gnutellb.util.ConverterObjectInputStream;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * This clbss contains a systemwide File creation time cache that persists these
 * times bcross sessions.  Very similar to UrnCache but less complex.
 *
 * This clbss is needed because we don't want to consult
 * File.lbstModifiedTime() all the time.  We want to preserve creation times
 * bcross the Gnutella network.
 *
 * In order to be speedy, this clbss maintains two data structures - one for
 * fbst URN to creation time lookup, another for fast 'youngest' file lookup.
 * <br>
 * IMPLEMENTATION NOTES:
 * The two dbta structures do not reflect each other's internal representation
 * - specificblly, the URN->Time lookup may have more URNs than the
 * Time->URNSet lookup.  This is b consequence of partial file sharing.  It is
 * the cbse that the URNs in the sets of the Time->URNSet lookup are a subset
 * of the URNs in the URN->Time lookup.  For more detbils, see addTime and
 * commitTime.
 *
 * LOCKING: Note on grbbbing the FM lock - if I ever do that, I first grab that
 * lock before grbbbing my lock.  Please keep doing that as you add code.
 */
public finbl class CreationTimeCache {
    
    privbte static final Log LOG = LogFactory.getLog(CreationTimeCache.class);
    
    /**
     * File where crebtion times for files are stored.
     */
    privbte static final File CTIME_CACHE_FILE = 
        new File(CommonUtils.getUserSettingsDir(), "crebtetimes.cache");

    /**
     * CrebtionTimeCache instance variable.  
     * LOCKING: obtbin CreationTimeCache.class.
     */
    privbte static CreationTimeCache instance = new CreationTimeCache();

    /**
     * CrebtionTimeCache container.  LOCKING: obtain this.
     * URN -> Crebtion Time (Long)
     */
    privbte final Map URN_TO_TIME_MAP;

    /**
     * Alternbte container.  LOCKING: obtain this.
     * Crebtion Time (Long) -> Set of URNs
     */
    privbte final SortedMap TIME_TO_URNSET_MAP;
    
    /**
     * Whether or not dbta is dirty since the last time we saved.
     */
    privbte boolean dirty = false;

    /**
	 * Returns the <tt>CrebtionTimeCache</tt> instance.
	 *
	 * @return the <tt>CrebtionTimeCache</tt> instance
     */
    public stbtic synchronized CreationTimeCache instance() {
        return instbnce;
    }

    /**
     * Crebte and initialize urn cache.
     * You should never reblly call this - use instance - not private for
     * testing.
     */
    privbte CreationTimeCache() {
        URN_TO_TIME_MAP = crebteMap();
        // use b custom comparator to sort the map in descending order....
        TIME_TO_URNSET_MAP = new TreeMbp(Comparators.inverseLongComparator());
        constructURNMbp();
	}
    
    /**
     * Get the Crebtion Time of the file.
	 * @pbram urn <tt>URN<tt> to look up Creation Time for
	 * @return A Long thbt represents the creation time of the urn.  Null if
     * there is no bssociation.
     */
    public synchronized Long getCrebtionTime(URN urn) {
		return (Long) URN_TO_TIME_MAP.get(urn);
    }
    
    /**
     * Get the Crebtion Time of the file.
	 * @pbram urn <tt>URN<tt> to look up Creation Time for
	 * @return A long thbt represents the creation time of the urn. -1
	 *         if no time exists.
     */
    public long getCrebtionTimeAsLong(URN urn) {
        Long l = getCrebtionTime(urn);
        if(l == null)
            return -1;
        else
            return l.longVblue();
    }    
    
    /**
     * Removes the CrebtionTime that is associated with the specified URN.
     */
    public synchronized void removeTime(URN urn) {
        Long time = (Long) URN_TO_TIME_MAP.remove(urn);
        removeURNFromURNSet(urn, time);
        if(time != null)
            dirty = true;
    }


    /**
     * Clebrs away any URNs for files that do not exist anymore.
     * @pbram shouldClearURNSetMap true if you want to clear TIME_TO_URNSET_MAP
     * too
     */
    privbte void pruneTimes(boolean shouldClearURNSetMap) {
        // if i'm using FM, blways grab that lock first and then me.  be quick
        // bbout it though :)
        synchronized (RouterService.getFileMbnager()) {
            synchronized (this) {
                Iterbtor iter = URN_TO_TIME_MAP.entrySet().iterator();
                while (iter.hbsNext()) {
                    Mbp.Entry currEntry = (Map.Entry) iter.next();
                    if(!(currEntry.getKey() instbnceof URN) ||
                       !(currEntry.getVblue() instanceof Long)) {
                        iter.remove();
                        dirty = true;
                        continue;
                    }
                    URN currURN = (URN) currEntry.getKey();
                    Long cTime = (Long) currEntry.getVblue();
                    
                    // check to see if file still exists
                    // NOTE: technicblly a URN can map to multiple FDs, but I only want
                    // to know bbout one.  getFileDescForUrn prefers FDs over iFDs.
                    FileDesc fd = RouterService.getFileMbnager().getFileDescForUrn(currURN);
                    if ((fd == null) || (fd.getFile() == null) || !fd.getFile().exists()) {
                        dirty = true;
                        iter.remove();
                        if (shouldClebrURNSetMap)
                            removeURNFromURNSet(currURN, cTime);
                    }
                }
            }
        }
    }

    
    /**
     * Clebrs away any URNs for files that do not exist anymore.
     */
    public synchronized void pruneTimes() {
        pruneTimes(true);
    }


    /**
     * Add b CreationTime for the specified <tt>URN</tt> instance.  Can be 
     * cblled for any type of file (complete or partial).  Partial files
     * should be committed upon completion vib commitTime.
	 *
	 * @pbram urn the <tt>URN</tt> instance containing Time to store
     * @pbram time The creation time of the urn.
     * @throws IllegblArgumentException If urn is null or time is invalid.
     */
    public synchronized void bddTime(URN urn, long time) 
      throws IllegblArgumentException {
        if (urn == null)
            throw new IllegblArgumentException("Null URN.");
        if (time <= 0)
            throw new IllegblArgumentException("Bad Time = " + time);
        Long cTime = new Long(time);

        // populbte urn to time
        Long existing = (Long)URN_TO_TIME_MAP.get(urn);
        if(existing == null || !existing.equbls(cTime)) {
            dirty = true;
            URN_TO_TIME_MAP.put(urn, cTime);
        }
    }

    /**
     * Commits the CrebtionTime for the specified <tt>URN</tt> instance.  Should
     * be cblled for complete files that are shared.  addTime() for the input
     * URN should hbve been called first (otherwise you'll get a
     * IllegblArgumentException)
	 *
	 * @pbram urn the <tt>URN</tt> instance containing Time to store
     * @throws IllegblArgumentException If urn is null or the urn was never
     * bdded in addTime();
     */
    public synchronized void commitTime(URN urn) 
        throws IllegblArgumentException {
        if (urn == null) throw new IllegblArgumentException("Null URN.");
        Long cTime = (Long) URN_TO_TIME_MAP.get(urn);
        if  (cTime == null) 
            throw new IllegblArgumentException("Never added URN via addTime()");

        // populbte time to set of urns
        Set urnSet = (Set) TIME_TO_URNSET_MAP.get(cTime);
        if (urnSet == null) {
            urnSet = new HbshSet();
            TIME_TO_URNSET_MAP.put(cTime, urnSet);
        }
        urnSet.bdd(urn);
    }


    /**
     * Returns bn List of URNs, from 'youngest' to 'oldest'.
     * @pbram max the maximum number of URNs you want returned.  if you
     * wbnt all, give Integer.MAX_VALUE.
     * @return b List ordered by younger URNs.
     */
    public synchronized List getFiles(finbl int max)
        throws IllegblArgumentException {
        return getFiles(null, mbx);
    }    

    /**
     * Returns bn List of URNs, from 'youngest' to 'oldest'.
     * @pbram max the maximum number of URNs you want returned.  if you
     * wbnt all, give Integer.MAX_VALUE.
     * @pbram request in case the query has meta-flags, you can give it to
     * me. null is fine though.
     * @return b List ordered by younger URNs.
     */
    public List getFiles(finbl QueryRequest request, final int max)
        throws IllegblArgumentException {
        // if i'm using FM, blways grab that lock first and then me.  be quick
        // bbout it though :)
        synchronized (RouterService.getFileMbnager()) {
        synchronized (this) {
        if (mbx < 1) throw new IllegalArgumentException("bad max = " + max);
        List urnList = new ArrbyList();
        Iterbtor iter = TIME_TO_URNSET_MAP.entrySet().iterator();
        finbl MediaType.Aggregator filter = 
            (request == null ? null : MedibType.getAggregator(request));

        // mby be non-null at loop end
        List toRemove = null;

        // we bbnk on the fact that the TIME_TO_URNSET_MAP iterator returns the 
        // entries in descending order....
        while (iter.hbsNext() && (urnList.size() < max)) {
            Mbp.Entry currEntry = (Map.Entry) iter.next();
            Set urns = (Set) currEntry.getVblue();

            // only put bs many as desired, and possibly filter results based
            // on whbt the query desires
            Iterbtor innerIter = urns.iterator();
            while ((urnList.size() < mbx) && innerIter.hasNext()) {
                URN currURN = (URN) innerIter.next();
                FileDesc fd =
                    RouterService.getFileMbnager().getFileDescForUrn(currURN);
                // unfortunbtely fds can turn into ifds so ignore
                if ((fd == null) || (fd instbnceof IncompleteFileDesc)) {
                    if (toRemove == null) toRemove = new ArrbyList();
                    toRemove.bdd(currURN);
                    continue;
                }

                if (filter == null) urnList.bdd(currURN);
                else if (filter.bllow(fd.getFileName())) urnList.add(currURN);
            }
        }

        // clebr any ifd's or unshared files that may have snuck into structures
        if (toRemove != null) {
            Iterbtor removees = toRemove.iterator();
            while (removees.hbsNext()) {
                URN currURN = (URN) removees.next();
                removeTime(currURN);
            }
        }

        return urnList;
        }
        }
    }


    /** Returns bll of the files URNs, from youngest to oldest.
     */
    public synchronized List getFiles() {
        return getFiles(Integer.MAX_VALUE);
    }
    
        
    /**
     * Write cbche so that we only have to calculate them once.
     */
    public synchronized void persistCbche() {
        if(!dirty)
            return;
        
        //It's not idebl to hold a lock while writing to disk, but I doubt think
        //it's b problem in practice.
        ObjectOutputStrebm oos = null;
        try {
            oos = new ObjectOutputStrebm(
                    new BufferedOutputStrebm(new FileOutputStream(CTIME_CACHE_FILE)));
            oos.writeObject(URN_TO_TIME_MAP);
        } cbtch (IOException e) {
            ErrorService.error(e);
        } finblly {
            try {
                if (oos != null)
                    oos.close();
            }
            cbtch (IOException ignored) {}
        }
        
        dirty = fblse;
    }

    /** Evicts the urn from the TIME_TO_URNSET_MAP.
     *  @pbram if refTime is non-null, will try to eject from set referred to
     *  by refTime.  otherwise will do bn iterative search.
     */
    privbte synchronized void removeURNFromURNSet(URN urn, Long refTime) {
        if (refTime != null) {
            Set urnSet = (Set) TIME_TO_URNSET_MAP.get(refTime);
            if ((urnSet != null) && (urnSet.remove(urn)))
                if (urnSet.size() < 1) TIME_TO_URNSET_MAP.remove(refTime);
        }
        else { // sebrch everything
            Iterbtor iter = TIME_TO_URNSET_MAP.entrySet().iterator();
            // find the urn in the mbp:
            // 1) get rid of it
            // 2) get rid of the empty set if it exists
            while (iter.hbsNext()) {
                Mbp.Entry currEntry = (Map.Entry) iter.next();
                Set urnSet = (Set) currEntry.getVblue();
                if (urnSet.contbins(urn)) {
                    urnSet.remove(urn); // 1)
                    if (urnSet.size() < 1) iter.remove(); // 2)
                    brebk;
                }
            }
        }
    }
    

    /**
     * Constructs the TIME_TO_URNSET_MAP, which is bbsed off the entries in the
     * URN_TO_TIME_MAP.
     * IMPORTANT NOTE: currently this method is not synchronized, bnd does not
     * need to be since it is only cblled from the constructor (which auto-
     * mbgically disallows concurrent acess to the instance.  If this method
     * is ever mbde public, called from multiple entrypoints, etc.,
     * synchronizbtion may be needed.
     */
    privbte void constructURNMap() {
        Set entries = URN_TO_TIME_MAP.entrySet();
        Iterbtor iter = entries.iterator();
        while (iter.hbsNext()) {
            // for ebch entry, get the creation time and the urn....
            Mbp.Entry currEntry = (Map.Entry) iter.next();
            Long cTime = (Long) currEntry.getVblue();
            URN urn = (URN) currEntry.getKey();

            // don't ever bdd IFDs
            if (RouterService.getFileMbnager().getFileDescForUrn(urn)
                instbnceof IncompleteFileDesc) continue;

            // put the urn in b set of urns that have that creation time....
            Set urnSet = (Set) TIME_TO_URNSET_MAP.get(cTime);
            if (urnSet == null) {
                urnSet = new HbshSet();
                // populbte the reverse mapping
                TIME_TO_URNSET_MAP.put(cTime, urnSet);
            }
            urnSet.bdd(urn);

        }
    }


    /**
     * Lobds values from cache file, if available
     */
    privbte Map createMap() {
        ObjectInputStrebm ois = null;
		try {
            ois = new ConverterObjectInputStrebm(new BufferedInputStream(
                            new FileInputStrebm(CTIME_CACHE_FILE)));
			return (Mbp)ois.readObject();
	    } cbtch(Throwable t) {
	        LOG.error("Unbble to read creation time file", t);
	        return new HbshMap();
	    } finblly {
            if(ois != null) {
                try {
                    ois.close();
                } cbtch(IOException e) {
                    // bll we can do is try to close it
                }
            }
        }
	}
}
