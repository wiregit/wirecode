package org.limewire.core.api.search;

import java.util.Collection;
import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.friend.api.Friend;

/**
 * Defines the API for a grouped search result.  The grouped result may be
 * supported by multiple sources.
 */
public interface GroupedSearchResult {

    /**
     * Adds the specified listener to the list that handles change events.
     */
    void addResultListener(GroupedSearchResultListener listener);
    
    /**
     * Adds the specified listener to the list that handles change events.
     */
    void removeResultListener(GroupedSearchResultListener listener);
    
    /**
     * Returns an indicator that determines if the item is from an anonymous
     * source.
     */
    boolean isAnonymous();
    
    /**
     * Returns a list of core SearchResult values associated with this visual 
     * result.
     */
    List<SearchResult> getCoreSearchResults();
    
    /**
     * Returns a Collection of friends that are sources for the item.
     */
    Collection<Friend> getFriends();
    
    /**
     * Returns the relevance value of the search result.  
     */
    float getRelevance();
    
    /**
     * Returns a Collection of sources that support the search result.  Each
     * source is represented by a RemoteHost object. 
     */
    Collection<RemoteHost> getSources();
    
    /**
     * Returns a unique identifier for this file.
     */
    URN getUrn();
}
