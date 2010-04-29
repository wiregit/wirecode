package com.limegroup.gnutella.related;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.URN;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

class BerkeleyFileCache implements Cache<URN> {

    private static final int MAX_SIZE = 100 * 1000;
    private static final int COMPACT_SIZE = 95 * 1000;

    private static final Log LOG = LogFactory.getLog(BerkeleyFileCache.class);

    private final String name;
    private Environment environment;
    private PrimaryIndex<String, Urn> index;
    private SecondaryIndex<Long, String, Urn> accessTimeIndex;

    BerkeleyFileCache(String name) {
        this.name = name;
    }

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    void setIndex(PrimaryIndex<String, Urn> index) {
        this.index = index;
    }

    void setAccessTimeIndex(SecondaryIndex<Long, String, Urn> accessTimeIndex) {
        this.accessTimeIndex = accessTimeIndex;
    }

    @Override
    public boolean add(URN urn) {
        if(index == null)
            return false;
        try {
            // This will replace the old timestamp, if there is one
            if(index.put(new Urn(urn.getNamespaceSpecificString())) == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Added " + urn + " to " + name);
                considerCompacting();
                return true;
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug(urn + " was already in " + name);
                return false;
            }
        } catch(DatabaseException e) {
            LOG.error("Error adding key", e);
            return false;
        }
    }

    @Override
    public boolean remove(URN urn) {
        if(index == null)
            return false;
        try {
            if(index.delete(urn.getNamespaceSpecificString())) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Removed " + urn + " from " + name);
                return true;
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug(urn + " was not in " + name);
                return false;
            }
        } catch(DatabaseException e) {
            LOG.error("Error removing key", e);
            return false;
        }
    }

    @Override
    public boolean contains(URN urn) {
        if(index == null)
            return false;
        try {
            if(index.get(urn.getNamespaceSpecificString()) == null)
                return false;
            // No locking between get() and put(), but that's OK - timestamps
            // always increase so it doesn't matter if we clobber another put()
            index.putNoReturn(new Urn(urn.getNamespaceSpecificString()));
            return true;
        } catch(DatabaseException e) {
            LOG.error("Error checking key", e);
            return false;
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
            EntityCursor<Urn> cursor = accessTimeIndex.entities(txn, null);
            while(count > COMPACT_SIZE) {
                Urn u = cursor.next();
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