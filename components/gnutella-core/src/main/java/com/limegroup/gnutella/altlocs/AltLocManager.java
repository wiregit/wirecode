package com.limegroup.gnutella.altlocs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.URN;

public class AltLocManager {

    private static final AltLocManager INSTANCE = new AltLocManager();
    public static AltLocManager instance() {
        return INSTANCE;
    }
    
    private final Map urnMap = Collections.synchronizedMap(new HashMap());
    private AltLocManager() {}
    
    /**
     * adds a given altloc to the manager
     * @return whether the manager already knew about this altloc
     */
    public boolean add(AlternateLocation al){
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection col = null;
        
        synchronized(urnMap) {
            col = (AlternateLocationCollection) urnMap.get(sha1);
            
            if (col == null) {
                col = AlternateLocationCollection.create(sha1);
                urnMap.put(sha1,col);
            }
        }
        
        return col.add(al);
        
    }
    
    /**
     * removes the given altloc (implementations may demote)
     */
    public boolean remove(AlternateLocation al) {
        URN sha1 = al.getSHA1Urn();
        synchronized(urnMap) {
            AlternateLocationCollection col = (AlternateLocationCollection) urnMap.get(sha1);
            if (col == null)
                return false;
            
            boolean ret = col.remove(al);
            if (!col.hasAlternateLocations())
                urnMap.remove(sha1);
            return ret;
        }
    }

    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     */
    public Collection getDirect(URN sha1, int size) {
        AlternateLocationCollection col = (AlternateLocationCollection) urnMap.get(sha1);
        if (col == null)
            return null;
        
        if (size == -1)
            size = col.getAltLocsSize();
        List ret = new ArrayList(size);
        
        for (Iterator iter = col.iterator(); iter.hasNext() && size > 0;) {
            AlternateLocation current = (AlternateLocation) iter.next();
            if (current instanceof DirectAltLoc) {
                ret.add(current);
                size--;
            }
        }
        
        return ret;
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     * @param FWTOnly whether the altlocs must support FWT
     */
    public Collection getPush(URN sha1, int size, boolean FWTOnly) {
        AlternateLocationCollection col = (AlternateLocationCollection) urnMap.get(sha1);
        if (col == null)
            return null;
        
        if (size == -1)
            size = col.getAltLocsSize();
        List ret = new ArrayList(size);
        
        for (Iterator iter = col.iterator(); iter.hasNext() && size > 0;) {
            AlternateLocation current = (AlternateLocation) iter.next();
            if (current instanceof PushAltLoc) {
                if (FWTOnly) {
                    PushAltLoc push = (PushAltLoc) current;
                    if (push.supportsFWTVersion() == 0)
                        continue;
                }
                ret.add(current);
                size--;
            }
        }
        
        return ret;
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     * @param FWTOnly whether the altlocs must support FWT
     * @return a Collection of direct altlocs at index [0], and Collection of push 
     * altlocs at index [1]
     */
    public Collection[] getBoth(URN sha1, int size, boolean FWTOnly) {
        AlternateLocationCollection col = (AlternateLocationCollection) urnMap.get(sha1);
        if (col == null)
            return null;
        
        if (size == -1)
            size = col.getAltLocsSize();
        List direct = new ArrayList(size);
        List push = new ArrayList(size);
        
        for (Iterator iter = col.iterator(); iter.hasNext();) {
            if (direct.size() == size && push.size() == size) 
                break;
            
            AlternateLocation current = (AlternateLocation) iter.next();
            
            if (current instanceof DirectAltLoc && direct.size() < size)
                direct.add(current);
            if (current instanceof PushAltLoc && push.size() < size) {
                if (FWTOnly) {
                    PushAltLoc palt = (PushAltLoc) current;
                    if (palt.supportsFWTVersion() == 0)
                        continue;
                }
                push.add(current);
            }
        }
        
        return new Collection[]{direct,push};
    }
    
    public void purge(){
        urnMap.clear();
    }
    
    public void purge(URN sha1) {
        urnMap.remove(sha1);
    }
    
    public boolean hasAltlocs(URN sha1) {
        AlternateLocationCollection al = (AlternateLocationCollection) urnMap.get(sha1);
        return al != null && al.hasAlternateLocations();
    }

}
