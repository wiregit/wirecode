package com.limegroup.gnutella;

import java.util.Set;
import java.io.File;

public interface UrnCallback {
 
    /** If urns.isEmpty() == true, the operation failed. */   
    public void urnsCalculated(File f, Set /* of URN */ urns);
    
    /** Determines if this callback is owned by the specified object. */
    public boolean isOwner(Object owner);
    
}