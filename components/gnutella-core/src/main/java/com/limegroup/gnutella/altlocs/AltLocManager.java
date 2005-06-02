package com.limegroup.gnutella.altlocs;

import java.util.ArrayList;
import java.util.Arrays;
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
    
    private static final int DIRECT = 0;
    private static final int PUSH = 1;
    private static final int FWT = 2;
    
    /**
     * Mapping of URN - > array of three alternate locations.
     * LOCKING: itself for all map operations as well as operations on the contained arrays
     */
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
            AlternateLocationCollection []cols = 
                (AlternateLocationCollection []) urnMap.get(sha1);
            
            if (cols == null) {
                cols = new AlternateLocationCollection[3];
                urnMap.put(sha1,col);
            }
            
            int type = -1;
            if (al instanceof DirectAltLoc) 
                type = DIRECT;
            else {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1) 
                    type = PUSH;
                else 
                    type = FWT;
            }
            
            if (cols[type] == null)
                cols[type] = AlternateLocationCollection.create(sha1);
            col = cols[type];
        }
        
        return col.add(al);
        
    }
    
    /**
     * removes the given altloc (implementations may demote)
     */
    public boolean remove(AlternateLocation al) {
        URN sha1 = al.getSHA1Urn();
        synchronized(urnMap) {
            AlternateLocationCollection [] cols = 
                (AlternateLocationCollection []) urnMap.get(sha1);
            if (cols == null)
                return false;
            
            int type = -1;
            if (al instanceof DirectAltLoc) 
                type = DIRECT;
            else {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1)
                    type = PUSH;
                else
                    type = FWT;
            }
            
            AlternateLocationCollection col = cols[type];
            
            boolean ret = col.remove(al);
            if (!col.hasAlternateLocations()) {
                cols[type] = null;
                if (cols[DIRECT] == null && cols[PUSH] == null && cols[FWT] == null)
                    urnMap.remove(sha1);
            }
            return ret;
        }
    }

    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     */
    public AlternateLocationCollection getDirect(URN sha1) {
        AlternateLocationCollection []cols = (AlternateLocationCollection []) urnMap.get(sha1);
        if (cols == null)
            return null;
        
        return cols[DIRECT];
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     * @param FWTOnly whether the altlocs must support FWT
     */
    public AlternateLocationCollection getPush(URN sha1, boolean FWTOnly) {
        AlternateLocationCollection []cols = (AlternateLocationCollection []) urnMap.get(sha1);
        if (cols == null)
            return null;
        
        return cols[FWTOnly ? FWT : PUSH];
    }
    
    public void purge(){
        urnMap.clear();
    }
    
    public void purge(URN sha1) {
        urnMap.remove(sha1);
    }
    
    public boolean hasAltlocs(URN sha1) {
        synchronized(urnMap) {
            AlternateLocationCollection []al = (AlternateLocationCollection[]) urnMap.get(sha1);
            if (al == null)
                return false;
            return ( al[DIRECT] != null && al[DIRECT].hasAlternateLocations() ) ||
                ( al[PUSH] != null && al[PUSH].hasAlternateLocations() ) ||
                ( al[FWT] != null && al[FWT].hasAlternateLocations() );
        }
    }

}
