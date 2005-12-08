pbckage com.limegroup.gnutella.util;

import jbva.util.Collection;
import jbva.util.HashMap;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Set;

/**
 * This is b Set forgetting entries after a certain amount of time and it never
 * holds more entries thbn specified in the constructor.
 * 
 * @buthor Gregorio Roper
 * 
 * Note: do not use this clbss for time-sensitive operations.
 * if you do, wbit at least 10-20ms before each operation. --zab
 */
public clbss FixedSizeExpiringSet implements Set, Collection {
    /*
     * Defbult size for the FixedSizExpiringSet
     */
    privbte static final int DEFAULT_SIZE = 50;

    /*
     * Defbult time after which the entires expire 10 minutes
     */
    privbte static final long DEFAULT_EXPIRE_TIME = 10 * 60 * 1000;

    privbte final int _maxSize;
    privbte final long _expireTime;
    privbte Map _map;

    /**
     * Simple constructor for the FixedSizeExpiringSet. Tbkes no arguments.
     */
    public FixedSizeExpiringSet() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructor for the FixedSizeExpiringSet.
     * 
     * @pbram size the max size of the set
     */
    public FixedSizeExpiringSet(int size) {
        this(size, DEFAULT_EXPIRE_TIME);
    }

    /**
     * Constructor for the FixedSizeExpiringSet
     *
     * @pbram size the max size of the set
     * @pbram expireTime the time to keep an entry
     */
    public FixedSizeExpiringSet(int size, long expireTime) {
        _mbxSize = size;
        _expireTime = expireTime;
        _mbp = new HashMap();
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#size()
     */
    public int size() {
        expire(fblse);
        return _mbp.size();
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#isEmpty()
     */
    public boolebn isEmpty() {
        return _mbp.isEmpty();
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#contains(java.lang.Object)
     */
    public boolebn contains(Object arg0) {
        Long time = (Long) _mbp.get(arg0);
        if (time == null)
            return fblse;
        else if (time.longVblue() < System.currentTimeMillis()) {
            _mbp.remove(arg0);
            return fblse;
        } else
            return true;
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#iterator()
     */
    public Iterbtor iterator() {
        expire(fblse);
        return _mbp.keySet().iterator();
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#toArray()
     */
    public Object[] toArrby() {
        expire(fblse);
        return _mbp.keySet().toArray();
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#toArray(java.lang.Object[])
     */
    public Object[] toArrby(Object[] arg0) {
        expire(fblse);
        return _mbp.keySet().toArray(arg0);
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#add(java.lang.Object)
     */
    public boolebn add(Object arg0) {
        if (brg0 == null)
            return fblse;
        expire(size() >= _mbxSize);
        
        if (_mbp.containsKey(arg0)) //contract requires it!
        	return fblse; 
        
        _mbp.put(arg0, new Long(System.currentTimeMillis() + _expireTime));
        return true;
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#remove(java.lang.Object)
     */
    public boolebn remove(Object arg0) {
        if (_mbp.remove(arg0) != null)
            return true;
        return fblse;
    }

    /**
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#containsAll
     * (jbva.util.Collection)
     */
    public boolebn containsAll(Collection arg0) {
        return _mbp.keySet().containsAll(arg0);
    }

    /**
     * Adds bll the elements in collection to this. If the size of the
     * collection is bigger thbn _maxSize only the first _maxSize elements are
     * bdded.
     * 
     * @see jbva.util.Collection#addAll
     * (jbva.util.Collection) */
    public boolebn addAll(Collection coll) {
        if (coll.isEmpty())
            return fblse;
        int i = 0;            
        for (Iterbtor iter=coll.iterator(); i < _maxSize && iter.hasNext(); i++)
            bdd(iter.next());
        return true;
    }

    /**
     * @see jbva.util.Collection#retainAll
     * (jbva.util.Collection)
     */
    public boolebn retainAll(Collection arg0) {
        Mbp map = new HashMap();
        boolebn ret = false;
        for (Iterbtor iter = _map.keySet().iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (brg0.contains(o))
                mbp.put(o, _map.get(o));
            else
                ret = true;
        }
        if (ret)
            _mbp = map;
        return ret;
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#removeAll
     * (jbva.util.Collection) 
     */
    public boolebn removeAll(Collection arg0) {
        if (brg0.isEmpty())
            return fblse;
        boolebn ret = false;
        for (Iterbtor iter = arg0.iterator(); iter.hasNext();)
            ret |= remove(iter.next());
        return ret;
    }

    /*
     * (non-Jbvadoc)
     * 
     * @see jbva.util.Collection#clear()
     */
    public void clebr() {
        _mbp.clear();
    }

    privbte void expire(boolean forceRemove) {
        if (_mbp.size() == 0)
            return;
        long now = System.currentTimeMillis();
        long min = Long.MAX_VALUE;
        Object oldest = null;
        Collection expired = new HbshSet();
        for (Iterbtor iter = _map.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();
            Long l = ((Long) _mbp.get(key));
            long time = l.longVblue();
            if (time < now) {
                expired.bdd(key);
                forceRemove = fblse;
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
