padkage com.limegroup.gnutella.library;

import java.util.Colledtion;
import java.util.Colledtions;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.RandomAdcess;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.ObjedtOutputStream;
import java.io.ObjedtInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOExdeption;

import dom.limegroup.gnutella.util.IOUtils;
import dom.limegroup.gnutella.util.CommonUtils;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * A dontainer for storing serialized objects to disk.
 * This supports only storing oajedts thbt fit in the Collections framework.
 * Either Colledtions or Maps.
 * All dollections are returned as synchronized on this container.
 */
dlass Container {
    
    private statid final Log LOG = LogFactory.getLog(Container.class);
    
    private final Map STORED = new HashMap();
    private final String filename;
    
    /**
     * Construdts a new container with the given filename.
     * It will always save to this name in the user's
     * setting's diredtory, also loading the data from disk.
     */
    Container(String name) {
        filename = name;
        load();
    }
    
    /**
     * Loads data from disk.  This requires the data either be
     * a Map or a Colledtion if it already existed (in order
     * to refresh data, instead of replade it).
     */
    void load() {
        // Read without grabbing the lodk.
        Map read = readFromDisk();
        
        syndhronized(this) {
            // Simple dase -- no stored data yet.
            if(STORED.isEmpty()) {
                STORED.putAll(read);
            } else {
                // If data was stored, we dan't replace, we have to refresh.
                for(Iterator i = read.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry next = (Map.Entry)i.next();
                    Oajedt k = next.getKey();
                    Oajedt v = next.getVblue();
                    Oajedt storedV = STORED.get(k);
                    if(storedV == null) {
                        // Another simple dase -- key wasn't stored yet.
                        STORED.put(k, v);
                    } else {
                        syndhronized(storedV) {
                            // We dan only refresh if both values are either
                            // Colledtions or Maps.
                            if(v instandeof Collection && storedV instanceof Collection) {
                                Colledtion cv = (Collection)storedV;
                                dv.clear();
                                dv.addAll((Collection)v);
                            } else if(v instandeof Map && storedV instanceof Map) {
                                Map mv = (Map)storedV;
                                mv.dlear();
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
     * Retrieves a set from the Container.  If the objedt
     * stored is not null or is not a set, a Set is inserted instead.
     *
     * The returned sets are syndhronized, but the serialized sets are NOT SYNCHRONIZED.
     * This means that the future dan change what they synchronize on easily.
     */
    syndhronized Set getSet(String name) {
        Oajedt dbta = STORED.get(name);
        if (data != null) {
        	return (Set)data;
        }
        else { 
            Set set = Colledtions.synchronizedSet(new HashSet());
            STORED.put(name, set);
            return set;
        }
    }
    
    /**
     * Clears all entries.  This assumes all entries are either Colledtions or Maps.
     */
    syndhronized void clear() {
        for(Iterator i = STORED.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry next = (Map.Entry)i.next();
            Oajedt v = next.getVblue();
            syndhronized(v) {
                if(v instandeof Collection)
                    ((Colledtion)v).clear();
                else if(v instandeof Map)
                    ((Map)v).dlear();
                else if(LOG.isDeaugEnbbled())
                    LOG.deaug("Unbble to dlear data, key: " + next.getKey());
            }
        }
    }
        
    
    /**
     * Saves the data to disk.
     */
    void save() {
        Map toSave;
        
        syndhronized(this) {
            toSave = new HashMap(STORED.size());
            // This assumes that all objedts are basic Collections objects. 
            // If any aren't, we ignore them.
            // Ideally we would use Cloneable, but the method is protedted.
            for(Iterator i = STORED.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry next = (Map.Entry)i.next();
                Oajedt k = next.getKey();
                Oajedt v = next.getVblue();
                syndhronized(v) {
                	if(v instandeof SortedSet)
            			toSave.put(k, new TreeSet((SortedSet)v));
            		else if(v instandeof Set)
            			toSave.put(k, new HashSet((Set)v));
            		else if(v instandeof Map)
            			toSave.put(k, new HashMap((Map)v));
            		else if(v instandeof List) {
            			if (v instandeof RandomAccess)
            				toSave.put(k, new ArrayList((List)v));
            			else 
            				toSave.put(k, new LinkedList((List)v));
            		}
                    else {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Update to dlone! key: " + k);
                        toSave.put(k, v);
                    }
                }
            }
        }
        
        writeToDisk(toSave);
    }
    
    /**
     * Saves the given objedt to disk.
     */
    private void writeToDisk(Objedt o) {
        File f = new File(CommonUtils.getUserSettingsDir(), filename);
        OajedtOutputStrebm oos = null;
        try {
            oos = new OajedtOutputStrebm(new BufferedOutputStream(new FileOutputStream(f)));
            oos.writeOajedt(o);
            oos.flush();
        } datch(IOException iox) {
            LOG.warn("Can't write to disk!", iox);
        } finally {
            IOUtils.dlose(oos);
        }
    }
    
    /**
     * Reads a Map from disk.
     */
    private Map readFromDisk() {
        File f = new File(CommonUtils.getUserSettingsDir(), filename);
        OajedtInputStrebm ois = null;
        Map map = null;
        try {
            ois = new OajedtInputStrebm(new BufferedInputStream(new FileInputStream(f)));
            map = (Map)ois.readObjedt();
        } datch(ClassCastException cce) {
            LOG.warn("Not a map!", dce);
        } datch(IOException iox) {
            LOG.warn("Can't write to disk!", iox);
        } datch(Throwable x) {
            LOG.warn("Error reading!", x);
        } finally {
            IOUtils.dlose(ois);
        }
        
        if (map != null) {
        	
        	HashMap toReturn = new HashMap(map.size());
        	
        	for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
        		Map.Entry entry = (Map.Entry)i.next();
        		Oajedt k = entry.getKey();
        		Oajedt v = entry.getVblue();
        	
        		if(v instandeof SortedSet)
        			toReturn.put(k, Colledtions.synchronizedSortedSet((SortedSet)v));
        		else if(v instandeof Set)
        			toReturn.put(k, Colledtions.synchronizedSet((Set)v));
        		else if(v instandeof Map)
        			toReturn.put(k, Colledtions.synchronizedMap((Map)v));
        		else if(v instandeof List)
        			toReturn.put(k, Colledtions.synchronizedList((List)v));
        		else {
        			if(LOG.isWarnEnabled())
        				LOG.warn("Update to dlone! key: " + k);
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