/**
 * This class contains a systemwide URN cache that persists file URNs (hashes)
 * across sessions.
 *
 * Modified by Gordon Mohr (2002/02/19): Added URN storage, calculation, caching
 * Repackaged by Greg Bildson (2002/02/19): Moved to dedicated class.
 */

package com.limegroup.gnutella;

import com.bitzi.util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.security.*;
import java.util.Enumeration;

public final class UrnCache {
    
    /**
     * File where urns (currently SHA1 urns) get persisted to
     */
    private static final String URN_CACHE_FILE = "fileurns.cache";

    /**
     * UrnCache instance variable
     */
    private static UrnCache instance = null;

    /**
     * UrnCache container
     */
    private Map /* String -> HashSet */ theUrnCache;

    /**
	 * Returns the <tt>UrnCache</tt> instance.
	 *
	 * @return the <tt>UrnCache</tt> instance
     */
    public static UrnCache instance() {
		if (instance == null) {
			instance = new UrnCache();
		}
        return instance;
    }

    /**
     *  Create and initialize urn cache
     */
    private UrnCache() {
        initCache();
	}
    
    /**
     * Find any URNs remembered from a previous session for the specified
	 * <tt>File</tt> instance.  The returned <tt>HashSet</tt> is
	 * guaranteed to be non-null, but it may be empty.
	 *
	 * @param file the <tt>File</tt> instance to look up URNs for
	 * @return a new <tt>HashSet</tt> containing any cached URNs for the
	 *  speficied <tt>File</tt> instance, guaranteed to be non-null, but
	 *  possibly empty
     */
    public Collection getUrns(File file) {
		long modTime = file.lastModified();
		String path = file.getAbsolutePath();
		Collection urns = new HashSet();

        /** one or more "urn:" names for this file */

        // don't trust failed mod times
        if (modTime==0L) 
		    return urns; 
        
        Collection cachedUrns = (Collection)theUrnCache.get(modTime+" "+path);
        if(cachedUrns!=null) {
            Iterator iter = cachedUrns.iterator();
            while(iter.hasNext()){
                URN urn = (URN)iter.next();
                urns.add(urn);
            }
        } // else just leave urns empty for now
		return urns;
    }



    /**
     * Add URNs for the specified <tt>FileDesc</tt> instance to theUrnCache.
	 *
	 * @param fileDesc the <tt>FileDesc</tt> instance containing URNs to store
     */
    public void persistUrns(FileDesc fileDesc) {
		File file = fileDesc.getFile();
		Collection urns = fileDesc.getUrns();
		long modTime = file.lastModified();
		String path = file.getAbsolutePath();
        theUrnCache.put(modTime+" "+path, urns);
    }
    
    //
    // UrnCache Management
    //
    
    /**
     * load values from cache file, if available
     */
    private void initCache() {
        try {
            ObjectInputStream ois = 
			    new ObjectInputStream(new FileInputStream(URN_CACHE_FILE));
            theUrnCache = (Hashtable)ois.readObject();
            ois.close();
        } catch (Exception e) {
            // lack of cache is non-fatal
        } 
        if (theUrnCache == null) {
            theUrnCache = new Hashtable();
            return;
        }
        // discard outdated info
        Iterator iter = theUrnCache.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String)iter.next();
            long modTime=Long.parseLong(key.substring(0,key.indexOf(' ')));
            String path=key.substring(key.indexOf(' ')+1);
            // check to see if file still exists unmodified
            File f = new File(path);
            if (!f.exists()||f.lastModified()!=modTime) {
                iter.remove();
            }
        }
    }
    
    /**
     * write cache to disk to save recalc time later
     */
    public void persistCache() {
        try {
            ObjectOutputStream oos = 
			    new ObjectOutputStream(new FileOutputStream(URN_CACHE_FILE));
            oos.writeObject(theUrnCache);
            oos.close();
        } catch (Exception e) {
            // no great loss
        }
    }
}





