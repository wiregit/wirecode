package com.limegroup.gnutella.altlocs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.URN;

public class AltLocManager {

    private static final AltLocManager INSTANCE = new AltLocManager();
    public static AltLocManager instance() {
        return INSTANCE;
    }
    
    /**
     * Map of the alternate location collections for each URN.
     * LOCKING: itself for all map operations as well as operations on the contained arrays
     */
    private final Map<URN, URNData> urnMap = Collections.synchronizedMap(new HashMap<URN, URNData>());
    
    private AltLocManager() {}
    
    /**
     * adds a given altloc to the manager
     * @return whether the manager already knew about this altloc
     */
    public boolean add(AlternateLocation al, Object source) {
        URN sha1 = al.getSHA1Urn();
        AlternateLocationCollection col = null;
        
        URNData data;
        synchronized(urnMap) {
            data = urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        
        synchronized(data) {    
            if (al instanceof DirectAltLoc) { 
                if (data.direct == AlternateLocationCollection.EMPTY)
                    data.direct = AlternateLocationCollection.create(sha1);
                col = data.direct;
            }
            else {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1) { 
                    if (data.push == AlternateLocationCollection.EMPTY)
                        data.push = AlternateLocationCollection.create(sha1);
                    col = data.push;
                } else { 
                    if (data.fwt == AlternateLocationCollection.EMPTY)
                        data.fwt = AlternateLocationCollection.create(sha1);
                    col = data.fwt;
                }
            }
        }
        
        boolean ret = col.add(al);
        
        // notify any listeners other than the source
        for(AltLocListener listener : data.getListeners()) {
            if (listener == source)
                continue;
            listener.locationAdded(al);
        }
        
        return ret;
    }
    
    /**
     * removes the given altloc (implementations may demote)
     */
    public boolean remove(AlternateLocation al, Object source) {
        URN sha1 = al.getSHA1Urn();
        URNData data = urnMap.get(sha1);
        if (data == null)
            return false;
        
        AlternateLocationCollection col;
        synchronized(data) {
            if (al instanceof DirectAltLoc) 
                col = data.direct;
            else {
                PushAltLoc push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1)
                    col = data.push;
                else
                    col = data.fwt;
            }
        }
        
        if (col == null)
            return false;
        
        boolean ret = col.remove(al);
        
        // if we emptied the current collection, see if the rest are empty as well
        if (!col.hasAlternateLocations())
            removeIfEmpty(sha1,data);
        
        return ret;
    }

    private void removeIfEmpty(URN sha1, URNData data) {
        boolean empty = false;
        synchronized(data) {
            if (!data.direct.hasAlternateLocations() &&
                    !data.push.hasAlternateLocations() &&
                    !data.fwt.hasAlternateLocations() &&
                    data.getListeners().isEmpty())
                empty = true;
        }
        
        if (empty)
            urnMap.remove(sha1);
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     */
    public AlternateLocationCollection getDirect(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return AlternateLocationCollection.EMPTY;
        
        synchronized(data) {
            return data.direct;
        }
    }
    
    /**
     * @param sha1 the URN for which to get altlocs
     * @param size the maximum number of altlocs to return
     * @param FWTOnly whether the altlocs must support FWT
     */
    public AlternateLocationCollection getPush(URN sha1, boolean FWTOnly) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return AlternateLocationCollection.EMPTY;
        
        synchronized(data) {
            return FWTOnly ? data.fwt : data.push;
        }
    }
    
    public void purge(){
        urnMap.clear();
    }
    
    public void purge(URN sha1) {
        urnMap.remove(sha1);
    }
    
    public boolean hasAltlocs(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return false;
        
        return data.hasAltLocs();
    }
    
    public int getNumLocs(URN sha1) {
        URNData data = urnMap.get(sha1);
        if (data == null)
            return 0;
        return data.getNumLocs();
    }
    
    public void addListener(URN sha1, AltLocListener listener) {
        URNData data; 
        synchronized(urnMap){
            data = urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        data.addListener(listener);
    }
    
    public void removeListener(URN sha1, AltLocListener listener) {
        URNData data =  urnMap.get(sha1);
        if (data == null)
            return;
        data.removeListener(listener);
        removeIfEmpty(sha1,data);
    }

    private static class URNData {
        /** 
         * The three alternate locations we keep with this urn.
         * LOCKING: this
         */
        public AlternateLocationCollection direct = AlternateLocationCollection.EMPTY;
        public AlternateLocationCollection push = AlternateLocationCollection.EMPTY;
        public AlternateLocationCollection fwt = AlternateLocationCollection.EMPTY;
        
        private volatile List<AltLocListener> listeners = Collections.emptyList();
        
        public synchronized boolean hasAltLocs() {
            return direct.hasAlternateLocations() || 
            push.hasAlternateLocations() || 
            fwt.hasAlternateLocations();
        }
        
        public synchronized int getNumLocs() {
            return direct.getAltLocsSize() + push.getAltLocsSize() + fwt.getAltLocsSize();
        }
        
        public synchronized void addListener(AltLocListener listener) {
            List<AltLocListener> updated = new ArrayList<AltLocListener>(listeners);
            updated.add(listener);
            listeners = updated;
        }
        
        public synchronized void removeListener(AltLocListener listener) {
            List<AltLocListener> updated = new ArrayList<AltLocListener>(listeners);
            updated.remove(listener);
            listeners = updated;
        }
        
        public List<AltLocListener> getListeners() {
            return listeners;
        }
    }
}
