package com.limegroup.gnutella;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** 
 * A factory to manage GuidMaps.
 * This will ensure that the guids are expired in an appropriate timeframe.
 */
class GuidMapFactory {
    
    /* The time which expired GUIDs will be purged. */
    private static long EXPIRE_POLL_TIME = 2 * 60 * 1000;
    
    /** A listing of all GuidMaps that have atleast one GUID that needs expiry. */
    private static List toExpire = new LinkedList();
    /** Whether or not we've scheduled our cleaner. */
    private static boolean scheduled = false;

    private GuidMapFactory() {};
    
    /**
     * Constructs a new GuidMap.
     * Returned GuidMaps will expire within 4 minutes of the expected expiry time.
     */
    public static GuidMap getMap() {
        return new GuidMapImpl();
    }
    
    /**
     * Removes a map from our accounting.
     * @param expiree
     */
    public static synchronized void removeMap(GuidMap expiree) {
        toExpire.remove(expiree);
    }

    /** Adds the GuidMapImpl to the list of maps that need to be expired. */
    private static synchronized void addMapToExpire(GuidMapImpl expiree) {
        // schedule it on demand
        if (!scheduled) {
            RouterService.schedule(new GuidExpirer(), 0, EXPIRE_POLL_TIME);
            scheduled = true;
        }
        toExpire.add(expiree);
    }
    
    /** Runnable that iterates through potential expirations and expires them. */
    private static class GuidExpirer implements Runnable {
        public void run() {
            synchronized (GuidMapFactory.class) {
                // iterator through all the maps....
                for(Iterator i = toExpire.iterator(); i.hasNext(); ) {
                    GuidMapImpl next = (GuidMapImpl) i.next();
                    synchronized (next) {
                        Map currMap = next.getMap();
                        // and expire as many entries as possible....
                        for(Iterator j = currMap.keySet().iterator(); j.hasNext(); ) {
                        	GUID.TimedGUID g = (GUID.TimedGUID) j.next();
                            if (g.shouldExpire())
                                j.remove();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Implementation of GuidMap that delegates to the factory to expire things.
     */
    private static class GuidMapImpl implements GuidMap {
        /** The default lifetime of the GUID (10 minutes). */
        private static long TIMED_GUID_LIFETIME = 10 * 60 * 1000;
        
        /** Mapping between new & old GUID.  Lazily constructed. */
        private Map map;
        
        public String toString() {
            return "impl, map: " + map;
        }
        
        /** Returns the mapping between the two GUIDs. */
        Map getMap() {
            return map;
        }

        /** Adds a mapping from origGUID to newGUID.  The default lifetime of 10 mintues is used. */
        public void addMapping(byte[] origGUID, byte[] newGUID) {
            addMapping(origGUID, newGUID, TIMED_GUID_LIFETIME);
        }
        
        public void addMapping(byte[] origGUID, byte[] newGUID, long lifetime) {
            boolean created = false;
            synchronized(this) {
                if(map == null) {
                    map = new HashMap();
                    created = true;
                }
                
                GUID.TimedGUID tGuid = new GUID.TimedGUID(new GUID(newGUID), lifetime);
                map.put(tGuid, new GUID(origGUID));
            }
            
            if(created)
                GuidMapFactory.addMapToExpire(this);
        }

        public synchronized byte[] getOriginalGUID(byte[] newGUID) {
            if(map != null) {
                GUID.TimedGUID wrapper = new GUID.TimedGUID(new GUID(newGUID), 0);
                GUID orig = (GUID) map.get(wrapper);
                if(orig != null)
                    return orig.bytes();
            }
            
            return null;
        }

        public synchronized GUID getNewGUID(GUID origGUID) {
            if(map != null) {
                for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                    Map.Entry next = (Map.Entry)i.next();
                    GUID val = (GUID) next.getValue();
                    if(val.equals(origGUID)) {
                    	GUID.TimedGUID key = (GUID.TimedGUID) next.getKey();
                        return key.getGUID();
                    }
                }
            }
            
            return null;
            
        }
        
    }
}