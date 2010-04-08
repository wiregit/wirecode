package com.limegroup.gnutella.related.memory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SHA1;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.FileRelationCache;

public class InMemoryFileRelationCache implements FileRelationCache {

    private static final Log LOG =
        LogFactory.getLog(InMemoryFileRelationCache.class);

    private final String filename;
    private OneToManyCache<URN, URN> map; // LOCKING: this

    public InMemoryFileRelationCache(String filename, int initialSize, int maxSize) {
        this.filename = filename;
        map = new OneToManyCache<URN, URN>(initialSize, maxSize);
    }

    @Override
    public void addRelations(SortedSet<URN> files) {
        if(files.isEmpty())
            return;
        SHA1 digest = new SHA1();
        for(URN file : files) {
            digest.update(file.getBytes());
        }
        URN browse;
        try {
            browse = URN.createSHA1UrnFromBytes(digest.digest());
        } catch(IOException e) {
            LOG.error("Error creating browse URN", e);
            return;
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Created browse URN " + browse);
        synchronized(this) {
            map.put(browse, files);
            for(URN file : files) {
                map.addToSet(file, browse);
            }
        }
    }

    @Override
    public synchronized void removeAllBrowses(URN file) {
        Set<URN> browses = map.remove(file);
        if(browses == null) {
            if(LOG.isDebugEnabled())
                LOG.debug("No browses to remove for " + file);
            return;
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Removing " + browses.size() + " browses for " + file);
        for(URN browse : browses) {
            Set<URN> files = map.remove(browse);
            // The browse might have expired from the cache
            if(files != null) {
                for(URN file1 : files) {
                    map.removeFromSet(file1, browse);
                }
            }
        }
    }

    @Override
    public synchronized Set<URN> getRelated(URN file) {
        Set<URN> browses = map.get(file);
        if(browses == null)
            return Collections.emptySet();
        Set<URN> related = new TreeSet<URN>();
        for(URN browse : browses) {
            Set<URN> files = map.get(browse);
            // The browse might have expired from the cache
            if(files != null) {
                related.addAll(files);
            }
        }
        related.remove(file);
        return related;
    }

    void load() {
        load(getFile());
    }

    void load(File file) {
        OneToManyCache<URN, URN> temp;
        synchronized(this) {
            temp = map.clone();
        }
        temp.load(file); // IO outside the lock
        synchronized(this) {
            map = temp;
        }
    }

    void save() {
        save(getFile());
    }

    void save(File file) {
        OneToManyCache<URN, URN> temp;
        synchronized(this) {
            temp = map.clone();
        }
        temp.save(file); // IO outside the lock
    }

    private File getFile() {
        return new File(CommonUtils.getUserSettingsDir(), filename);
    }
}
