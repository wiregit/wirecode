package com.limegroup.gnutella.dht;

import java.net.InetAddress;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Version;

/**
 * 
 */
public interface AltLocDHTValue extends DHTValue {
    
    /**
     * DHTValueType for AltLocs
     */
    public static final DHTValueType ALT_LOC = DHTValueType.valueOf("ALOC");
    
    /**
     * Version of AltLocDHTValue
     */
    public static final Version VERSION = Version.valueOf(0, 0);
    
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
     * Returns the features of the AltLoc
     * 
     * Note: Returns only something meaningful if
     * this is a firewalled AltLoc!
     */
    public int getFeatures();
    
    /**
     * Returns the f2f version of the AltLoc
     * 
     * Note: Returns only something meaningful if
     * this is a firewalled AltLoc!
     */
    public int getFwtVersion();
    
    /**
     * Returns the InetAddress of the AltLoc
     * 
     * Note: Returns only something meaningful if
     * this is a firewalled AltLoc!
     */
    public InetAddress getInetAddress();
    
    /**
     * Returns the (Gnutella) Port number of the AltLoc's Push Proxy
     * who published this Value (an Ultrapeer)
     * 
     * Note: Returns only something meaningful if
     * this is a firewalled AltLoc!
     */
    public int getPushProxyPort();
    
    /**
     * Returns true if this AltLoc represents the localhost
     */
    public boolean isLocalAltLoc();
}
