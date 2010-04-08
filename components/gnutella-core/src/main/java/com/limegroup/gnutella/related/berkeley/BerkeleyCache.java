package com.limegroup.gnutella.related.berkeley;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.related.Cache;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;

public class BerkeleyCache<K> implements Cache<K> {

    private static final Log LOG = LogFactory.getLog(BerkeleyCache.class);

    private final String name;
    private Environment environment;
    private PrimaryIndex<String, Count> index;

    public BerkeleyCache(String name) {
        this.name = name;
    }

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    void setIndex(PrimaryIndex<String, Count> index) {
        this.index = index;
    }

    @Override
    public boolean add(K key) {
        if(index == null)
            return false;
        try {
            if(index.put(new Count(key.toString())) == null) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Added " + key + " to " + name);
                return true;
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug(key + " was already in " + name);
                return false;
            }
        } catch(DatabaseException e) {
            LOG.error("Error adding key", e);
            return false;
        }
    }

    @Override
    public boolean remove(K key) {
        if(index == null)
            return false;
        try {
            if(index.delete(key.toString())) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Removed " + key + " from " + name);
                return true;
            } else {
                if(LOG.isDebugEnabled())
                    LOG.debug(key + " was not in " + name);
                return false;
            }
        } catch(DatabaseException e) {
            LOG.error("Error removing key", e);
            return false;
        }
    }

    @Override
    public boolean contains(K key) {
        if(index == null)
            return false;
        try {
            return index.get(key.toString()) != null;
        } catch(DatabaseException e) {
            LOG.error("Error checking key", e);
            return false;
        }
    }

    @Override
    public int get(K key) {
        if(index == null)
            return 0;
        try {
            Count c = index.get(key.toString());
            if(c == null)
                return 0;
            return c.count;
        } catch(DatabaseException e) {
            LOG.error("Error getting value", e);
            return 0;
        }
    }

    @Override
    public int increment(K key) {
        if(environment == null || index == null)
            return 0;
        Transaction txn = null;
        try {
            txn = environment.beginTransaction(null, null);
            Count count = index.get(txn, key.toString(), null);
            if(count == null)
                count = new Count(key.toString());
            count.count++;
            index.put(txn, count);
            txn.commit();
            return count.count;
        } catch(DatabaseException e) {
            LOG.error("Error incrementing value", e);
            abort(txn);
            return 0;
        }
    }

    @Override
    public int total() {
        if(environment == null || index == null)
            return 0;
        EntityCursor<Count> cursor = null;
        try {
            int total = 0;
            cursor = index.entities();
            for(Count c = cursor.first(); c != null; c = cursor.next()) {
                total += c.count;
            }
            cursor.close();
            return total;
        } catch(DatabaseException e) {
            LOG.error("Error totalling values", e);
            close(cursor);
            return 0;
        }
    }

    private void abort(Transaction txn) {
        try {
            if(txn != null)
                txn.abort();
        } catch(DatabaseException e) {
            LOG.error("Error while aborting transaction", e);
        }
    }

    private void close(EntityCursor cursor) {
        try {
            if(cursor != null)
                cursor.close();
        } catch(DatabaseException e) {
            LOG.error("Error while closing cursor", e);
        }
    }
}
