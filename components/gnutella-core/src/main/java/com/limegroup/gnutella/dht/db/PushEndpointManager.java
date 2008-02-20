package com.limegroup.gnutella.dht.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

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
    private ConcurrentMap<GUID, AtomicLong> lastSearchTimeByGUID = new ConcurrentHashMap<GUID, AtomicLong>(); 
    
    public static final long TIME_BETWEEN_SEARCHES = 5L * 60L * 1000L;
    
    private long timeBetweenSearches = TIME_BETWEEN_SEARCHES;
    
    @Inject
    public PushEndpointManager(PushEndpointCache pushEndpointCache, DHTPushEndpointFinder pushEndpointFinder) {
        this.pushEndpointCache = pushEndpointCache;
        this.pushEndpointFinder = pushEndpointFinder;
    }
    
    /**
     * Mainly for testing but could be exposed if necessary. 
     */
    void setTimeBetweenSearches(long timeBetweenSearches) {
        this.timeBetweenSearches = timeBetweenSearches;
    }
    
    long getTimeBetweenSearches() {
        return timeBetweenSearches;
    }
    
    public void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener) {
        listener = SearchListenerAdapter.nonNullListener(listener);
        PushEndpoint cachedPushEndpoint = pushEndpointCache.getPushEndpoint(guid);
        if (cachedPushEndpoint != null) {
            listener.handleResult(cachedPushEndpoint);
            listener.handleSearchDone(true);
        } else {
            long currentTime = System.currentTimeMillis();
            AtomicLong lastSearchTime = lastSearchTimeByGUID.putIfAbsent(guid, new AtomicLong(currentTime));
            // no old value, just start a search, knowing we are set as last search time
            if (lastSearchTime == null) {
                startSearch(guid, listener);
            } else {
                long lastSearch = lastSearchTime.longValue();
                // check if we can start a new search
                if (currentTime - lastSearch > timeBetweenSearches) {
                    // only start a search if we set the current time
                    if (lastSearchTime.compareAndSet(lastSearch, currentTime)) {
                        startSearch(guid, listener);
                    }
                }
            }
        }
    }
    
    private void startSearch(GUID guid, final SearchListener<PushEndpoint> listener) {
        pushEndpointFinder.findPushEndpoint(guid, new SearchListener<PushEndpoint>() {
                public void handleResult(PushEndpoint result) {
                    result.updateProxies(true);
                    listener.handleResult(result);
                }
                public void handleSearchDone(boolean success) {
                    listener.handleSearchDone(success);
                }
        });
    }

    public PushEndpoint getPushEndpoint(GUID guid) {
        BlockingSearchListener<PushEndpoint> listener = new BlockingSearchListener<PushEndpoint>();
        findPushEndpoint(guid, listener);
        return listener.getResult();
    }
        
}
