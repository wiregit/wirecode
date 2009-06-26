package com.limegroup.gnutella.xml;

import com.limegroup.gnutella.library.FileDesc;

/** A simple interface for getting cached XML data or determining if XML data can be constructed. */
public interface XmlController {
    
    /**
     * Loads cached XML only.  Does not construct documents if there is nothing cached.
     * Returns true if this successfully loaded cached XML, false otherwise.
     */
    boolean loadCachedXml(FileDesc fd);
    
    /** Determines whether or not XML can be constructed for this file. */
    boolean canConstructXml(FileDesc fd);
    
    /** Loads new XML for a file. Returns true if anything loaded. */
    boolean loadXml(FileDesc fd);

}
