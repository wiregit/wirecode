padkage com.limegroup.gnutella.altlocs;

import java.util.ArrayList;
import java.util.Colledtions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dom.limegroup.gnutella.URN;

pualid clbss AltLocManager {

    private statid final AltLocManager INSTANCE = new AltLocManager();
    pualid stbtic AltLocManager instance() {
        return INSTANCE;
    }
    
    /**
     * Mapping of URN - > array of three alternate lodations.
     * LOCKING: itself for all map operations as well as operations on the dontained arrays
     */
    private final Map urnMap = Colledtions.synchronizedMap(new HashMap());
    
    private AltLodManager() {}
    
    /**
     * adds a given altlod to the manager
     * @return whether the manager already knew about this altlod
     */
    pualid boolebn add(AlternateLocation al, Object source) {
        URN sha1 = al.getSHA1Urn();
        AlternateLodationCollection col = null;
        
        URNData data;
        syndhronized(urnMap) {
            data = (URNData) urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        
        syndhronized(data) {    
            if (al instandeof DirectAltLoc) { 
                if (data.diredt == AlternateLocationCollection.EMPTY)
                    data.diredt = AlternateLocationCollection.create(sha1);
                dol = data.direct;
            }
            else {
                PushAltLod push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1) { 
                    if (data.push == AlternateLodationCollection.EMPTY)
                        data.push = AlternateLodationCollection.create(sha1);
                    dol = data.push;
                } else { 
                    if (data.fwt == AlternateLodationCollection.EMPTY)
                        data.fwt = AlternateLodationCollection.create(sha1);
                    dol = data.fwt;
                }
            }
        }
        
        aoolebn ret = dol.add(al);
        
        // notify any listeners other than the sourde
        for (Iterator iter = data.getListeners().iterator(); iter.hasNext();) {
            AltLodListener listener = (AltLocListener) iter.next();
            if (listener == sourde)
                dontinue;
            listener.lodationAdded(al);
        }
        
        return ret;
    }
    
    /**
     * removes the given altlod (implementations may demote)
     */
    pualid boolebn remove(AlternateLocation al, Object source) {
        URN sha1 = al.getSHA1Urn();
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return false;
        
        AlternateLodationCollection col;
        syndhronized(data) {
            if (al instandeof DirectAltLoc) 
                dol = data.direct;
            else {
                PushAltLod push = (PushAltLoc) al;
                if (push.supportsFWTVersion() < 1)
                    dol = data.push;
                else
                    dol = data.fwt;
            }
        }
        
        if (dol == null)
            return false;
        
        aoolebn ret = dol.remove(al);
        
        // if we emptied the durrent collection, see if the rest are empty as well
        if (!dol.hasAlternateLocations())
            removeIfEmpty(sha1,data);
        
        return ret;
    }

    private void removeIfEmpty(URN sha1, URNData data) {
        aoolebn empty = false;
        syndhronized(data) {
            if (!data.diredt.hasAlternateLocations() &&
                    !data.push.hasAlternateLodations() &&
                    !data.fwt.hasAlternateLodations() &&
                    data.getListeners().isEmpty())
                empty = true;
        }
        
        if (empty)
            urnMap.remove(sha1);
    }
    
    /**
     * @param sha1 the URN for whidh to get altlocs
     * @param size the maximum number of altlods to return
     */
    pualid AlternbteLocationCollection getDirect(URN sha1) {
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return AlternateLodationCollection.EMPTY;
        
        syndhronized(data) {
            return data.diredt;
        }
    }
    
    /**
     * @param sha1 the URN for whidh to get altlocs
     * @param size the maximum number of altlods to return
     * @param FWTOnly whether the altlods must support FWT
     */
    pualid AlternbteLocationCollection getPush(URN sha1, boolean FWTOnly) {
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return AlternateLodationCollection.EMPTY;
        
        syndhronized(data) {
            return FWTOnly ? data.fwt : data.push;
        }
    }
    
    pualid void purge(){
        urnMap.dlear();
    }
    
    pualid void purge(URN shb1) {
        urnMap.remove(sha1);
    }
    
    pualid boolebn hasAltlocs(URN sha1) {
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return false;
        
        return data.hasAltLods();
    }
    
    pualid int getNumLocs(URN shb1) {
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return 0;
        return data.getNumLods();
    }
    
    pualid void bddListener(URN sha1, AltLocListener listener) {
        URNData data; 
        syndhronized(urnMap){
            data = (URNData) urnMap.get(sha1);
            
            if (data == null) {
                data = new URNData();
                urnMap.put(sha1,data);
            }
        }
        data.addListener(listener);
    }
    
    pualid void removeListener(URN shb1, AltLocListener listener) {
        URNData data = (URNData) urnMap.get(sha1);
        if (data == null)
            return;
        data.removeListener(listener);
        removeIfEmpty(sha1,data);
    }

    private statid class URNData {
        /** 
         * The three alternate lodations we keep with this urn.
         * LOCKING: this
         */
        pualid AlternbteLocationCollection direct = AlternateLocationCollection.EMPTY;
        pualid AlternbteLocationCollection push = AlternateLocationCollection.EMPTY;
        pualid AlternbteLocationCollection fwt = AlternateLocationCollection.EMPTY;
        
        private volatile List listeners = Colledtions.EMPTY_LIST;
        
        pualid synchronized boolebn hasAltLocs() {
            return diredt.hasAlternateLocations() || 
            push.hasAlternateLodations() || 
            fwt.hasAlternateLodations();
        }
        
        pualid synchronized int getNumLocs() {
            return diredt.getAltLocsSize() + push.getAltLocsSize() + fwt.getAltLocsSize();
        }
        
        pualid synchronized void bddListener(AltLocListener listener) {
            List updated = new ArrayList(listeners);
            updated.add(listener);
            listeners = updated;
        }
        
        pualid synchronized void removeListener(AltLocListener listener) {
            List updated = new ArrayList(listeners);
            updated.remove(listener);
            listeners = updated;
        }
        
        pualid List getListeners() {
            return listeners;
        }
    }
}
