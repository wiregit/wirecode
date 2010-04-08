package com.limegroup.gnutella.related.berkeley;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.SHA1;
import org.limewire.util.Base32;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.related.FileRelationCache;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.PrimaryIndex;

public class BerkeleyFileRelationCache implements FileRelationCache {

    private static final Log LOG =
        LogFactory.getLog(BerkeleyFileRelationCache.class);

    private Environment environment;
    private PrimaryIndex<String, UrnSet> index;

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    void setIndex(PrimaryIndex<String, UrnSet> index) {
        this.index = index;
    }

    @Override
    public void addRelations(SortedSet<URN> files) {
        if(environment == null || index == null)
            return;
        if(files.isEmpty())
            return;
        SHA1 digest = new SHA1();
        for(URN file : files) {
            digest.update(file.getBytes());
        }
        String browse = Base32.encode(digest.digest());
        if(LOG.isDebugEnabled())
            LOG.debug("Created browse URN " + browse);
        try {
            SortedSet<String> rawFiles = new TreeSet<String>();
            for(URN file : files) {
                rawFiles.add(file.getNamespaceSpecificString());
            }
            index.put(new UrnSet(browse, rawFiles));
            for(String file : rawFiles) {
                addToSet(file, browse);
            }
            return;
        } catch(DatabaseException e) {
            LOG.error("Error creating relations", e);
            return;
        }
    }

    @Override
    public void removeAllBrowses(URN file) {
        if(environment == null || index == null)
            return;
        try {
            String rawFile = file.getNamespaceSpecificString();
            UrnSet browses = index.get(rawFile);
            if(browses == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("No browses to remove for " + file);
                return;
            }
            index.delete(rawFile);
            if(LOG.isDebugEnabled())
                LOG.debug("Removing " + browses.size() + " browses for " + file);
            for(String browse : browses) {
                UrnSet files = index.get(browse);
                if(files == null) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(browse + " not found while removing " + file);
                    continue;
                }
                index.delete(browse);
                for(String file1 : files) {
                    removeFromSet(file1, browse);
                }
            }
            return;
        } catch(DatabaseException e) {
            LOG.error("Error creating relations", e);
            return;
        }
    }

    @Override
    public Set<URN> getRelated(URN file) {
        if(environment == null || index == null)
            return Collections.emptySet();
        try {
            String rawFile = file.getNamespaceSpecificString();
            UrnSet browses = index.get(rawFile);
            if(browses == null)
                return Collections.emptySet();
            Set<URN> related = new TreeSet<URN>();
            for(String browse : browses) {
                UrnSet files = index.get(browse);
                if(files == null) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(browse + " not found for " + file);
                    continue;
                }
                try {
                    for(String file1 : files) {
                        related.add(URN.createSHA1Urn("urn:sha1:" + file1));
                    }
                } catch(IOException e) {
                    LOG.error("Error creating URN", e);
                }
            }
            related.remove(file);
            return related;
        } catch(DatabaseException e) {
            LOG.error("Error creating relations", e);
            return Collections.emptySet();
        }
    }

    private void addToSet(String key, String value) throws DatabaseException {
        Transaction txn = environment.beginTransaction(null, null);
        try {
            UrnSet values = index.get(txn, key, null);
            if(values == null)
                values = new UrnSet(key, new TreeSet<String>());
            values.add(value);
            index.put(txn, values);
            txn.commit();
        } catch(DatabaseException e) {
            LOG.error("Error adding value to set", e);
            txn.abort();
        }
    }

    private void removeFromSet(String key, String value) throws DatabaseException {
        Transaction txn = environment.beginTransaction(null, null);
        try {
            UrnSet values = index.get(txn, key, null);
            if(values == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug(key + " not found while removing " + value);
                txn.abort();
                return;
            }
            values.remove(value);
            if(values.isEmpty())
                index.delete(txn, key);
            else
                index.put(txn, values);
            txn.commit();
        } catch(DatabaseException e) {
            LOG.error("Error removing value from set", e);
            txn.abort();
        }
    }
}
