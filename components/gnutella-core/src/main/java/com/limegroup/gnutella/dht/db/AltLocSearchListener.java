package com.limegroup.gnutella.dht.db;

import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * Listener interface that is notified of DHT lookups performed by the
 * {@link AltLocFinder}.
 * 
 * There is no guaranteed order between calls to {@link #handleAlternateLocation(AlternateLocation)}
 * and {@link #handleAltLocSearchDone(boolean)} and both can be called repeatedly.
 */
public interface AltLocSearchListener {
    
    /**
     * Called when an alternate location was found.
     */
    void handleAlternateLocation(AlternateLocation alternateLocation);
    
    /**
     * Is called when a lookup for an alternate location has been performed, any
     * result has been returned or an exception occurred during lookup.
     *
     * Note: This could just mean the lookup of a firewalled location without its
     * pushproxies has taken place.
     * 
     * @param success whether or not the lookup was successful
     */
    void handleAltLocSearchDone(boolean success);
}
