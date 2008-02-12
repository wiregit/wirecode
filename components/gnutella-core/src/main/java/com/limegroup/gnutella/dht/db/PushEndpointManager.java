package com.limegroup.gnutella.dht.db;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointCache.CachedPushEndpoint;

/**
 * Transparently interfaces with {@link PushEndpointFinder} 
 * and {@link PushEndpointCache}.
 * <p>
 * It looks up {@link PushEndpoint push endpoints} in the cache and if it
 * can't find a good instance there decides if it asks the finder to look up
 * the push endpoint.
 */
@Singleton
public class PushEndpointManager implements PushEndpointService {

    private final PushEndpointCache pushEndpointCache;
    private final DHTPushEndpointFinder pushEndpointFinder;

    @Inject
    public PushEndpointManager(PushEndpointCache pushEndpointCache, DHTPushEndpointFinder pushEndpointFinder) {
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFinder = pushEndpointFinder;
    }
    
    public boolean findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        CachedPushEndpoint cachedPushEndpoint = pushEndpointCache.getCached(guid);
        if (cachedPushEndpoint != null) {
         // TODO create a PushEndpoint here
            listener.handleResult(null);
            listener.handleSearchDone(true);
            return true; 
        } else {
            // TODO check for timing requirements, 
            return pushEndpointFinder.findPushEndpoint(guid, listener);
        }
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        BlockingSearchListener<PushEndpoint> listener = new BlockingSearchListener<PushEndpoint>();
        if (findPushEndpoint(guid, listener)) {
            return listener.getResult();
        } else {
            return null;
        }
    }
        
}
