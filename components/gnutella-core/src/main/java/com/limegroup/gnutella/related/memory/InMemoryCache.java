package com.limegroup.gnutella.related.memory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.related.Cache;

public class InMemoryCache<K> implements Cache<K> {

    private static final Log LOG =
        LogFactory.getLog(InMemoryCache.class);

    private final String filename;
    private final int maxSize;
    private Map<K, Integer> map; // LOCKING: this

    public InMemoryCache(String filename, int initialSize, int maxSize) {
        this.filename = filename;
        this.maxSize = maxSize;
        map = new LinkedHashMap<K, Integer>(initialSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Integer> e) {
                if(size() > InMemoryCache.this.maxSize) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(e.getKey() + " expired from " +
                                InMemoryCache.this.filename);
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public synchronized boolean add(K key) {
        if(map.put(key, 0) == null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Added " + key + " to " + filename);
            return true;
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug(key + " was already in " + filename);
            return false;
        }
    }

    @Override
    public synchronized boolean remove(K key) {
        if(map.remove(key) != null) {
            if(LOG.isDebugEnabled())
                LOG.debug("Removed " + key + " from " + filename);
            return true;
        } else {
            if(LOG.isDebugEnabled())
                LOG.debug(key + " was not in " + filename);
            return false;
        }
    }

    @Override
    public synchronized boolean contains(K key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized int get(K key) {
        Integer i = map.get(key);
        if(i == null)
            return 0;
        return i;
    }

    @Override
    public synchronized int increment(K key) {
        Integer i = map.get(key);
        if(i == null)
            i = 0;
        map.put(key, i + 1);
        return i + 1;
    }

    @Override
    public synchronized int total() {
        int total = 0;
        for(Integer i : map.values()) {
            total += i;
        }
        return total;
    }

    void load() {
        load(getFile());
    }

    @SuppressWarnings("unchecked")
    void load(File file) {
        Map<K, Integer> temp;
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(file)));
            temp = (Map) is.readObject();
            if(LOG.isDebugEnabled())
                LOG.debug("Loaded " + temp.size() + " entries from " + filename);
        } catch(IOException e) {
            LOG.error("Error loading cache ", e);
            return;
        } catch(ClassCastException e) {
            LOG.error("Error loading cache ", e);
            return;
        } catch(ClassNotFoundException e) {
            LOG.error("Error loading cache ", e);
            return;
        } finally {
            IOUtils.close(is);
        }
        synchronized(this) {
            for(Map.Entry<K, Integer> e : temp.entrySet()) {
                map.put(e.getKey(), e.getValue());
            }
        }
    }

    void save() {
        save(getFile());
    }

    void save(File file) {
        Map<K, Integer> temp;
        synchronized(this) {
            temp = new LinkedHashMap<K, Integer>(map);
        }
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(file)));
            oos.writeObject(temp);
            oos.flush();
            if(LOG.isDebugEnabled())
                LOG.debug("Saved " + temp.size() + " entries to " + filename);
        } catch(IOException e) {
            LOG.error("Error saving cache", e);
        } finally {
            IOUtils.close(oos);
        }
    }

    private File getFile() {
        return new File(CommonUtils.getUserSettingsDir(), filename);
    }
}