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

public class UrnCache {
    
    /**
     * UrnCache instance variable
     */
    private static UrnCache instance = null;

    /**
     * UrnCache container
     */
    private Hashtable /* String -> HashSet */ theUrnCache;

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
     * Find any URNs remembered from a previous session for a file
     */
    public HashSet getUrns(String path, long modTime) {
		HashSet urns = new HashSet();

        /** one or more "urn:" names for this file */

        // don't trust failed mod times
        if (modTime==0L) 
		    return urns; 
        
        HashSet cachedUrns = (HashSet)theUrnCache.get(modTime+" "+path);
        if(cachedUrns!=null) {
            Iterator iter = cachedUrns.iterator();
            while(iter.hasNext()){
                String urn = (String)iter.next();
                urns.add(urn);
            }
        } // else just leave urns empty for now
		return urns;
    }



    /**
     * Add URNs to theUrnCache
     */
    public void persistUrns(String path, long modTime, HashSet urns) {
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
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("fileurns.cache"));
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
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("fileurns.cache"));
            oos.writeObject(theUrnCache);
            oos.close();
        } catch (Exception e) {
            // no great loss
        }
    }
}


