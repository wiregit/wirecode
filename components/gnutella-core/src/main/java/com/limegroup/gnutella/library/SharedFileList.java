package com.limegroup.gnutella.library;

import com.limegroup.gnutella.URN;

public interface SharedFileList extends FileList {
    
    /**
     * Returns the <tt>FileDesc</tt> for the specified URN. This only returns
     * one <tt>FileDesc</tt>, even though multiple indices are possible.
     * 
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *         <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    FileDesc getFileDesc(URN urn);

}
