package com.limegroup.gnutella.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A Set version of DualCollection.
 */
public class DualSet extends DualCollection implements Set {
   
    /**
     * Constructs a new DualSet.
     */ 
    public DualSet() {
        super();
    }
    
    /**
     * Constructs a new DualSet, backed by the two sets.
     */
    public DualSet(Set a, Set b) {
        super(a, b);
    }
    
    /**
     * Returns a new HashSet.
     */
    protected Collection createCollection() {
        return new HashSet();
    }
}
    