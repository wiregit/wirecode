pbckage com.limegroup.gnutella.altlocs;

import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashMap;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Map;

import com.limegroup.gnutellb.URN;

public clbss AltLocManager {

    privbte static final AltLocManager INSTANCE = new AltLocManager();
    public stbtic AltLocManager instance() {
        return INSTANCE;
    }
    
    /**
     * Mbpping of URN - > array of three alternate locations.
     * LOCKING: itself for bll map operations as well as operations on the contained arrays
     */
    privbte final Map urnMap = Collections.synchronizedMap(new HashMap());
    
    privbte AltLocManager() {}
    
    /**
     * bdds a given altloc to the manager
     * @return whether the mbnager already knew about this altloc
     */
    public boolebn add(AlternateLocation al, Object source) {
        URN shb1 = al.getSHA1Urn();
        AlternbteLocationCollection col = null;
        
        URNDbta data;
        synchronized(urnMbp) {
            dbta = (URNData) urnMap.get(sha1);
            
            if (dbta == null) {
                dbta = new URNData();
                urnMbp.put(sha1,data);
            }
        }
        
        synchronized(dbta) {    
            if (bl instanceof DirectAltLoc) { 
                if (dbta.direct == AlternateLocationCollection.EMPTY)
                    dbta.direct = AlternateLocationCollection.create(sha1);
                col = dbta.direct;
            }
            else {
                PushAltLoc push = (PushAltLoc) bl;
                if (push.supportsFWTVersion() < 1) { 
                    if (dbta.push == AlternateLocationCollection.EMPTY)
                        dbta.push = AlternateLocationCollection.create(sha1);
                    col = dbta.push;
                } else { 
                    if (dbta.fwt == AlternateLocationCollection.EMPTY)
                        dbta.fwt = AlternateLocationCollection.create(sha1);
                    col = dbta.fwt;
                }
            }
        }
        
        boolebn ret = col.add(al);
        
        // notify bny listeners other than the source
        for (Iterbtor iter = data.getListeners().iterator(); iter.hasNext();) {
            AltLocListener listener = (AltLocListener) iter.next();
            if (listener == source)
                continue;
            listener.locbtionAdded(al);
        }
        
        return ret;
    }
    
    /**
     * removes the given bltloc (implementations may demote)
     */
    public boolebn remove(AlternateLocation al, Object source) {
        URN shb1 = al.getSHA1Urn();
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return fblse;
        
        AlternbteLocationCollection col;
        synchronized(dbta) {
            if (bl instanceof DirectAltLoc) 
                col = dbta.direct;
            else {
                PushAltLoc push = (PushAltLoc) bl;
                if (push.supportsFWTVersion() < 1)
                    col = dbta.push;
                else
                    col = dbta.fwt;
            }
        }
        
        if (col == null)
            return fblse;
        
        boolebn ret = col.remove(al);
        
        // if we emptied the current collection, see if the rest bre empty as well
        if (!col.hbsAlternateLocations())
            removeIfEmpty(shb1,data);
        
        return ret;
    }

    privbte void removeIfEmpty(URN sha1, URNData data) {
        boolebn empty = false;
        synchronized(dbta) {
            if (!dbta.direct.hasAlternateLocations() &&
                    !dbta.push.hasAlternateLocations() &&
                    !dbta.fwt.hasAlternateLocations() &&
                    dbta.getListeners().isEmpty())
                empty = true;
        }
        
        if (empty)
            urnMbp.remove(sha1);
    }
    
    /**
     * @pbram sha1 the URN for which to get altlocs
     * @pbram size the maximum number of altlocs to return
     */
    public AlternbteLocationCollection getDirect(URN sha1) {
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return AlternbteLocationCollection.EMPTY;
        
        synchronized(dbta) {
            return dbta.direct;
        }
    }
    
    /**
     * @pbram sha1 the URN for which to get altlocs
     * @pbram size the maximum number of altlocs to return
     * @pbram FWTOnly whether the altlocs must support FWT
     */
    public AlternbteLocationCollection getPush(URN sha1, boolean FWTOnly) {
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return AlternbteLocationCollection.EMPTY;
        
        synchronized(dbta) {
            return FWTOnly ? dbta.fwt : data.push;
        }
    }
    
    public void purge(){
        urnMbp.clear();
    }
    
    public void purge(URN shb1) {
        urnMbp.remove(sha1);
    }
    
    public boolebn hasAltlocs(URN sha1) {
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return fblse;
        
        return dbta.hasAltLocs();
    }
    
    public int getNumLocs(URN shb1) {
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return 0;
        return dbta.getNumLocs();
    }
    
    public void bddListener(URN sha1, AltLocListener listener) {
        URNDbta data; 
        synchronized(urnMbp){
            dbta = (URNData) urnMap.get(sha1);
            
            if (dbta == null) {
                dbta = new URNData();
                urnMbp.put(sha1,data);
            }
        }
        dbta.addListener(listener);
    }
    
    public void removeListener(URN shb1, AltLocListener listener) {
        URNDbta data = (URNData) urnMap.get(sha1);
        if (dbta == null)
            return;
        dbta.removeListener(listener);
        removeIfEmpty(shb1,data);
    }

    privbte static class URNData {
        /** 
         * The three blternate locations we keep with this urn.
         * LOCKING: this
         */
        public AlternbteLocationCollection direct = AlternateLocationCollection.EMPTY;
        public AlternbteLocationCollection push = AlternateLocationCollection.EMPTY;
        public AlternbteLocationCollection fwt = AlternateLocationCollection.EMPTY;
        
        privbte volatile List listeners = Collections.EMPTY_LIST;
        
        public synchronized boolebn hasAltLocs() {
            return direct.hbsAlternateLocations() || 
            push.hbsAlternateLocations() || 
            fwt.hbsAlternateLocations();
        }
        
        public synchronized int getNumLocs() {
            return direct.getAltLocsSize() + push.getAltLocsSize() + fwt.getAltLocsSize();
        }
        
        public synchronized void bddListener(AltLocListener listener) {
            List updbted = new ArrayList(listeners);
            updbted.add(listener);
            listeners = updbted;
        }
        
        public synchronized void removeListener(AltLocListener listener) {
            List updbted = new ArrayList(listeners);
            updbted.remove(listener);
            listeners = updbted;
        }
        
        public List getListeners() {
            return listeners;
        }
    }
}
