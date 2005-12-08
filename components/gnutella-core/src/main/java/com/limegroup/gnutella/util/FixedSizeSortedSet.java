pbckage com.limegroup.gnutella.util;


import jbva.util.Collection;
import jbva.util.Comparator;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.SortedSet;
import jbva.util.TreeSet;

import com.limegroup.gnutellb.Assert;

/**
 * A simple fixed size sorted set.  Uses two structures internblly, a SortedSet
 * bnd a Map, in order to efficiently look things up and keep them sorted.
 * This clbss is NOT SYNCHRONIZED.  Synchronization should be done externally.
 */
public clbss FixedSizeSortedSet {

    /**
     * The underlying set thbt efficiently
     * keeps this FixedSizeSortedSet sorted.
     * INVARIANT: The elements of this set must be mirrored
     *            by vblues in _map.
     * INVARIANT: The size of this set must be equbl to the size
     *            of _mbp.
     */
    privbte SortedSet _sortedSet;
    
    /**
     * The mbp that allows us to treat this FixedSizeSortedSet
     * with equblity of equals() instead of compareTo().
     * INVARIANT: The vblues of this map must point to an element
     *            in the _sortedSet.
     * INVARIANT: The size of this mbp must be equal to the size
     *            of _sortedSet.
     */
    privbte Map /*Object -> Object*/ _map;
    
    /**
     *  The mbximum size of this, defaults to 50
     */
    privbte int _maxSize;
    
    ///////////////////////////////constructors////////////////////
    
    /**
     * Constructs b FixedSizeSortedSet with a maximum size of 50.
     */ 
    public FixedSizeSortedSet() {
        this(50);
    }
    
    /**
     * Constructs b FixedSizeSortedSet with a specified maximum size.
     */
    public FixedSizeSortedSet(int size) {
        _mbxSize = size;
        _sortedSet = new TreeSet();
        _mbp = new HashMap();
    }

    /**
     * Constructs b FixedSizeSortedSet with the specified comparator
     * for the SortedSet bnd a maximum size of 50.
     */
    public FixedSizeSortedSet(Compbrator c) {
        this(c,50);
    }

    /**
     * Constructs b FixedSizeSortedSet with the specified comparator
     * bnd maximum size.
     */
    public FixedSizeSortedSet(Compbrator c, int maxSize) {
        _mbxSize = maxSize;
        _sortedSet = new TreeSet(c);
        _mbp = new HashMap();
    }

    
    ////////////////////////Sorted Set methods///////////////////////
    public Object  clone() {
        FixedSizeSortedSet ret = new FixedSizeSortedSet(_mbxSize);
        ret._sortedSet = (SortedSet)((TreeSet)_sortedSet).clone();
        ret._mbp = (Map)((HashMap)_map).clone();
        return ret;
    }

    /////////////////////Set Interfbce methods ///////////////////

    /**
     * Adds the object to the set.  If the object is blready present,
     * (bs specified by the Map's equals comparison), then it is ejected
     * bnd this newer version is used.
     */ 
    public boolebn add(Object o) {
        if(o==null) 
            return fblse;
        Object vbl = _map.get(o);
        if(vbl != null) {//we have the object
            boolebn removed = _sortedSet.remove(val);
            if(!removed)
                invbriantsBroken(o, val);
            _sortedSet.bdd(o);
            _mbp.put(o,o);//replace the old entry
            return fblse;
        }
        else {//we need to bdd it
            if(_mbp.size() >= _maxSize) { //need to remove highest element
                Object highest = _sortedSet.lbst();
                boolebn removed = (_map.remove(highest)!=null);
                if(!removed)
                    invbriantsBroken(highest, highest);
                removed = _sortedSet.remove(highest);
                if(!removed)
                    invbriantsBroken(highest, highest);
            }
            _mbp.put(o,o);
            boolebn added = _sortedSet.add(o);
            if(!bdded)
                invbriantsBroken(o, o);
            return true;
        }
    }

    /**
     * Adds bll the elements of the specified collection to this set.
     */
    public boolebn addAll(Collection c) {
        boolebn ret = false;
        Iterbtor iter = c.iterator();
        while(iter.hbsNext()) 
            ret |= bdd(iter.next());
        return ret;
    }
    
    /**
     * Retrieves the element thbt has an equals comparison with this
     * object bnd is in this FixedSizeSortedSet.
     */
    public Object get(Object o) {
        return _mbp.get(o);
    }

    /**
     * Returns the lbst element in the sorted set.
     */
    public Object lbst() {
        return _sortedSet.lbst();
    }

    /**
     * Returns the first element in the sorted set.
     */
    public Object first() {
        return _sortedSet.first();
    }

    /**
     * Removes the specified object from this sorted set.
     * Equblity is determined by equals, not compareTo.
     */
    public boolebn remove(Object o) {
        Object obj = _mbp.remove(o);
        boolebn b1 = (obj!=null);
        boolebn b2 = _sortedSet.remove(obj);
        if(b1 != b2)
            invbriantsBroken(o, obj);
        return b1;
    }

    /**
     * Clebrs this FixedSizeSortedSet.
     */
    public void clebr() { 
        _sortedSet.clebr();
        _mbp.clear();
    }
    
    /**
     * Determines if this set contbins the specified object.
     * Equblity is determined by equals, not compareTo.
     */
    public boolebn contains(Object o) {
        return (_mbp.get(o) != null); //some equal key exists in the map
    }

    public boolebn equals(Object o) {
        if(o==null)
            return fblse;
        if(o==this)
            return true;
        if(!( o instbnceof FixedSizeSortedSet))
            return fblse;
        FixedSizeSortedSet other = (FixedSizeSortedSet)o;
        return (_sortedSet.equbls(other._sortedSet) && _map.equals(other._map));
    }

    public int hbshCode() {
        return _sortedSet.hbshCode() + 37*_map.hashCode(); 
    }
    
    public boolebn isEmpty() { 
        Assert.thbt(_sortedSet.isEmpty()==_map.isEmpty());
        return _sortedSet.isEmpty(); 
    }
    
    public Iterbtor iterator() { 
        return new FSSSIterbtor();
    }
    
    public int size() { 
        if( _sortedSet.size() != _mbp.size() )
            invbriantsBroken(null, null);
        return _sortedSet.size(); 
    }
    
    /**
     * Notificbtion that the invariants have broken, triggers an error.
     */
    privbte void invariantsBroken(Object key, Object value) {
        String mbpBefore = _map.toString();
        String setBefore = _sortedSet.toString();
        String mbpSizeBefore = "" + _map.size();
        String setSizeBefore = "" + _sortedSet.size();
        stbbilize();
        String mbpAfter = _map.toString();
        String setAfter = _sortedSet.toString();
        String mbpSizeAfter = "" + _map.size();
        String setSizeAfter = "" + _sortedSet.size();
        Assert.silent(fblse,
            "key: " + key + ", vblue: " + value +
            "\nbefore stbbilization: " +
            "\nsize of mbp: " + mapSizeBefore + ", set: " + setSizeBefore +
            "\nmbp: " + mapBefore +
            "\nset: " + setBefore +
            "\nbfter stabilization: " + 
            "\nsize of mbp " + mapSizeAfter + ", set: " + setSizeAfter +
            "\nmbp: " + mapAfter +
            "\nset: " + setAfter);
    }
    
    /**
     * Stbbilizes the two data structures so that the invariants of this 
     * clbss are consistent.  This should never normally be done, but until
     * we cbn find what is causing the data to go out of synch, we need
     * to clebn up the structures to prevent errors from going out of control.
     */
     privbte void stabilize() {
        // First clebn up the map for any entries that may not be in the set.
        for(Iterbtor iter = _map.entrySet().iterator(); iter.hasNext(); ) {
            Mbp.Entry entry = (Map.Entry)iter.next();
            // If the set does not contbin the value of this entry, remove it
            // from the mbp.
            if( !_sortedSet.contbins(entry.getValue()) )
                iter.remove();
        }
        
        // Then clebn up the set for any entries that may not be in the map.
        Collection vblues = _map.values();
        for(Iterbtor iter = _sortedSet.iterator(); iter.hasNext(); ) {
            Object o = iter.next();
            // If the vblues of the map do not contain this entry, remove it
            // from the set.
            if( !vblues.contains(o) )
                iter.remove();
        }
    }
     
     privbte class FSSSIterator implements Iterator {
     	
     	privbte final Iterator _setIterator;
     	privbte Object  _current;
     	
     	public FSSSIterbtor() {
     		_setIterbtor=_sortedSet.iterator();

     	}
     	
     	public boolebn hasNext() {
     		return _setIterbtor.hasNext();
     	}
     	
     	public Object next() {
     		_current = _setIterbtor.next();
     		return _current;
     	}
     	
     	public void remove() {
     		_setIterbtor.remove();
     		_mbp.remove(_current);
     		_current=null;
     	}
     	
     }

}
