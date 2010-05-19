package com.limegroup.gnutella.dht.db;

import org.limewire.io.GUID;
import org.limewire.mojito2.concurrent.DHTFuture;

import com.limegroup.gnutella.PushEndpoint;

/**
 * A unit that allows for blocking or asynchronous retrieval of 
 * {@link PushEndpoint push endpoints}.
 */
public interface PushEndpointService {

    /**
     * Performs a non-blocking lookup for a push endpoint.
     * @param guid the guid of the push endpoint to look up 
     * @param listener could be null, but is strongly discouraged again since
     * this notifies of the outcome of the lookup
     */
    DHTFuture<PushEndpoint> findPushEndpoint(GUID guid);
}
