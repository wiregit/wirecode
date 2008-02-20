package com.limegroup.gnutella.dht.db;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;

/**
 * Transparently interfaces with {@link PushEndpointService} 
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
    private ConcurrentMap<GUID, List<SearchListener<PushEndpoint>>> listenersByGuid = new ConcurrentHashMap<GUID, List<SearchListener<PushEndpoint>>>();
    
    @Inject
    public PushEndpointManager(PushEndpointCache pushEndpointCache, DHTPushEndpointFinder pushEndpointFinder) {
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFinder = pushEndpointFinder;
    }
    
    public void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        PushEndpoint cachedPushEndpoint = pushEndpointCache.getCached(guid);
        if (cachedPushEndpoint != null) {
            // TODO create a PushEndpoint here
            listener.handleResult(cachedPushEndpoint);
            listener.handleSearchDone(true);
        } else {
            // TODO check for timing requirements,
            final List<SearchListener<PushEndpoint>> newList =
                new CopyOnWriteArrayList<SearchListener<PushEndpoint>>();
            List<SearchListener<PushEndpoint>> oldList = listenersByGuid.putIfAbsent(guid, newList);
            if (oldList != null) {
                oldList.add(listener);
            } else {
                newList.add(listener);
            }
            pushEndpointFinder.findPushEndpoint(guid, new SearchListener<PushEndpoint>() {

                public void handleResult(PushEndpoint result) {
                    // TODO notify cache
                    
                    for (SearchListener<PushEndpoint> listener : newList) {
                        listener.handleResult(result);
                    }
                }

                public void handleSearchDone(boolean success) {
                    for (SearchListener<PushEndpoint> listener : newList) {
                        listener.handleSearchDone(success);
                    }
                }
                
            });
        }
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        BlockingSearchListener<PushEndpoint> listener = new BlockingSearchListener<PushEndpoint>();
        findPushEndpoint(guid, listener);
        return listener.getResult();
    }
        
}
