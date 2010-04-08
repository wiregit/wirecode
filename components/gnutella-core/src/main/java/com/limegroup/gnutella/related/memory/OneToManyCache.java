package com.limegroup.gnutella.related.memory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;

class OneToManyCache<K, V> extends LinkedHashMap<K, Set<V>>
implements Cloneable, Serializable {

    private static final Log LOG = LogFactory.getLog(OneToManyCache.class);

    private final int maxSize;

    OneToManyCache(int initialSize, int maxSize) {
        super(initialSize, 0.75f, true); // LRU replacement policy
        this.maxSize = maxSize;
    }

    // This method will be called on every get(), put(), and putAll()
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, Set<V>> e) {
        return size() > maxSize;
    }

    @Override
    public OneToManyCache<K, V> clone() {
        OneToManyCache<K, V> clone =
            new OneToManyCache<K, V>(size(), maxSize);
        // The iterator returns the least-recently-used entry first
        for(Map.Entry<K, Set<V>> e : entrySet()) {
            clone.put(e.getKey(), new TreeSet<V>(e.getValue()));
        }
        return clone;
    }

    @SuppressWarnings("unchecked")
    void load(File file) {
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(file)));
            Map<K, Set<V>> temp = (Map) is.readObject();
            clear();
            putAll(temp);
            if(LOG.isDebugEnabled())
                LOG.debug("Loaded " + size() + " entries");
        } catch(IOException e) {
            LOG.error("Error loading cache ", e);
        } catch(ClassCastException e) {
            LOG.error("Error loading cache ", e);
        } catch(ClassNotFoundException e) {
            LOG.error("Error loading cache ", e);
        } finally {
            IOUtils.close(is);
        }
    }

    boolean save(File file) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(file)));
            oos.writeObject(this);
            oos.flush();
            if(LOG.isDebugEnabled())
                LOG.debug("Saved " + size() + " entries");
        } catch(IOException e) {
            LOG.error("Error saving cache", e);
            return false;
        } finally {
            IOUtils.close(oos);
        }
        return true;
    }

    boolean addToSet(K key, V value) {
        Set<V> valueSet = get(key);
        if(valueSet == null) {
            valueSet = new TreeSet<V>();
            valueSet.add(value);
            put(key, valueSet);
            return true;
        } else {
            return valueSet.add(value);
        }
    }

    boolean removeFromSet(K key, V value) {
        Set<V> valueSet = get(key);
        if(valueSet == null)
            return false;
        if(!valueSet.remove(value))
            return false;
        if(valueSet.isEmpty())
            remove(key);
        return true;
    }
}