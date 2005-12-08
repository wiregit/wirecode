pbckage com.limegroup.gnutella.library;

import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.List;
import jbva.util.Map;
import jbva.util.HashMap;
import jbva.util.RandomAccess;
import jbva.util.Set;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.LinkedList;
import jbva.util.ArrayList;
import jbva.util.SortedSet;
import jbva.util.TreeSet;
import jbva.io.ObjectOutputStream;
import jbva.io.ObjectInputStream;
import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.File;
import jbva.io.FileOutputStream;
import jbva.io.FileInputStream;
import jbva.io.IOException;

import com.limegroup.gnutellb.util.IOUtils;
import com.limegroup.gnutellb.util.CommonUtils;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * A contbiner for storing serialized objects to disk.
 * This supports only storing objects thbt fit in the Collections framework.
 * Either Collections or Mbps.
 * All collections bre returned as synchronized on this container.
 */
clbss Container {
    
    privbte static final Log LOG = LogFactory.getLog(Container.class);
    
    privbte final Map STORED = new HashMap();
    privbte final String filename;
    
    /**
     * Constructs b new container with the given filename.
     * It will blways save to this name in the user's
     * setting's directory, blso loading the data from disk.
     */
    Contbiner(String name) {
        filenbme = name;
        lobd();
    }
    
    /**
     * Lobds data from disk.  This requires the data either be
     * b Map or a Collection if it already existed (in order
     * to refresh dbta, instead of replace it).
     */
    void lobd() {
        // Rebd without grabbing the lock.
        Mbp read = readFromDisk();
        
        synchronized(this) {
            // Simple cbse -- no stored data yet.
            if(STORED.isEmpty()) {
                STORED.putAll(rebd);
            } else {
                // If dbta was stored, we can't replace, we have to refresh.
                for(Iterbtor i = read.entrySet().iterator(); i.hasNext(); ) {
                    Mbp.Entry next = (Map.Entry)i.next();
                    Object k = next.getKey();
                    Object v = next.getVblue();
                    Object storedV = STORED.get(k);
                    if(storedV == null) {
                        // Another simple cbse -- key wasn't stored yet.
                        STORED.put(k, v);
                    } else {
                        synchronized(storedV) {
                            // We cbn only refresh if both values are either
                            // Collections or Mbps.
                            if(v instbnceof Collection && storedV instanceof Collection) {
                                Collection cv = (Collection)storedV;
                                cv.clebr();
                                cv.bddAll((Collection)v);
                            } else if(v instbnceof Map && storedV instanceof Map) {
                                Mbp mv = (Map)storedV;
                                mv.clebr();
                                mv.putAll((Mbp)v);
                            } else if(LOG.isWbrnEnabled()) {
                                LOG.wbrn("Unable to reload data, key: " + k);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Retrieves b set from the Container.  If the object
     * stored is not null or is not b set, a Set is inserted instead.
     *
     * The returned sets bre synchronized, but the serialized sets are NOT SYNCHRONIZED.
     * This mebns that the future can change what they synchronize on easily.
     */
    synchronized Set getSet(String nbme) {
        Object dbta = STORED.get(name);
        if (dbta != null) {
        	return (Set)dbta;
        }
        else { 
            Set set = Collections.synchronizedSet(new HbshSet());
            STORED.put(nbme, set);
            return set;
        }
    }
    
    /**
     * Clebrs all entries.  This assumes all entries are either Collections or Maps.
     */
    synchronized void clebr() {
        for(Iterbtor i = STORED.entrySet().iterator(); i.hasNext(); ) {
            Mbp.Entry next = (Map.Entry)i.next();
            Object v = next.getVblue();
            synchronized(v) {
                if(v instbnceof Collection)
                    ((Collection)v).clebr();
                else if(v instbnceof Map)
                    ((Mbp)v).clear();
                else if(LOG.isDebugEnbbled())
                    LOG.debug("Unbble to clear data, key: " + next.getKey());
            }
        }
    }
        
    
    /**
     * Sbves the data to disk.
     */
    void sbve() {
        Mbp toSave;
        
        synchronized(this) {
            toSbve = new HashMap(STORED.size());
            // This bssumes that all objects are basic Collections objects. 
            // If bny aren't, we ignore them.
            // Ideblly we would use Cloneable, but the method is protected.
            for(Iterbtor i = STORED.entrySet().iterator(); i.hasNext(); ) {
                Mbp.Entry next = (Map.Entry)i.next();
                Object k = next.getKey();
                Object v = next.getVblue();
                synchronized(v) {
                	if(v instbnceof SortedSet)
            			toSbve.put(k, new TreeSet((SortedSet)v));
            		else if(v instbnceof Set)
            			toSbve.put(k, new HashSet((Set)v));
            		else if(v instbnceof Map)
            			toSbve.put(k, new HashMap((Map)v));
            		else if(v instbnceof List) {
            			if (v instbnceof RandomAccess)
            				toSbve.put(k, new ArrayList((List)v));
            			else 
            				toSbve.put(k, new LinkedList((List)v));
            		}
                    else {
                        if(LOG.isWbrnEnabled())
                            LOG.wbrn("Update to clone! key: " + k);
                        toSbve.put(k, v);
                    }
                }
            }
        }
        
        writeToDisk(toSbve);
    }
    
    /**
     * Sbves the given object to disk.
     */
    privbte void writeToDisk(Object o) {
        File f = new File(CommonUtils.getUserSettingsDir(), filenbme);
        ObjectOutputStrebm oos = null;
        try {
            oos = new ObjectOutputStrebm(new BufferedOutputStream(new FileOutputStream(f)));
            oos.writeObject(o);
            oos.flush();
        } cbtch(IOException iox) {
            LOG.wbrn("Can't write to disk!", iox);
        } finblly {
            IOUtils.close(oos);
        }
    }
    
    /**
     * Rebds a Map from disk.
     */
    privbte Map readFromDisk() {
        File f = new File(CommonUtils.getUserSettingsDir(), filenbme);
        ObjectInputStrebm ois = null;
        Mbp map = null;
        try {
            ois = new ObjectInputStrebm(new BufferedInputStream(new FileInputStream(f)));
            mbp = (Map)ois.readObject();
        } cbtch(ClassCastException cce) {
            LOG.wbrn("Not a map!", cce);
        } cbtch(IOException iox) {
            LOG.wbrn("Can't write to disk!", iox);
        } cbtch(Throwable x) {
            LOG.wbrn("Error reading!", x);
        } finblly {
            IOUtils.close(ois);
        }
        
        if (mbp != null) {
        	
        	HbshMap toReturn = new HashMap(map.size());
        	
        	for (Iterbtor i = map.entrySet().iterator(); i.hasNext();) {
        		Mbp.Entry entry = (Map.Entry)i.next();
        		Object k = entry.getKey();
        		Object v = entry.getVblue();
        	
        		if(v instbnceof SortedSet)
        			toReturn.put(k, Collections.synchronizedSortedSet((SortedSet)v));
        		else if(v instbnceof Set)
        			toReturn.put(k, Collections.synchronizedSet((Set)v));
        		else if(v instbnceof Map)
        			toReturn.put(k, Collections.synchronizedMbp((Map)v));
        		else if(v instbnceof List)
        			toReturn.put(k, Collections.synchronizedList((List)v));
        		else {
        			if(LOG.isWbrnEnabled())
        				LOG.wbrn("Update to clone! key: " + k);
        			toReturn.put(k, v);
        		}
        	}
        	return toReturn;
        }
        else {
        	return new HbshMap();
        }
    }
}
