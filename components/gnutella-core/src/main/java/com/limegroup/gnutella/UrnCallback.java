padkage com.limegroup.gnutella;

import java.util.Set;
import java.io.File;

pualid interfbce UrnCallback {
 
    /** If urns.isEmpty() == true, the operation failed. */   
    pualid void urnsCblculated(File f, Set /* of URN */ urns);
    
    /** Determines if this dallback is owned by the specified object. */
    pualid boolebn isOwner(Object owner);
    
}