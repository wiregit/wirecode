package com.limegroup.gnutella.altlocs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;

/**
 * The manager of the alternate locations we send and receive from the mesh,
 * this class is responsible to make sure that LimeWire keeps it's
 * representation of the mesh current. Internally this class maintains two
 * AlternateLocationCollections, and removes items from one and adds it to the
 * other.  
 * LOCKING: while iterating over _validLAltLocs or _failedAltLocs we need to 
 * obtain their monitors.
 */
public class AltLocCollectionsManager {
    
    /**The AlternateLocationCollection that contains AltLocs that have  worked*/
    private AlternateLocationCollection _validAltLocs; 

    /** The AlternateLocationsCollection that should be removed from the mesh*/
    private AlternateLocationCollection _failedAltLocs;    
    
    public AltLocCollectionsManager(AlternateLocationCollection v, 
                                   AlternateLocationCollection f) {
        if(!v.getSHA1Urn().equals(f.getSHA1Urn()))
            throw new IllegalArgumentException("Collections do not match");
        _validAltLocs = v;
        _failedAltLocs = f;
    }
    
    /**
     * Adds alt to _validAltLocs if alt is not already in _failedAltLocs
     */
    public boolean addLocation(AlternateLocation alt) {
        if(_failedAltLocs.contains(alt))
            return false;
        return _validAltLocs.add(alt);        
    }
    
    /**
     * @param failed if true we will add alt to _failedAltLoc otherwise just
     * remove from _valid 
     */
    public boolean removeLocation(AlternateLocation alt, boolean failed) {
        boolean ret = _validAltLocs.remove(alt);
        if(failed) 
            _failedAltLocs.add(alt);
        return ret;
    }
    
    /**
     * @return true if alt was removed from _validAltLocs because of failure
     * false otherwise.
     */
    public boolean wasRemoved(AlternateLocation alt) {
        return _failedAltLocs.contains(alt);
    }
     
    /**
     * Adds all the elements in _validAltLocs
     */ 
    public int addAll(AlternateLocationCollection collection) {
        return _validAltLocs.addAll(collection);
    }
    
    //Sumeet:TODO1: add a diffAlternateLocationCollection method??
    
}

