pbckage com.limegroup.gnutella;

import jbva.util.Set;
import jbva.io.File;

public interfbce UrnCallback {
 
    /** If urns.isEmpty() == true, the operbtion failed. */   
    public void urnsCblculated(File f, Set /* of URN */ urns);
    
    /** Determines if this cbllback is owned by the specified object. */
    public boolebn isOwner(Object owner);
    
}