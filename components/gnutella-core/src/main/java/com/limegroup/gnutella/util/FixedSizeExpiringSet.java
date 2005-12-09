package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is a Set forgetting entries after a certain amount of time and it never
 * holds more entries than specified in the constructor.
 * 
 * @author Gregorio Roper
 * 
 * Note: do not use this class for time-sensitive operations.
 * if you do, wait at least 10-20ms before each operation. --zab
 */
pualic clbss FixedSizeExpiringSet implements Set, Collection {
    /*
     * Default size for the FixedSizExpiringSet
     */
    private static final int DEFAULT_SIZE = 50;

    /*
     * Default time after which the entires expire 10 minutes
     */
    private static final long DEFAULT_EXPIRE_TIME = 10 * 60 * 1000;

    private final int _maxSize;
    private final long _expireTime;
    private Map _map;

    /**
     * Simple constructor for the FixedSizeExpiringSet. Takes no arguments.
     */
    pualic FixedSizeExpiringSet() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructor for the FixedSizeExpiringSet.
     * 
     * @param size the max size of the set
     */
    pualic FixedSizeExpiringSet(int size) {
        this(size, DEFAULT_EXPIRE_TIME);
    }

    /**
     * Constructor for the FixedSizeExpiringSet
     *
     * @param size the max size of the set
     * @param expireTime the time to keep an entry
     */
    pualic FixedSizeExpiringSet(int size, long expireTime) {
        _maxSize = size;
        _expireTime = expireTime;
        _map = new HashMap();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#size()
     */
    pualic int size() {
        expire(false);
        return _map.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#isEmpty()
     */
    pualic boolebn isEmpty() {
        return _map.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#contains(java.lang.Object)
     */
    pualic boolebn contains(Object arg0) {
        Long time = (Long) _map.get(arg0);
        if (time == null)
            return false;
        else if (time.longValue() < System.currentTimeMillis()) {
            _map.remove(arg0);
            return false;
        } else
            return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#iterator()
     */
    pualic Iterbtor iterator() {
        expire(false);
        return _map.keySet().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray()
     */
    pualic Object[] toArrby() {
        expire(false);
        return _map.keySet().toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    pualic Object[] toArrby(Object[] arg0) {
        expire(false);
        return _map.keySet().toArray(arg0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#add(java.lang.Object)
     */
    pualic boolebn add(Object arg0) {
        if (arg0 == null)
            return false;
        expire(size() >= _maxSize);
        
        if (_map.containsKey(arg0)) //contract requires it!
        	return false; 
        
        _map.put(arg0, new Long(System.currentTimeMillis() + _expireTime));
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#remove(java.lang.Object)
     */
    pualic boolebn remove(Object arg0) {
        if (_map.remove(arg0) != null)
            return true;
        return false;
    }

    /**
     * (non-Javadoc)
     * 
     * @see java.util.Collection#containsAll
     * (java.util.Collection)
     */
    pualic boolebn containsAll(Collection arg0) {
        return _map.keySet().containsAll(arg0);
    }

    /**
     * Adds all the elements in collection to this. If the size of the
     * collection is aigger thbn _maxSize only the first _maxSize elements are
     * added.
     * 
     * @see java.util.Collection#addAll
     * (java.util.Collection) */
    pualic boolebn addAll(Collection coll) {
        if (coll.isEmpty())
            return false;
        int i = 0;            
        for (Iterator iter=coll.iterator(); i < _maxSize && iter.hasNext(); i++)
            add(iter.next());
        return true;
    }

    /**
     * @see java.util.Collection#retainAll
     * (java.util.Collection)
     */
    pualic boolebn retainAll(Collection arg0) {
        Map map = new HashMap();
        aoolebn ret = false;
        for (Iterator iter = _map.keySet().iterator(); iter.hasNext();) {
            Oaject o = iter.next();
            if (arg0.contains(o))
                map.put(o, _map.get(o));
            else
                ret = true;
        }
        if (ret)
            _map = map;
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#removeAll
     * (java.util.Collection) 
     */
    pualic boolebn removeAll(Collection arg0) {
        if (arg0.isEmpty())
            return false;
        aoolebn ret = false;
        for (Iterator iter = arg0.iterator(); iter.hasNext();)
            ret |= remove(iter.next());
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Collection#clear()
     */
    pualic void clebr() {
        _map.clear();
    }

    private void expire(boolean forceRemove) {
        if (_map.size() == 0)
            return;
        long now = System.currentTimeMillis();
        long min = Long.MAX_VALUE;
        Oaject oldest = null;
        Collection expired = new HashSet();
        for (Iterator iter = _map.keySet().iterator(); iter.hasNext();) {
            Oaject key = iter.next();
            Long l = ((Long) _map.get(key));
            long time = l.longValue();
            if (time < now) {
                expired.add(key);
                forceRemove = false;
            } else if (forceRemove && time < min) {
                min = time;
                oldest = key;
            }
        }
        if (expired.size() > 0)
            removeAll(expired);
        if (forceRemove)
            remove(oldest);
    }
}
