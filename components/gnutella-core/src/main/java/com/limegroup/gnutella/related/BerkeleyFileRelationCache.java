package com.limegroup.gnutella.related;

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
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

class BerkeleyFileRelationCache implements FileRelationCache {

    private static final int MAX_SIZE = 1000 * 1000;
    private static final int COMPACT_SIZE = 950 * 1000;

    private static final Log LOG =
        LogFactory.getLog(BerkeleyFileRelationCache.class);

    private Environment environment;
    private PrimaryIndex<String, UrnSet> index;
    private SecondaryIndex<Long, String, UrnSet> accessTimeIndex;

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    void setIndex(PrimaryIndex<String, UrnSet> index) {
        this.index = index;
    }

    void setAccessTimeIndex(SecondaryIndex<Long, String, UrnSet> accessTimeIndex) {
        this.accessTimeIndex = accessTimeIndex;
    }

    @Override
    public void addRelations(SortedSet<URN> files) {
        if(environment == null || index == null || accessTimeIndex == null) {
            LOG.warn("Not initialized");
            return;
        }
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
            index.putNoReturn(new UrnSet(browse, rawFiles));
            for(String file : rawFiles) {
                addToSet(file, browse);
            }
            considerCompacting();
            return;
        } catch(DatabaseException e) {
            LOG.error("Error creating relations", e);
            return;
        }
    }

    @Override
    public void removeAllBrowses(URN file) {
        if(environment == null || index == null || accessTimeIndex == null) {
            LOG.warn("Not initialized");
            return;
        }
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
        if(environment == null || index == null || accessTimeIndex == null) {
            LOG.warn("Not initialized");
            return Collections.emptySet();
        }
        try {
            String rawFile = file.getNamespaceSpecificString();
            UrnSet browses = index.get(rawFile);
            if(browses == null)
                return Collections.emptySet();
            browses.touch();
            Set<URN> related = new TreeSet<URN>();
            for(String browse : browses) {
                UrnSet files = index.get(browse);
                if(files == null) {
                    if(LOG.isDebugEnabled())
                        LOG.debug(browse + " not found for " + file);
                    continue;
                }
                files.touch();
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
            else
                values.touch();
            values.add(value);
            index.putNoReturn(txn, values);
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
            values.touch();
            values.remove(value);
            if(values.isEmpty())
                index.delete(txn, key);
            else
                index.putNoReturn(txn, values);
            txn.commit();
        } catch(DatabaseException e) {
            LOG.error("Error removing value from set", e);
            txn.abort();
        }
    }

    private void considerCompacting() {
        long count = 0;
        try {
            count = index.count();
            if(count <= MAX_SIZE) {
                LOG.debug("Not compacting database");
                return;
            }
        } catch(DatabaseException e) {
            LOG.error("Error counting records", e);
            return;
        }
        LOG.debug("Compacting database");
        Transaction txn = environment.beginTransaction(null, null);
        try {
            EntityCursor<UrnSet> cursor = accessTimeIndex.entities(txn, null);
            while(count > COMPACT_SIZE) {
                UrnSet u = cursor.next();
                if(u == null)
                    break;
                cursor.delete();
                count--;
            }
            cursor.close();
            txn.commit();
        } catch(DatabaseException e) {
            LOG.error("Error removing old records", e);
            txn.abort();
        }
    }
}
