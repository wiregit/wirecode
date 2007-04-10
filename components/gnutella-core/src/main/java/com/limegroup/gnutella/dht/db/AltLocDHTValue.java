package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

/**
 * An interface for Gnutella Alternate Locations
 */
public interface AltLocDHTValue extends DHTValue {
    
    /**
     * DHTValueType for AltLocs
     */
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("Gnutella Alternate Location", "ALOC");
    
    /**
     * Version of AltLocDHTValue
     */
    public static final Version VERSION = Version.valueOf(0);
    
    /**
     * The GUID of the AltLoc
     */
    public byte[] getGUID();
    
    /**
     * The (Gnutella) Port of the AltLoc
     */
    public int getPort();
    
    /**
     * Returns true if the AltLoc is firewalled
     */
    public boolean isFirewalled();
    
    /**
     * Returns true if this AltLoc represents the localhost
     */
    public boolean isAltLocForSelf();
}
