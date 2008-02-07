package com.limegroup.gnutella;

import com.limegroup.gnutella.search.SearchResultStats;

/**
 * 
 * @author cjones
 *
 */
public class QueryControllerImpl implements QueryController {
    
    /**
     * Holds the location/count information of the query results.
     */
    private SearchResultStats _stats;
    
    /**
     * Uses the provided SearchResultStats to access the location
     * count information for a query. See SearchResultHandler.GuidCount.
     * 
     * @param gc
     */
    public QueryControllerImpl (SearchResultStats stats) {
        _stats = stats;
    }

    /**
     * Returns a handler which can be used to easily access the
     * location count information for the given URN.
     * 
     */
    public QueryResultHandler getResultHandler (final URN urn) {
        final SearchResultStats stats = _stats;
        
        return new QueryResultHandler () {
            public int getPercentAvailable() {
                return stats.getPercentAvailable(urn);
            }
            public int getNumberOfLocations() {
                return stats.getNumberOfLocations(urn);
            }
        };
    }
    
}
