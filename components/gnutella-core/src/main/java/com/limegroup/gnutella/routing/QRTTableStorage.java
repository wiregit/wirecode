package com.limegroup.gnutella.routing;

import org.limewire.collection.BitField;

/**
 * Something that stores the hash values contained in a routing table.
 */
interface QRTTableStorage extends BitField {

    //// these will be removed soon as they're not relevant to
    //// all implementations.
    public int getUnusedUnits();
    public int getUnitsInUse();
    public int numUnitsWithLoad(int load);
    
    /**
     * @return % of units that are set.
     */
    public double getPercentFull();
    
    /**
     * sets the specified entry as present.
     */
    public void set(int hash);
    
    /**
     * clears the specified entry as absent
     */
    public void clear(int hash);
    
    /**
     * optional - compacts the memory representation of this.
     */
    public void compact();
    
    /**
     * @param newSize the new size we desire.
     * @return a new storage with the specified new size.  
     */
    public QRTTableStorage resize(int newSize);
    
    /**
     * @return a clone of this storage.
     */
    public QRTTableStorage clone();
    
    /**
     * sets all entries present in other to be present in this as well.
     */
    public void or(BitField other);
    
    /**
     * performs a xor with the other BitField.
     */
    public void xor(BitField other);
    
    
}
