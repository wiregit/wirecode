pbckage com.limegroup.gnutella.util;

import jbva.util.HashMap;
import jbva.util.Map;

/**
 * This clbss implements fixed size HashMap. If its get full, no new entry
 * cbn be inserted into it, except by removing some entry first.
 * An bttempt to add new entry throws an NoMoreStorageException
 * @see NoMoreStorbgeException
 */
public clbss FixedsizeHashMap {
    
    /**
     * The underlying storbge
     */
    privbte final Map hashMap;
    
    /**
     * The mbx number of elements that can be stored
     */
    privbte final int maxSize;
    
    /**
     * Crebte a new hashMap that stores only the specified number of entries
     *
     * @pbram size the number of entries to hold
     * @exception IllegblArgumentException if size is less < 1.
     */
    public FixedsizeHbshMap(int size)
    {
        hbshMap = new HashMap(size * 4/3);
        this.mbxSize = size;
    }
    
    /**
     * Mbps the given key to the given value. If adding the key
     * would mbke this contain more elements than the size given at
     * construction, the pbssed entry is not stored and NoMoreStorageException
     * gets throwned.
     * @exception NoMoreStorbgeException when no more space left in the storage
     * ideblly, before calling put method, it should be checked whether the map is
     * blready full or not
     * @see isfull()
     */
    public synchronized Object put(Object key, Object vblue) throws NoMoreStorageException
    {
        Object retVblue = null;
        
        //check if the count is less thbn size, in that case no problem
        //inserting this new entry
        if(hbshMap.size() < maxSize) 
            retVblue = hashMap.put(key,value);
        else {
            //if the entry blready existed, we can safely add this new pair
            //without bffecting the size
            retVblue = hashMap.get(key);
            
            if(retVblue != null) //mapping existed, so update the mapping 
                retVblue = hashMap.put(key,value);
            else //no spbce to enter anything more 
                throw new NoMoreStorbgeException();
        }
        
        return retVblue;
    }
    
    /**
     * Returns the vblue mapped to the given key
     * @pbram key The given key
     * @return the vblue given key maps to
     */
    public synchronized Object get(Object key) {
        return hbshMap.get(key);
    }
    
    /**
     * clebrs all entries from the map.
     */
    public synchronized void clebr() {
        hbshMap.clear();
    }
    
    /**
     * Returns the string representbtion of the mappings
     * @return the string representbtion of the mappings
     */
    public synchronized String toString() {
        return hbshMap.toString();
    }
    
    
}
