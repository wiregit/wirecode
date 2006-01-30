package com.limegroup.gnutella.library;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.RandomAccess;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.CommonUtils;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A container for storing serialized objects to disk.
 * This supports only storing objects that fit in the Collections framework.
 * Either Collections or Maps.
 * All collections are returned as synchronized on this container.
 */
class Container {
    
    private static final Log LOG = LogFactory.getLog(Container.class);
    
    private final Map STORED = new HashMap();
    private final String filename;
    
    /**
     * Constructs a new container with the given filename.
     * It will always save to this name in the user's
     * setting's directory, also loading the data from disk.
     */
    Container(String name) {
        filename = name;
        load();
    }
    
    /**
     * Loads data from disk.  This requires the data either be
     * a Map or a Collection if it already existed (in order
     * to refresh data, instead of replace it).
     */
    void load() {
        // Read without grabbing the lock.
        Map read = readFromDisk();
        
        synchronized(this) {
            // Simple case -- no stored data yet.
            if(STORED.isEmpty()) {
                STORED.putAll(read);
            } else {
                // If data was stored, we can't replace, we have to refresh.
                for(Iterator i = read.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry next = (Map.Entry)i.next();
                    Object k = next.getKey();
                    Object v = next.getValue();
                    Object storedV = STORED.get(k);
                    if(storedV == null) {
                        // Another simple case -- key wasn't stored yet.
                        STORED.put(k, v);
                    } else {
                        synchronized(storedV) {
                            // We can only refresh if both values are either
                            // Collections or Maps.
                            if(v instanceof Collection && storedV instanceof Collection) {
                                Collection cv = (Collection)storedV;
                                cv.clear();
                                cv.addAll((Collection)v);
                            } else if(v instanceof Map && storedV instanceof Map) {
                                Map mv = (Map)storedV;
                                mv.clear();
                                mv.putAll((Map)v);
                            } else if(LOG.isWarnEnabled()) {
                                LOG.warn("Unable to reload data, key: " + k);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Retrieves a set from the Container.  If the object
     * stored is not null or is not a set, a Set is inserted instead.
     *
     * The returned sets are synchronized, but the serialized sets are NOT SYNCHRONIZED.
     * This means that the future can change what they synchronize on easily.
     */
    synchronized Set getSet(String name) {
        Object data = STORED.get(name);
        if (data != null) {
        	return (Set)data;
        }
        else { 
            Set set = Collections.synchronizedSet(new HashSet());
            STORED.put(name, set);
            return set;
        }
    }
    
    /**
     * Clears all entries.  This assumes all entries are either Collections or Maps.
     */
    synchronized void clear() {
        for(Iterator i = STORED.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            Object v = next.getValue();
            synchronized(v) {
                if(v instanceof Collection)
                    ((Collection)v).clear();
                else if(v instanceof Map)
                    ((Map)v).clear();
                else if(LOG.isDebugEnabled())
                    LOG.debug("Unable to clear data, key: " + next.getKey());
            }
        }
    }
        
    
    /**
     * Saves the data to disk.
     */
    void save() {
        Map toSave;
        
        synchronized(this) {
            toSave = new HashMap(STORED.size());
            // This assumes that all objects are basic Collections objects. 
            // If any aren't, we ignore them.
            // Ideally we would use Cloneable, but the method is protected.
            for(Iterator i = STORED.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                Object k = next.getKey();
                Object v = next.getValue();
                synchronized(v) {
                	if(v instanceof SortedSet)
            			toSave.put(k, new TreeSet((SortedSet)v));
            		else if(v instanceof Set)
            			toSave.put(k, new HashSet((Set)v));
            		else if(v instanceof Map)
            			toSave.put(k, new HashMap((Map)v));
            		else if(v instanceof List) {
            			if (v instanceof RandomAccess)
            				toSave.put(k, new ArrayList((List)v));
            			else 
            				toSave.put(k, new LinkedList((List)v));
            		}
                    else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Update to clone! key: " + k);
                        toSave.put(k, v);
                    }
                }
            }
        }
        
        writeToDisk(toSave);
    }
    
    /**
     * Saves the given object to disk.
     */
    private void writeToDisk(Object o) {
        File f = new File(CommonUtils.getUserSettingsDir(), filename);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            oos.writeObject(o);
            oos.flush();
        } catch(IOException iox) {
            LOG.warn("Can't write to disk!", iox);
        } finally {
            IOUtils.close(oos);
        }
    }
    
    /**
     * Reads a Map from disk.
     */
    private Map readFromDisk() {
        File f = new File(CommonUtils.getUserSettingsDir(), filename);
        ObjectInputStream ois = null;
        Map map = null;
        try {
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
            map = (Map)ois.readObject();
        } catch(ClassCastException cce) {
            LOG.warn("Not a map!", cce);
        } catch(IOException iox) {
            LOG.warn("Can't write to disk!", iox);
        } catch(Throwable x) {
            LOG.warn("Error reading!", x);
        } finally {
            IOUtils.close(ois);
        }
        
        if (map != null) {
        	
        	HashMap toReturn = new HashMap(map.size());
        	
        	for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
        		Map.Entry entry = (Map.Entry)i.next();
        		Object k = entry.getKey();
        		Object v = entry.getValue();
        	
        		if(v instanceof SortedSet)
        			toReturn.put(k, Collections.synchronizedSortedSet((SortedSet)v));
        		else if(v instanceof Set)
        			toReturn.put(k, Collections.synchronizedSet((Set)v));
        		else if(v instanceof Map)
        			toReturn.put(k, Collections.synchronizedMap((Map)v));
        		else if(v instanceof List)
        			toReturn.put(k, Collections.synchronizedList((List)v));
        		else {
        			if(LOG.isWarnEnabled())
        				LOG.warn("Update to clone! key: " + k);
        			toReturn.put(k, v);
        		}
        	}
        	return toReturn;
        }
        else {
        	return new HashMap();
        }
    }
}