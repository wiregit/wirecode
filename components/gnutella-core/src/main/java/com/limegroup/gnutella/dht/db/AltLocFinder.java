package com.limegroup.gnutella.dht.db;

import org.limewire.io.GUID;
import org.limewire.mojito2.concurrent.DHTFuture;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;

/**
 * An AltLocFinder finds alternate locations for a urn or retrieves the 
 * alternate location for a {@link GUID}.
 */
public interface AltLocFinder {

    /**
     * Finds alternate locations for the given URN.
     * 
     * @param urn for the alternate location, must not be null
     */
    DHTFuture<AlternateLocation[]> findAltLocs(URN urn);
}