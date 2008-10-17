package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import com.limegroup.gnutella.URN;

public interface UrnCallback {
 
    /** If urns.isEmpty() == true, the operation failed. */   
    public void urnsCalculated(File f, Set<? extends URN> urns);
    
    /** Determines if this callback is owned by the specified object. */
    public boolean isOwner(Object owner);
    
}