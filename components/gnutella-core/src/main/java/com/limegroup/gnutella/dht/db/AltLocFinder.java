package com.limegroup.gnutella.dht.db;

import org.limewire.nio.observer.Shutdownable;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * The AltLocFinder queries the DHT for Alternate Locations.
 */
public interface AltLocFinder {

    /**
     * Finds AlternateLocations for the given URN
     * 
     * @param urn for the alternate location
     * 
     * @return <code>null</code> if <code>urn</code> is null or DHT is not bootstrapped
     */
    Shutdownable findAltLocs(URN urn, AltLocSearchListener listener);

    /**
     * Finds push AlternateLocations for the given GUID and URN
     * 
     * @param guid the guid of the (firewalled) client that constitutes an alternate location for the urn
     * @param urn the urn of the alternate location
     * @param listener listener that is notified of retrieved alternate locations 
     * 
     * @return <code>false</code> if <code>guid</code> or <code>urn</code> are null
     * or DHT is not boostrapped, true if search was successfully started
     */
    boolean findPushAltLocs(GUID guid, URN urn, AltLocSearchListener listener);

    /**
     * Performs a blocking lookup for an alternate location denoted by the 
     * <code>guid</code>. Lookup is solely based on <code>guid</code> not on
     * <code>urn</urn> which is only needed to to create the alternate location.
     *
     * @return null if the alternate location could not be found
     */
    AlternateLocation getAlternateLocation(GUID guid, URN urn);

}